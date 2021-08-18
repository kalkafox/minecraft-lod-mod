package com.seibel.lod;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.objects.*;

import java.awt.*;

public class Main {
    public static void main(String[] args){
        try {
            System.out.println(Math.floorMod(-11, 4));
            LevelPos pos = new LevelPos((byte) 0, -1000, -1000);
            LodDimension lodDim = new LodDimension(null,null,10);
            lodDim.move(new RegionPos(0,0));
            /*
            for (int g = 0; g <= 9; g++) {
                System.out.print("test ");
                System.out.println(lodDim.getData(pos.convert((byte) g)));
                System.out.println(lodDim.hasThisPositionBeenGenerated(pos.convert((byte) g)));
                System.out.println(lodDim.doesDataExist(pos.convert((byte) g)));
                System.out.println(lodDim.getGenerationMode(pos.convert((byte) g)));
            }
            */
            lodDim.addData(pos, new LodDataPoint((short) 50, (short) 10, new Color(1, 1, 1)), DistanceGenerationMode.FEATURES, true, true);
             /*
            for (int g = 0; g <= 9; g++) {
                System.out.print("test ");
                System.out.println(lodDim.getData(pos.convert((byte) g)));
                System.out.println(lodDim.hasThisPositionBeenGenerated(pos.convert((byte) g)));
                System.out.println(lodDim.doesDataExist(pos.convert((byte) g)));
                System.out.println(lodDim.getGenerationMode(pos.convert((byte) g)));
            }

              */
            lodDim.addData(pos, new LodDataPoint((short) 50, (short) 10, new Color(1, 1, 1)), DistanceGenerationMode.SERVER, true, true);
            /*
            for (int g = 0; g <= 9; g++) {
                System.out.print("test ");
                System.out.println(lodDim.getData(pos.convert((byte) g)));
                System.out.println(lodDim.hasThisPositionBeenGenerated(pos.convert((byte) g)));
                System.out.println(lodDim.doesDataExist(pos.convert((byte) g)));
                System.out.println(lodDim.getGenerationMode(pos.convert((byte) g)));
            }

             */
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
