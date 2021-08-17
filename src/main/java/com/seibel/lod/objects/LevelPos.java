package com.seibel.lod.objects;

import com.seibel.lod.util.LodUtil;

public class LevelPos {
    public int posX;
    public int posZ;
    public byte detailLevel;

    public LevelPos(byte detailLevel, int posX, int posZ){
        this.posX = posX;
        this.posZ = posZ;
        this.detailLevel = detailLevel;
    }

    public void convert( byte newDetailLevel){
        posX = Math.floorDiv(posX, (int) Math.pow(2, newDetailLevel - detailLevel));
        posZ = Math.floorDiv(posZ, (int) Math.pow(2, newDetailLevel - detailLevel));
    }

    public String toString(){
        String s = (detailLevel + " " + posX + " " + posZ);
        return s;
    }
}
