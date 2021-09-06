package com.seibel.lod.objects;

import com.seibel.lod.util.LodUtil;

import java.util.Arrays;
import java.util.logging.Level;

public class PosToRenderContainer
{
	private byte minDetail;
	private int regionPosX;
	private int regionPosZ;
	private int numberOfPosToRender;
	private int[][] posToRender;
	/*TODO this population matrix could be converted to boolean to improve memory use*/
	private byte[][] population;

	public PosToRenderContainer(byte minDetail)
	{
		this.numberOfPosToRender = 0;
	}
	public PosToRenderContainer(byte minDetail, int regionPosX, int regionPosZ)
	{
		this.minDetail = minDetail;
		this.numberOfPosToRender = 0;
		this.regionPosX = regionPosX;
		this.regionPosZ = regionPosZ;
		posToRender = new int[1][3];
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - minDetail);
		population = new byte[size][size];
	}

	public void addPosToRender(int[] levelPos)
	{
		addPosToRender(LevelPosUtil.getDetailLevel(levelPos), LevelPosUtil.getPosX(levelPos), LevelPosUtil.getPosZ(levelPos));
	}

	public void addPosToRender(byte detailLevel, int posX, int posZ)
	{
		if(numberOfPosToRender >= posToRender.length)
			posToRender = Arrays.copyOf(posToRender, posToRender.length*2);
		posToRender[numberOfPosToRender][0] = detailLevel;
		posToRender[numberOfPosToRender][1] = posX;
		posToRender[numberOfPosToRender][2] = posZ;
		numberOfPosToRender++;
		population[LevelPosUtil.getRegionModule((byte) 0, LevelPosUtil.convert(detailLevel,posX,(byte) 0))]
				[LevelPosUtil.getRegionModule((byte) 0, LevelPosUtil.convert(detailLevel,posZ,(byte) 0))] = (byte) (detailLevel + 1);
	}

	public boolean contains(int[] levelPos){
		return contains(
				LevelPosUtil.getDetailLevel(levelPos),
				LevelPosUtil.getPosX(levelPos),
				LevelPosUtil.getPosZ(levelPos));
	}

	public boolean contains(byte detailLevel, int posX, int posZ)
	{
		if(LevelPosUtil.getRegion(detailLevel, posX) == regionPosX && LevelPosUtil.getRegion(detailLevel, posZ) == regionPosZ)
		{
			return (population[LevelPosUtil.getRegionModule((byte) 0, LevelPosUtil.convert(detailLevel,posX,(byte) 0))]
					        [LevelPosUtil.getRegionModule((byte) 0, LevelPosUtil.convert(detailLevel,posZ,(byte) 0))] == (detailLevel + 1));
		}else
		{
			return false;
		}
	}

	public int getNumberOfPos()
	{
		return numberOfPosToRender;
	}

	public int[] getNthPos(int n)
	{
		return posToRender[n];
	}

	public String toString()
	{

		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < numberOfPosToRender; i++)
		{
			builder.append(posToRender[i][0]);
			builder.append(" ");
			builder.append(posToRender[i][1]);
			builder.append(" ");
			builder.append(posToRender[i][2]);
			builder.append('\n');
		}
		builder.append('\n');
		return builder.toString();
	}
}
