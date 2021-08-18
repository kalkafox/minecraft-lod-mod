package com.seibel.lod;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.objects.*;

import java.awt.*;

public class Main {
    public static void main(String[] args){
        try {
            System.out.println(Math.floorMod(-11, 4));
            LevelPos pos = new LevelPos((byte) 1, -1000, -1000);
            System.out.println(pos.convert((byte) 1));
            /*
            LodDimension lodDim = new LodDimension(null,null,10);
            lodDim.move(new RegionPos(0,0));
            lodDim.addData(pos, new LodDataPoint((short) 50, (short) 10, new Color(1, 1, 1)), DistanceGenerationMode.FEATURES, true, true);
            lodDim.addData(pos, new LodDataPoint((short) 50, (short) 10, new Color(1, 1, 1)), DistanceGenerationMode.SERVER, true, true);
             */
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
