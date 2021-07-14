package com.seibel.lod.builders.lodNodeTemplates;

import com.seibel.lod.objects.LodQuadTreeNode;
import com.seibel.lod.objects.LodQuadTreeDimension;
import net.minecraft.client.renderer.BufferBuilder;

/**
 * TODO DynamicLodTemplate
 * Chunks smoothly transition between
 * each other, unless a neighboring chunk
 * is at a significantly different height.
 * 
 * @author James Seibel
 * @version 06-16-2021
 */
public class DynamicLodNodeTemplate extends AbstractLodNodeTemplate
{
	@Override
	public void addLodToBuffer(BufferBuilder buffer,
							   LodQuadTreeDimension lodDim, LodQuadTreeNode lod,
							   double xOffset, double yOffset, double zOffset,
							   boolean debugging)
	{
		System.err.println("DynamicLodTemplate not implemented!");
	}

	@Override
	public int getBufferMemoryForSingleLod() {
		// TODO Auto-generated method stub
		return 0;
	}
}