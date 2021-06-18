package com.seibel.lod.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.util.LodConfig;

/**
 * This object handles creating LodRegions
 * from files and saving LodRegion objects
 * to file.
 * 
 * @author James Seibel
 * @version 6-13-2021
 */
public class LodDimensionFileHandler
{
	/** This is what separates each piece of data */
	public static final char DATA_DELIMITER = ',';
	
	
	private LodDimension loadedDimension = null;
	public long regionLastWriteTime[][];
	
	private File dimensionDataSaveFolder;
	
	/** lod */
	private final String FILE_NAME_PREFIX = "lod";
	/** .txt */
	private final String FILE_EXTENSION = ".txt";
	
	/** This is the file version currently accepted by this
	 * file handler, older versions (smaller numbers) will be deleted and overwritten,
	 * newer versions (larger numbers) will be ignored and won't be read. */
	public static final int LOD_SAVE_FILE_VERSION = 2;
	
	/** This is the string written before the file version */
	private static final String LOD_FILE_VERSION_PREFIX = "lod_save_file_version";
	
	/** Allow saving asynchronously, but never try to save multiple regions
	 * at a time */
	private ExecutorService fileWritingThreadPool = Executors.newSingleThreadExecutor();
	
	
	public LodDimensionFileHandler(File newSaveFolder, LodDimension newLoadedDimension)
	{
		if (newSaveFolder == null)
			throw new IllegalArgumentException("LodDimensionFileHandler requires a valid File location to read and write to.");
		
		dimensionDataSaveFolder = newSaveFolder;
		
		loadedDimension = newLoadedDimension;
		// these two variable are used in sync with the LodDimension
		regionLastWriteTime = new long[loadedDimension.getWidth()][loadedDimension.getWidth()];
		for(int i = 0; i < loadedDimension.getWidth(); i++)
			for(int j = 0; j < loadedDimension.getWidth(); j++)
				regionLastWriteTime[i][j] = -1;
	}
	
	
	
	
	
	//================//
	// read from file //
	//================//
	
	
	
	/**
	 * Return the LodRegion at the given coordinates.
	 * (null if the file doesn't exist)
	 */
	public LodRegion loadRegionFromFile(int regionX, int regionZ)
	{
		
		String fileName = getFileNameAndPathForRegion(regionX, regionZ, LodConfig.CLIENT.lodDetail.get());
		
		File f = new File(fileName);
		
		if (!f.exists())
		{
			// there wasn't a file, don't
			// return anything
			return null;
		}
		
		LodRegion region = new LodRegion(regionX, regionZ);
		
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(f));
			String s = br.readLine();
			int fileVersion = -1;
						
			if(s != null && !s.isEmpty())
			{
				// try to get the file version
				try
				{
					fileVersion = Integer.parseInt(s.substring(s.indexOf(' ')).trim());
				}
				catch(NumberFormatException | StringIndexOutOfBoundsException e)
				{
					// this file doesn't have a version
					// keep the version as -1
					fileVersion = -1;
				}
				
				// check if this file can be read by this file handler
				if(fileVersion < LOD_SAVE_FILE_VERSION)
				{
					// the file we are reading is an older version,
					// close the reader and delete the file.
					br.close();
					f.delete();
					ClientProxy.LOGGER.info("Outdated LOD region file for region: (" + regionX + "," + regionZ + ") version: " + fileVersion +
							", version requested: " + LOD_SAVE_FILE_VERSION +
							" File was been deleted.");
					
					return null;
				}
				else if(fileVersion > LOD_SAVE_FILE_VERSION)
				{
					// the file we are reading is a newer version,
					// close the reader and ignore the file, we don't
					// want to accidently delete anything the user may want.
					br.close();
					ClientProxy.LOGGER.info("Newer LOD region file for region: (" + regionX + "," + regionZ + ") version: " + fileVersion + 
							", version requested: " + LOD_SAVE_FILE_VERSION +
							" this region will not be written to in order to protect the newer file.");
					
					return null;
				}
			}
			else
			{
				// there is no data in this file
				br.close();
				return null;
			}
			
			
			// this file is a readable version, begin reading the file
			s = br.readLine();
			
			while(s != null && !s.isEmpty())
			{
				try
				{
					// convert each line into an LOD object and add it to the region
					LodChunk lod = new LodChunk(s, LodConfig.CLIENT.lodDetail.get());
					
					region.addLod(lod);
				}
				catch(IllegalArgumentException e)
				{
					// we were unable to create this chunk
					// for whatever reason.
					// skip to the next chunk
					ClientProxy.LOGGER.warn(e.getMessage());
				}
				
				s = br.readLine();
			}
			
			br.close();
		}
		catch (IOException e)
		{
			// the buffered reader encountered a 
			// problem reading the file
			return null;
		}
		
		return region;
	}
	
	
	
	
	
	
	
	
	//==============//
	// Save to File //
	//==============//
	
	/**
	 * Save all dirty regions in this LodDimension to file.
	 */
	public void saveDirtyRegionsToFileAsync()
	{
		fileWritingThreadPool.execute(saveDirtyRegionsThread);
	}
	
	private Thread saveDirtyRegionsThread = new Thread(() -> 
	{
		for(int i = 0; i < loadedDimension.getWidth(); i++)
		{
			for(int j = 0; j < loadedDimension.getWidth(); j++)
			{
				if(loadedDimension.isRegionDirty[i][j] && loadedDimension.regions[i][j] != null)
				{
					saveRegionToDisk(loadedDimension.regions[i][j]);
					loadedDimension.isRegionDirty[i][j] = false;
				}
			}
		}
	});
 	
	/**
	 * Save a specific region to disk.<br>
	 * Note: <br>
	 * 1. If a file already exists for a newer version
	 * the file won't be written.<br>
	 * 2. This will save to the LodDimension that this
	 * handler is associated with.
	 */
	private void saveRegionToDisk(LodRegion region)
	{
		// convert chunk coordinates to region
		// coordinates
		int x = region.x;
		int z = region.z;
		
		File f = new File(getFileNameAndPathForRegion(x, z, LodConfig.CLIENT.lodDetail.get()));
		
		try
		{
			// make sure the file and folder exists
			if (!f.exists())
			{
				// the file doesn't exist,
				// create it and the folder if need be
				if(!f.getParentFile().exists())
					f.getParentFile().mkdirs();
				f.createNewFile();
			}
			else
			{
				// the file exists, make sure it
				// is the correct version.
				// (to make sure we don't overwrite a newer
				// version file if it exists)
				
				BufferedReader br = new BufferedReader(new FileReader(f));
				String s = br.readLine();
				int fileVersion = LOD_SAVE_FILE_VERSION;
							
				if(s != null && !s.isEmpty())
				{
					// try to get the file version
					try
					{
						fileVersion = Integer.parseInt(s.substring(s.indexOf(' ')).trim());
					}
					catch(NumberFormatException | StringIndexOutOfBoundsException e)
					{
						// this file doesn't have a correctly formated version
						// just overwrite the file
					}
				}
				br.close();
				
				// check if this file can be written to by the file handler
				if(fileVersion <= LOD_SAVE_FILE_VERSION)
				{
					// we are good to continue and overwrite the old file
				}
				else //if(fileVersion > LOD_SAVE_FILE_VERSION)
				{
					// the file we are reading is a newer version,
					// don't write anything, we don't want to accidently 
					// delete anything the user may want.
					return;
				}
			}
			
			FileWriter fw = new FileWriter(f);
			
			// add the version of this file
			fw.write(LOD_FILE_VERSION_PREFIX + " " + LOD_SAVE_FILE_VERSION + "\n");
			
			// add each LodChunk to the file
			for(LodChunk[] chunkArray : region.getAllLods())
				for(LodChunk chunk : chunkArray)
					if(chunk != null && !chunk.isPlaceholder())
						fw.write(chunk.toData() + "\n");
			
			fw.close();
		}
		catch(Exception e)
		{
			ClientProxy.LOGGER.error("LOD file write error: " + e.getMessage());
		}
	}
	
	
 	
	
	
	
	
	//================//
	// helper methods //
	//================//
	
	
	/**
	 * Return the name of the file that should contain the 
	 * region at the given x and z. <br>
	 * Returns null if this object isn't ready to read and write. <br><br>
	 * 
	 * example: "lod.FULL.0.0.txt"
	 */
	private String getFileNameAndPathForRegion(int regionX, int regionZ, LodDetail detail)
	{
		try
		{
			// saveFolder is something like
			// ".\Super Flat\DIM-1\data"
			// or
			// ".\Super Flat\data"
			return dimensionDataSaveFolder.getCanonicalPath() + "\\" + 
					FILE_NAME_PREFIX + "." + detail.toString() + "." + regionX + "." + regionZ + FILE_EXTENSION;
		}
		catch(IOException e)
		{
			return null;
		}
	}
	
}