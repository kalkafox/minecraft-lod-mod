package com.backsun.lod.builders;
import java.awt.Color;
import java.util.concurrent.Callable;

import org.lwjgl.opengl.GL11;

import com.backsun.lod.objects.NearFarBuffer;
import com.backsun.lod.renderer.RenderUtil;
import com.backsun.lod.util.enums.FogDistance;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * 
 * 
 * @author James Seibel
 * @version 02-21-2021
 */
public class BuildBufferThread implements Callable<NearFarBuffer>
{
	public BufferBuilder nearBuffer;
	public BufferBuilder farBuffer;
	public FogDistance distanceMode;
	public AxisAlignedBB[][] lods;
	public Color[][] colors;
	
	private int startLodIndex = 0;
	private int endLodIndex = -1;
	
	
	
	public BuildBufferThread()
	{
		
	}
	
	public BuildBufferThread(BufferBuilder newNearBufferBuilder, BufferBuilder newFarBufferBuilder, AxisAlignedBB[][] newLods, Color[][] newColors, FogDistance newDistanceMode, int threadNumber, int totalThreads)
	{
		setNewData(newNearBufferBuilder, newFarBufferBuilder, distanceMode, newLods, newColors, threadNumber, totalThreads);
	}
	
	public void setNewData(BufferBuilder newNearBufferBuilder, BufferBuilder newFarBufferBuilder, FogDistance newDistanceMode, AxisAlignedBB[][] newLods, Color[][] newColors, int threadNumber, int totalThreads)
	{
		nearBuffer = newNearBufferBuilder;
		farBuffer = newFarBufferBuilder;
		distanceMode = newDistanceMode;
		lods = newLods;
		colors = newColors;
		
		int numbChunksWide = lods.length;
		int rowsToRender = numbChunksWide / totalThreads;
		startLodIndex = threadNumber * rowsToRender;
		endLodIndex = (threadNumber + 1) * rowsToRender;
	}
	
	@Override
	public NearFarBuffer call()
	{
		nearBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		farBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		
		int numbChunksWide = lods.length;
		
		BufferBuilder currentBuffer;
		AxisAlignedBB bb;
		int red;
		int green;
		int blue;
		int alpha;
		
		if (distanceMode == FogDistance.NEAR)
		{
			currentBuffer = nearBuffer;
		}
		else // if (distanceMode == FogDistance.FAR)
		{
			currentBuffer = farBuffer;
		}
		
		
		// x axis
		for (int i = startLodIndex; i < endLodIndex; i++)
		{
			// z axis
			for (int j = 0; j < numbChunksWide; j++)
			{
				if (lods[i][j] == null || colors[i][j] == null)
					continue;
				
				bb = lods[i][j];
				
				// get the color of this LOD object
				red = colors[i][j].getRed();
				green = colors[i][j].getGreen();
				blue = colors[i][j].getBlue();
				alpha = colors[i][j].getAlpha();
				
				// choose which buffer to add these LODs too
				if (distanceMode == FogDistance.NEAR_AND_FAR)
				{
					if (RenderUtil.isCoordinateInNearFogArea(i, j, numbChunksWide / 2))
						currentBuffer = nearBuffer;
					else
						currentBuffer = farBuffer;
				}
				
				
				if (bb.minY != bb.maxY)
				{
					// top (facing up)
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
					// bottom (facing down)
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);

					// south (facing -Z) 
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					// north (facing +Z)
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);

					// west (facing -X)
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
					// east (facing +X)
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
				}
				else
				{
					// render this LOD as one block thick
					
					// top (facing up)
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					// bottom (facing down)
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);

					// south (facing -Z) 
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					// north (facing +Z)
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);

					// west (facing -X)
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					// east (facing +X)
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
				}
				
			} // z axis
		} // x axis
		
		nearBuffer.finishDrawing();
		farBuffer.finishDrawing();
		
		return new NearFarBuffer(nearBuffer, farBuffer);
	}
	
	private void addPosAndColor(BufferBuilder buffer, double x, double y, double z, int red, int green, int blue, int alpha)
	{
		buffer.pos(x, y, z).color(red, green, blue, alpha).endVertex();
	}

	
}