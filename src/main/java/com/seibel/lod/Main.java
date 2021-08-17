package com.seibel.lod;

import com.seibel.lod.objects.LevelPos;
import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.objects.RegionPos;

import java.awt.*;

public class Main {
    public static void main(String[] args){
        System.out.println(Math.floorMod(-11,4));
        LodRegion lodRegion = new LodRegion((byte) 0,new RegionPos(-1,-1));
        lodRegion.setData(new LevelPos((byte) 2,-1,-1), new LodDataPoint((short) 50,(short) 10, new Color(1,1,1)), (byte) 2,true);
        lodRegion.setData(new LevelPos((byte) 2,-1,-2), new LodDataPoint((short) 50,(short) 10, new Color(1,1,1)), (byte) 2,true);
        lodRegion.setData(new LevelPos((byte) 2,-2,-1), new LodDataPoint((short) 50,(short) 10, new Color(1,1,1)), (byte) 2,true);
        lodRegion.setData(new LevelPos((byte) 2,-2,-2), new LodDataPoint((short) 50,(short) 10, new Color(1,1,1)), (byte) 2,true);
        try {
            System.out.print("test ");
            System.out.println(lodRegion.getData(new LevelPos((byte) 3,-1,-1)));
        }catch (Exception e){
            e.printStackTrace();
        }
        return;
    }
}
