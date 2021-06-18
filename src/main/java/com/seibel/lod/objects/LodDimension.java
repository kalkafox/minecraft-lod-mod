package com.seibel.lod.objects;

import java.io.File;
import java.io.IOException;

import com.seibel.lod.handlers.LodDimensionFileHandler;
import com.seibel.lod.util.LodUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

/**
 * This object holds all loaded LOD regions
 * for a given dimension.
 * 
 * @author James Seibel
 * @version 03-19-2021
 */
public class LodDimension
{
	public final DimensionType dimension;
	
	private volatile int width;
	private volatile int halfWidth;
	
	public LodRegion regions[][];
	public boolean isRegionDirty[][];
	
	private int centerX;
	private int centerZ;
	
	private LodDimensionFileHandler fileHandler;
	
	
	public LodDimension(DimensionType newDimension, LodWorld lodWorld, int newMaxWidth)
	{
		dimension = newDimension;
		width = newMaxWidth;
		
		try
		{
			Minecraft mc = Minecraft.getInstance();
			
			File saveDir;
			if(mc.isIntegratedServerRunning())
			{
				// local world
				
				ServerWorld serverWorld = LodUtils.getServerWorldFromDimension(newDimension);
				// provider needs a separate variable to prevent
				// the compiler from complaining
				ServerChunkProvider provider = serverWorld.getChunkProvider();
				saveDir = new File(provider.getSavedData().folder.getCanonicalFile() + "\\lod");
			}
			else
			{
				// connected to server
				
				saveDir = new File(mc.gameDir.getCanonicalFile() + 
						"\\lod server data\\" + LodUtils.getDimensionIDFromWorld(mc.world));
			}
			
			fileHandler = new LodDimensionFileHandler(saveDir, this);
		}
		catch(IOException e)
		{
			// the file handler wasn't able to be created
			// we won't be able to read or write any files
		}
		
		
		regions = new LodRegion[width][width];
		isRegionDirty = new boolean[width][width];
		
		// populate isRegionDirty
		for(int i = 0; i < width; i++)
			for(int j = 0; j < width; j++)
				isRegionDirty[i][j] = false;
		
		centerX = 0;
		centerZ = 0;
		
		halfWidth = (int)Math.floor(width / 2);
	}
	
	
	/**
	 * Move the center of this LodDimension and move all owned
	 * regions over by the given x and z offset.
	 */
	public void move(int xOffset, int zOffset)
	{		
		// if the x or z offset is equal to or greater than
		// the total size, just delete the current data
		// and update the centerX and/or centerZ
		if (Math.abs(xOffset) >= width || Math.abs(zOffset) >= width)
		{
			for(int x = 0; x < width; x++)
			{
				for(int z = 0; z < width; z++)
				{
					regions[x][z] = null;
				}
			}
			
			// update the new center
			centerX += xOffset;
			centerZ += zOffset;
			
			return;
		}
		
		
		// X
		if(xOffset > 0)
		{
			// move everything over to the left (as the center moves to the right)
			for(int x = 0; x < width; x++)
			{
				for(int z = 0; z < width; z++)
				{
					if(x + xOffset < width)
						regions[x][z] = regions[x + xOffset][z];
					else
						regions[x][z] = null;
				}
			}
		}
		else
		{
			// move everything over to the right (as the center moves to the left)
			for(int x = width - 1; x >= 0; x--)
			{
				for(int z = 0; z < width; z++)
				{
					if(x + xOffset >= 0)
						regions[x][z] = regions[x + xOffset][z];
					else
						regions[x][z] = null;
				}
			}
		}
		
		
		
		// Z
		if(zOffset > 0)
		{
			// move everything up (as the center moves down)
			for(int x = 0; x < width; x++)
			{
				for(int z = 0; z < width; z++)
				{
					if(z + zOffset < width)
						regions[x][z] = regions[x][z + zOffset];
					else
						regions[x][z] = null;
				}
			}
		}
		else
		{
			// move everything down (as the center moves up)
			for(int x = 0; x < width; x++)
			{
				for(int z = width - 1; z >= 0; z--)
				{
					if(z + zOffset >= 0)
						regions[x][z] = regions[x][z + zOffset];
					else
						regions[x][z] = null;
				}
			}
		}
		
		
		
		// update the new center
		centerX += xOffset;
		centerZ += zOffset;
	}
	
	
	
	
	
	
	/**
	 * Gets the region at the given X and Z
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodRegion getRegion(int regionX, int regionZ)
	{
		int xIndex = (regionX - centerX) + halfWidth;
		int zIndex = (regionZ - centerZ) + halfWidth;
		
		if (!regionIsInRange(regionX, regionZ))
			// out of range
			return null;
		
		if (regions[xIndex][zIndex] == null)
		{
			regions[xIndex][zIndex] = getRegionFromFile(regionX, regionZ);
			if (regions[xIndex][zIndex] == null)
			{
				regions[xIndex][zIndex] = new LodRegion(regionX, regionZ);
			}
		}
		
		return regions[xIndex][zIndex];
	}
	
	/**
	 * Overwrite the LodRegion at the location of newRegion with newRegion.
	 * @throws ArrayIndexOutOfBoundsException if newRegion is outside what can be stored in this LodDimension.
	 */
	public void setRegion(LodRegion newRegion) throws ArrayIndexOutOfBoundsException
	{
		int xIndex = (newRegion.x - centerX) + halfWidth;
		int zIndex = (centerZ - newRegion.z) + halfWidth;
		
		if (!regionIsInRange(newRegion.x, newRegion.z))
			// out of range
			throw new ArrayIndexOutOfBoundsException();
		
		regions[xIndex][zIndex] = newRegion;
	}
	
	
	
	
	
	/**
	 * Add the given LOD to this dimension at the coordinate
	 * stored in the LOD. If an LOD already exists at the given
	 * coordinates it will be overwritten.
	 */
	public void addLod(LodChunk lod)
	{
		RegionPos pos = LodUtils.convertChunkPosToRegionPos(new ChunkPos(lod.x, lod.z));
		
		// don't continue if the region can't be saved
		if (!regionIsInRange(pos.x, pos.z))
		{
			return;
		}
			
		LodRegion region = getRegion(pos.x, pos.z);
		
		if (region == null)
		{
			// if no region exists, create it
			region = new LodRegion(pos.x, pos.z);
			setRegion(region);
		}
		
		region.addLod(lod);
		
		// don't save empty place holders to disk
		if (!lod.isPlaceholder() && fileHandler != null)
		{
			// mark the region as dirty so it will be saved to disk
			int xIndex = (pos.x - centerX) + halfWidth;
			int zIndex = (pos.z - centerZ) + halfWidth;
			isRegionDirty[xIndex][zIndex] = true;
			fileHandler.saveDirtyRegionsToFileAsync();
		}
	}
	
	/**
	 * Get the LodChunk at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or 
	 * is outside the loaded area.
	 */
	public LodChunk getLodFromCoordinates(int chunkX, int chunkZ)
	{
		RegionPos pos = LodUtils.convertChunkPosToRegionPos(new ChunkPos(chunkX, chunkZ));
		
		LodRegion region = getRegion(pos.x, pos.z);
		
		if(region == null)
			return null;
		
		return region.getLod(chunkX, chunkZ);
	}
	
	
	/**
	 * Get the region at the given X and Z coordinates from the
	 * RegionFileHandler.
	 */
	public LodRegion getRegionFromFile(int regionX, int regionZ)
	{
		if (fileHandler != null)
			return fileHandler.loadRegionFromFile(regionX, regionZ);
		else
			return null;
	}
	
	
	/**
	 * Returns whether the region at the given X and Z coordinates
	 * is within the loaded range.
	 */
	public boolean regionIsInRange(int regionX, int regionZ)
	{
		int xIndex = (regionX - centerX) + halfWidth;
		int zIndex = (regionZ - centerZ) + halfWidth;
		
		return xIndex >= 0 && xIndex < width && zIndex >= 0 && zIndex < width;
	}

	
	
	
	
	

	public int getCenterX()
	{
		return centerX;
	}
	
	public int getCenterZ()
	{
		return centerZ;
	}
	
	
	/**
	 * Returns how many non-null LodChunks
	 * are stored in this LodDimension.
	 */
	public int getNumberOfLods()
	{
		int numbLods = 0;
        for (LodRegion[] regions : regions)
        {
        	if(regions == null)
    			continue;
        	
        	for (LodRegion region : regions)
            {
        		if(region == null)
        			continue;
        			
            	for(LodChunk[] lods : region.getAllLods())
            	{
            		if(lods == null)
            			continue;
            		
            		for(LodChunk lod : lods)
                	{
                		if (lod != null)
                			numbLods++;
                	}
            	}
            }
        }
        
        return numbLods;
	}
	
	
	public int getWidth()
	{
		return width;
	}
	
	public void setRegionWidth(int newWidth)
	{
		width = newWidth;
		halfWidth = (int)Math.floor(width / 2);
		
		regions = new LodRegion[width][width];
		isRegionDirty = new boolean[width][width];
		
		// populate isRegionDirty
		for(int i = 0; i < width; i++)
			for(int j = 0; j < width; j++)
				isRegionDirty[i][j] = false;
	}
	
	
	@Override
	public String toString()
	{
		String s = "";
		
		s += "dim: " + dimension.toString() + "\t";
		s += "(" + centerX + "," + centerZ + ")";
		
		return s;
	}
}




