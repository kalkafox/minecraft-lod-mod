package com.seibel.lod.builders;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.objects.LevelPosUtil;
import net.minecraft.util.math.ChunkPos;

/**
 * @author Leonardo Amato
 * @version 22-08-2021
 */
public class GenerationRequest
{
	public final int[] levelPos;
	public final DistanceGenerationMode generationMode;

	public GenerationRequest(int[] levelPos, DistanceGenerationMode generationMode)
	{
		this.levelPos = levelPos;
		this.generationMode = generationMode;
	}

	public ChunkPos getChunkPos()
	{
		return new ChunkPos(LevelPosUtil.getChunkPosX(levelPos),LevelPosUtil.getChunkPosZ(levelPos));
	}
}