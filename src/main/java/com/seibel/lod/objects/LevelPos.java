package com.seibel.lod.objects;

import com.seibel.lod.util.LodUtil;

public class LevelPos implements Cloneable{
    public byte detailLevel;
    public int posX;
    public int posZ;

    public LevelPos(byte detailLevel, int posX, int posZ){
        this.posX = posX;
        this.posZ = posZ;
        this.detailLevel = detailLevel;
    }

    public void convert( byte newDetailLevel){
        posX = Math.floorDiv(posX, (int) Math.pow(2, newDetailLevel - detailLevel));
        posZ = Math.floorDiv(posZ, (int) Math.pow(2, newDetailLevel - detailLevel));
        detailLevel = newDetailLevel;
    }

    public RegionPos getRegionPos(){
        return new RegionPos(
                Math.floorDiv(posX, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel)),
                Math.floorDiv(posZ, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel)));
    }

    public void regionModule(){
        posX = Math.floorMod(posX, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel));
        posZ = Math.floorMod(posZ, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel));
    }

    public LevelPos clone(){
        return new LevelPos(detailLevel,posX,posZ);
    }

    public String toString(){
        String s = (detailLevel + " " + posX + " " + posZ);
        return s;
    }
}
