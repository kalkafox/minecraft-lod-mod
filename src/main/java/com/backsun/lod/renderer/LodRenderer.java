package com.backsun.lod.renderer;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import com.backsun.lod.builders.BuildBufferThread;
import com.backsun.lod.handlers.ReflectionHandler;
import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.objects.NearFarBuffer;
import com.backsun.lod.util.LodConfig;
import com.backsun.lod.util.enums.ColorDirection;
import com.backsun.lod.util.enums.FogDistance;
import com.backsun.lod.util.enums.FogQuality;
import com.backsun.lod.util.enums.LodLocation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;

/**
 * @author James Seibel
 * @version 2-22-2021
 */
public class LodRenderer
{
	/** If true the LODs colors will be replaced with
	 * a checkerboard, this can be used for debugging. */
	public boolean debugging = false;
	
	private Minecraft mc;
	private float farPlaneDistance;
	// make sure this is an even number, or else it won't align with the chunk grid
	/** this is the total width of the LODs (I.E the diameter, not the radius) */
	private static final int LOD_CHUNK_DISTANCE_RADIUS = 6;
	
	private Tessellator tessellator;
	private BufferBuilder bufferBuilder;
	
	private ReflectionHandler reflectionHandler;
	
	public LodDimension lodDimension = null;
	
	
	
	private int maxNumbThreads = Runtime.getRuntime().availableProcessors();
	/** How many threads should be used for building the render buffer. */
	private int numbBufferThreads = maxNumbThreads;
	private ArrayList<BuildBufferThread> bufferThreads = new ArrayList<BuildBufferThread>();
	private volatile BufferBuilder[] drawableNearBuffers = null;
	private volatile BufferBuilder[] drawableFarBuffers = null;
	
	private volatile BufferBuilder[] buildableNearBuffers = null;
	private volatile BufferBuilder[] buildableFarBuffers = null;
	
	private ExecutorService bufferThreadPool = Executors.newFixedThreadPool(maxNumbThreads);
	private ExecutorService genThread = Executors.newSingleThreadExecutor();
	
	/** This is used to determine if the LODs should be regenerated */
	private int previousChunkRenderDistance = 0;
	/** This is used to determine if the LODs should be regenerated */
	private int prevChunkX = 0;
	/** This is used to determine if the LODs should be regenerated */
	private int prevChunkZ = 0;
	/** This is used to determine if the LODs should be regenerated */
	private FogDistance prevFogDistance = FogDistance.NEAR_AND_FAR;
	
	/** if this is true the LODs should be regenerated */
	private boolean regen = false;
	
	private volatile boolean regenerating = false;
	private volatile boolean switchBuffers = false;
	
	
	
	
	
	
	public LodRenderer()
	{
		mc = Minecraft.getMinecraft();
		
		// for some reason "Tessellator.getInstance()" won't work here, we have to create a new one
		tessellator = new Tessellator(2097152);
		bufferBuilder = tessellator.getBuffer();
		
		reflectionHandler = new ReflectionHandler();
	}
	
	
	
	public void drawLODs(LodDimension newDimension, float partialTicks)
	{
		if (reflectionHandler.fovMethod == null)
		{
			// don't continue if we can't get the
			// user's FOV
			return;
		}
		
		if (reflectionHandler.fovMethod == null)
		{
			// we aren't able to get the user's
			// FOV, don't render anything
			return;
		}
		
		if (lodDimension == null && newDimension == null)
		{
			// if there aren't any loaded LodChunks
			// don't try drawing anything
			return;
		}
		
		
		
		// should LODs be regenerated?
		if ((int)Minecraft.getMinecraft().player.posX / LodChunk.WIDTH != prevChunkX ||
			(int)Minecraft.getMinecraft().player.posZ / LodChunk.WIDTH != prevChunkZ ||
			previousChunkRenderDistance != mc.gameSettings.renderDistanceChunks ||
			prevFogDistance != LodConfig.fogDistance ||
			lodDimension != newDimension)
		{
			regen = true;
			
			prevChunkX = (int)Minecraft.getMinecraft().player.posX / LodChunk.WIDTH;
			prevChunkZ = (int)Minecraft.getMinecraft().player.posZ / LodChunk.WIDTH;
			prevFogDistance = LodConfig.fogDistance;
		}
		else
		{
			// nope, the player hasn't moved, the
			// render distance hasn't changed, and
			// the dimension is the same
			regen = false;
		}
		
		lodDimension = newDimension;
		
		
		
		
		
		
		// used for debugging and viewing how long different processes take
		mc.mcProfiler.endSection();
		mc.mcProfiler.startSection("LOD");
		mc.mcProfiler.startSection("LOD setup");
		if (LodConfig.drawCheckerBoard)
		{
			if (debugging != LodConfig.drawCheckerBoard)
				regen = true;
			debugging = true;
		}
		else
		{
			if (debugging != LodConfig.drawCheckerBoard)
				regen = true;
			debugging = false;
		}
		
		
		
		// get the camera location
		Entity player = mc.player;
		double cameraX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		double cameraY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		double cameraZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

		

		
		// determine how far the game's render distance is currently set
		int renderDistWidth = mc.gameSettings.renderDistanceChunks;
		farPlaneDistance = renderDistWidth * LodChunk.WIDTH;
		
		// set how big the LODs will be and how far they will go
		int totalLength = (int) farPlaneDistance * LOD_CHUNK_DISTANCE_RADIUS * 2;
		int numbChunksWide = (totalLength / LodChunk.WIDTH);
		
		
		
		
		
		
		//=================//
		// create the LODs //
		//=================//
		
		// only regenerate LODs if:
		// 1. we want to regenerate LODs
		// 2. we aren't already regenerating LODs
		// 3. we aren't waiting for the build and draw buffers to swap
		if (regen && !regenerating && !switchBuffers)
		{
			mc.mcProfiler.endStartSection("LOD generation");
			regenerating = true;
			
			
			if (numbBufferThreads != bufferThreads.size())
				setupBufferThreads();
			
			if (drawableNearBuffers == null || drawableFarBuffers == null || 
				previousChunkRenderDistance != mc.gameSettings.renderDistanceChunks)
				setupBuffers(numbChunksWide);
			
			
			genThread.execute(createLodBufferGenerationThread(cameraX, cameraZ, numbChunksWide));
		}
		
		// replace the buffers used to draw and build,
		// this is done to keep everything thread safe
		if (switchBuffers)
		{
			swapBuffers();
			switchBuffers = false;
		}
		
		
		
		
		
		//===========================//
		// GL settings for rendering //
		//===========================//
		
		// set the required open GL settings
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glLineWidth(2.0f);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		
		GlStateManager.translate(-cameraX, -cameraY, -cameraZ);
		
		setProjectionMatrix(partialTicks);
		setupLighting(partialTicks);
		
		
		
		
		
		
		//===========//
		// rendering //
		//===========//
		
		switch(LodConfig.fogDistance)
		{
		case NEAR_AND_FAR:
			mc.mcProfiler.endStartSection("LOD draw setup");
			setupFog(FogDistance.NEAR, reflectionHandler.getFogQuality());
			sendLodsToGpuAndDraw(drawableNearBuffers);
			
			mc.mcProfiler.endStartSection("LOD draw setup");
			setupFog(FogDistance.FAR, reflectionHandler.getFogQuality());
			sendLodsToGpuAndDraw(drawableFarBuffers);
			break;
		case NEAR:
			mc.mcProfiler.endStartSection("LOD draw setup");
			setupFog(FogDistance.NEAR, reflectionHandler.getFogQuality());
			sendLodsToGpuAndDraw(drawableNearBuffers);
			break;
		case FAR:
			mc.mcProfiler.endStartSection("LOD draw setup");
			setupFog(FogDistance.FAR, reflectionHandler.getFogQuality());
			sendLodsToGpuAndDraw(drawableFarBuffers);
			break;
		}
		
		
		
		
		
		//=========//
		// cleanup //
		//=========//
		
		mc.mcProfiler.endStartSection("LOD cleanup");
		
		// this must be done otherwise other parts of the screen may be drawn with a fog effect
		// IE the GUI
		GlStateManager.disableFog();
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHT2);
		GL11.glDisable(GL11.GL_COLOR_MATERIAL);
		
		// change the perspective matrix back to prevent incompatibilities
		// with other mods that may render during forgeRenderLast
		Project.gluPerspective(reflectionHandler.getFov(mc, partialTicks, true), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, this.farPlaneDistance * MathHelper.SQRT_2);
		
		// this can't be called until after the buffers are built
		// because otherwise the buffers may be set to the wrong size
		previousChunkRenderDistance = mc.gameSettings.renderDistanceChunks;
		
		
		// end of profiler tracking
		mc.mcProfiler.endSection();
	}
	
	
	
	
	
	/**
	 * draw an array of cubes (or squares) with the given colors.
	 * @param lods bounding boxes to draw
	 * @param colors color of each box to draw
	 */
	private void generateLodBuffers(AxisAlignedBB[][] lods, Color[][] colors, FogDistance fogDistance)
	{
		List<Future<NearFarBuffer>> bufferFutures = new ArrayList<>();
		
		for(int i = 0; i < numbBufferThreads; i++)
		{
			bufferThreads.get(i).setNewData(buildableNearBuffers[i], buildableFarBuffers[i], fogDistance, lods, colors, i, numbBufferThreads);
		}
		
		try
		{
			bufferFutures = bufferThreadPool.invokeAll(bufferThreads);
		}
		catch (InterruptedException e)
		{
			// this should never happen, but just in case
			e.printStackTrace();
		}
		
		for(int i = 0; i < numbBufferThreads; i++)
		{
			try
			{
				buildableNearBuffers[i] = bufferFutures.get(i).get().nearBuffer;
				buildableFarBuffers[i] = bufferFutures.get(i).get().farBuffer;
			}
			catch(CancellationException | ExecutionException| InterruptedException e)
			{
				// this should never happen, but just in case
				e.printStackTrace();
			}
		}
		
	}
	
	private void sendLodsToGpuAndDraw(BufferBuilder[] buffers)
	{
		for(int i = 0; i < numbBufferThreads; i++)
		{
			int pos = bufferBuilder.getByteBuffer().position();
			buffers[i].getByteBuffer().position(pos);
			
			bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			bufferBuilder.getByteBuffer().clear();
			bufferBuilder.putBulkData(buffers[i].getByteBuffer());
			
			mc.mcProfiler.endStartSection("LOD draw");
			tessellator.draw();
			mc.mcProfiler.endStartSection("LOD draw setup");
			
			bufferBuilder.getByteBuffer().clear(); // this is required otherwise nothing is drawn
		}
	}
	
	
	
	
	
	
	
	//=================//
	// Setup Functions //
	//=================//
	
	private void setupFog(FogDistance fogDistance, FogQuality fogQuality)
	{
		if(fogQuality == FogQuality.OFF)
		{
			GlStateManager.disableFog();
			return;
		}
		
		if(fogDistance == FogDistance.NEAR_AND_FAR)
		{
			throw new IllegalArgumentException("setupFog doesn't accept the NEAR_AND_FAR fog distance.");
		}

		// the multipliers are percentages
		// of the regular view distance.
		
		if(fogDistance == FogDistance.NEAR)
		{
			// the reason that I wrote fogEnd then fogStart backwards
			// is because we are using fog backwards to how
			// it is normally used, with it hiding near objects
			// instead of far objects.
			
			if (fogQuality == FogQuality.FANCY)
			{
				GlStateManager.setFogEnd(farPlaneDistance * 0.3f * LOD_CHUNK_DISTANCE_RADIUS);
				GlStateManager.setFogStart(farPlaneDistance * 0.35f * LOD_CHUNK_DISTANCE_RADIUS);
			}
			else if(fogQuality == FogQuality.FAST)
			{
				// for the far fog of the normal chunks
				// to start right where the LODs' end use:
				// end = 0.8f, start = 1.5f
				
				GlStateManager.setFogEnd(farPlaneDistance * 1.5f);
				GlStateManager.setFogStart(farPlaneDistance * 2.0f);
			}
		}
		else if(fogDistance == FogDistance.FAR)
		{
			if (fogQuality == FogQuality.FANCY)
			{
				GlStateManager.setFogStart(farPlaneDistance * 0.78f * LOD_CHUNK_DISTANCE_RADIUS);
				GlStateManager.setFogEnd(farPlaneDistance * 1.0f * LOD_CHUNK_DISTANCE_RADIUS);
			}
			else if(fogQuality == FogQuality.FAST)
			{
				GlStateManager.setFogStart(farPlaneDistance * 0.5f * LOD_CHUNK_DISTANCE_RADIUS);
				GlStateManager.setFogEnd(farPlaneDistance * 0.75f * LOD_CHUNK_DISTANCE_RADIUS);
			}
		}
		
		GlStateManager.setFogDensity(0.1f);
		GlStateManager.enableFog();
	}
	

	/**
	 * create a new projection matrix and send it over to the GPU
	 * @param partialTicks how many ticks into the frame we are
	 * @return true if the matrix was successfully created and sent to the GPU, false otherwise
	 */
	private void setProjectionMatrix(float partialTicks)
	{
		// create a new view frustum so that the squares can be drawn outside the normal view distance
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.loadIdentity();	
		
		// only continue if we can get the FOV
		if (reflectionHandler.fovMethod != null)
		{
			Project.gluPerspective(reflectionHandler.getFov(mc, partialTicks, true), (float) mc.displayWidth / (float) mc.displayHeight, 0.5F, farPlaneDistance * 12);
		}
		
		// we weren't able to set up the projection matrix
		return;
	}
	
	
	/**
	 * setup the lighting to be used for the LODs
	 */
	private void setupLighting(float partialTicks)
	{
		GL11.glEnable(GL11.GL_COLOR_MATERIAL); // set the color to be used as the material (this allows lighting to be enabled)
		
		// this isn't perfect right now, but it looks pretty good at 50% brightness
		float sunBrightness = mc.world.getSunBrightness(partialTicks) * mc.world.provider.getSunBrightnessFactor(partialTicks);
		float skyHasLight = mc.world.provider.hasSkyLight()? 1.0f : 0.15f;
		float gammaMultiplyer = (mc.gameSettings.gammaSetting * 0.5f + 0.5f);
		float lightStrength = sunBrightness * skyHasLight * gammaMultiplyer;
		float lightAmbient[] = {lightStrength, lightStrength, lightStrength, 1.0f};
        	
		ByteBuffer temp = ByteBuffer.allocateDirect(16);
		temp.order(ByteOrder.nativeOrder());
		GL11.glLight(GL11.GL_LIGHT2, GL11.GL_AMBIENT, (FloatBuffer) temp.asFloatBuffer().put(lightAmbient).flip());
		GL11.glEnable(GL11.GL_LIGHT2); // Enable the above lighting
		
		GlStateManager.enableLighting();
	}
	
	
	private void setupBufferThreads()
	{
		bufferThreads.clear();
		for(int i = 0; i < numbBufferThreads; i++)
			bufferThreads.add(new BuildBufferThread());
	}
	
	/**
	 * 
	 */
	private void setupBuffers(int numbChunksWide)
	{
		drawableNearBuffers = new BufferBuilder[numbBufferThreads];
		drawableFarBuffers = new BufferBuilder[numbBufferThreads];
		
		buildableNearBuffers = new BufferBuilder[numbBufferThreads];
		buildableFarBuffers = new BufferBuilder[numbBufferThreads];
		
		
		// calculate the max amount of storage needed (in bytes)
		// by any singular buffer
		// NOTE: most buffers won't use the full amount, but this should prevent
		//		them from needing to allocate more memory (which is a slow progress)
		int bufferMaxCapacity = (numbChunksWide * numbChunksWide * (6 * 4 * ((3 * 4) + (4 * 4)))) / numbBufferThreads;
		
		for(int i = 0; i < numbBufferThreads; i++)
		{
			drawableNearBuffers[i] = new BufferBuilder(bufferMaxCapacity);
			drawableFarBuffers[i] = new BufferBuilder(bufferMaxCapacity);
			
			buildableNearBuffers[i] = new BufferBuilder(bufferMaxCapacity);
			buildableFarBuffers[i] = new BufferBuilder(bufferMaxCapacity);
		}
	}
	
	
	
	
	
	
	
	//======================//
	// Other Misc Functions // 
	//======================//
	
	
	/**
	 * Returns -1 if there are no valid points
	 */
	private int getHighestPointInLod(short[] heightPoints)
	{
		if (heightPoints[LodLocation.NE.value] != -1)
			return heightPoints[LodLocation.NE.value];
		if (heightPoints[LodLocation.NW.value] != -1)
			return heightPoints[LodLocation.NW.value];
		if (heightPoints[LodLocation.SE.value] != -1)
			return heightPoints[LodLocation.NE.value];
		return heightPoints[LodLocation.NE.value];
	}
	
	
	/**
	 * Create a thread to asynchronously generate LOD buffers
	 * centered around the given camera X and Z.
	 * <br>
	 * This thread will write to the drawableNearBuffers and drawableFarBuffers.
	 * <br>
	 * After the buildable buffers have been generated they must be
	 * swapped with the drawable buffers to be drawn.
	 */
	private Thread createLodBufferGenerationThread(double cameraX, double cameraZ,
			int numbChunksWide)
	{
		// this is where we store the points for each LOD object
		AxisAlignedBB lodArray[][] = new AxisAlignedBB[numbChunksWide][numbChunksWide];
		// this is where we store the color for each LOD object
		Color colorArray[][] = new Color[numbChunksWide][numbChunksWide];
		
		int alpha = 255; // 0 - 255
		Color red = new Color(255, 0, 0, alpha);
		Color black = new Color(0, 0, 0, alpha);
		Color white = new Color(255, 255, 255, alpha);
		@SuppressWarnings("unused")
		Color invisible = new Color(0,0,0,0);
		@SuppressWarnings("unused")
		Color error = new Color(255, 0, 225, alpha); // bright pink
		
		// this seemingly useless math is required,
		// just using (int) camera doesn't work
		int playerXChunkOffset = ((int) cameraX / LodChunk.WIDTH) * LodChunk.WIDTH;
		int playerZChunkOffset = ((int) cameraZ / LodChunk.WIDTH) * LodChunk.WIDTH;
		// this is where we will start drawing squares
		// (exactly half the total width)
		int startX = (-LodChunk.WIDTH * (numbChunksWide / 2)) + playerXChunkOffset;
		int startZ = (-LodChunk.WIDTH * (numbChunksWide / 2)) + playerZChunkOffset;
		
		
		Thread t = new Thread(()->
		{
			// x axis
			for (int i = 0; i < numbChunksWide; i++)
			{
				// z axis
				for (int j = 0; j < numbChunksWide; j++)
				{
					// skip the middle
					// (As the player moves some chunks will overlap or be missing,
					// this is just how chunk loading/unloading works. This can hopefully
					// be hidden with careful use of fog)
					int middle = (numbChunksWide / 2);
					if (RenderUtil.isCoordinateInLoadedArea(i, j, middle))
					{
						continue;
					}
					
					
					// set where this square will be drawn in the world
					double xOffset = (LodChunk.WIDTH * i) + // offset by the number of LOD blocks
									startX; // offset so the center LOD block is centered underneath the player
					double yOffset = 0;
					double zOffset = (LodChunk.WIDTH * j) + startZ;
					
					int chunkX = i + (startX / LodChunk.WIDTH);
					int chunkZ = j + (startZ / LodChunk.WIDTH);
					
					LodChunk lod = lodDimension.getLodFromCoordinates(chunkX, chunkZ);
					if (lod == null)
					{
						// note: for some reason if any color or lod objects are set here
						// it causes the game to use 100% gpu; 
						// undefined in the debug menu
						// and drop to ~6 fps.
						colorArray[i][j] = null;
						lodArray[i][j] = null;
						
						continue;
					}
					
					Color c = new Color(
							(lod.colors[ColorDirection.TOP.value].getRed()),
							(lod.colors[ColorDirection.TOP.value].getGreen()),
							(lod.colors[ColorDirection.TOP.value].getBlue()),
							lod.colors[ColorDirection.TOP.value].getAlpha());
					
					
					
					if (!debugging)
					{
						// add the color to the array
						colorArray[i][j] = c;
					}
					else
					{
						// if debugging draw the squares as a black and white checker board
						if ((chunkX + chunkZ) % 2 == 0)
							c = white;
						else
							c = black;
						// draw the first square as red
						if (i == 0 && j == 0)
							c = red;
						
						colorArray[i][j] = c;
					}
					
					
					// add the new box to the array
					int topPoint = getHighestPointInLod(lod.top);
					int bottomPoint = getHighestPointInLod(lod.bottom);
					
					// don't draw an LOD if it is empty
					if (topPoint == -1 && bottomPoint == -1)
						continue;
					
					lodArray[i][j] = new AxisAlignedBB(0, bottomPoint, 0, LodChunk.WIDTH, topPoint, LodChunk.WIDTH).offset(xOffset, yOffset, zOffset);
				}
			}
			
			generateLodBuffers(lodArray, colorArray, LodConfig.fogDistance);
			
			regenerating = false;
			switchBuffers = true;
		});
		return t;
	}
	
	/**
	 * Swap buildable and drawable buffers.
	 */
	private void swapBuffers()
	{
		for(int i = 0; i < buildableNearBuffers.length; i++)
		{				
			try
			{
				BufferBuilder tmp = buildableNearBuffers[i];
				buildableNearBuffers[i] = drawableNearBuffers[i];
				drawableNearBuffers[i] = tmp;
				
				tmp = buildableFarBuffers[i];
				buildableFarBuffers[i] = drawableFarBuffers[i];
				drawableFarBuffers[i] = tmp;
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
	
}