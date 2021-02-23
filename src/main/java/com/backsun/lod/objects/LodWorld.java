package com.backsun.lod.objects;

import java.util.Hashtable;
import java.util.Map;

/**
 * This stores all LODs for a given world.
 * 
 * @author James Seibel
 * @version 02-22-2021
 */
public class LodWorld
{
	public String worldName;
	
	/**
	 * Key = Dimension id (as an int)
	 */
	private Map<Integer, LodDimension> lodDimensions;
	
	
	public LodWorld(String newWorldName)
	{
		worldName = newWorldName;
		lodDimensions = new Hashtable<Integer, LodDimension>();
	}
	
	
	
	public void addLodDimension(LodDimension newStorage)
	{
		lodDimensions.put(newStorage.dimension.getId(), newStorage);
	}
	
	public LodDimension getLodDimension(int dimensionId)
	{
		return lodDimensions.get(dimensionId);
	}
	
	/**
	 * Resizes the max width in regions that each LodDimension
	 * should use. 
	 */
	public void resizeDimensionRegionWidth(int newWidth)
	{
		for(Integer key : lodDimensions.keySet())
			lodDimensions.get(key).setRegionWidth(newWidth);
	}
	
	
	
	@Override
	public String toString()
	{
		String s = "";
		
		s += worldName + "\t - dimensions: ";
		for(Integer key : lodDimensions.keySet())
			s += lodDimensions.get(key).dimension.getName() + ", ";
		
		return s;
	}
}
