package com.seibel.lod.objects;

import com.seibel.lod.util.LodUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.awt.*;
import java.io.Serializable;

/**
 * STANDARD TO FOLLOW
 * every coordinate called posX or posZ is a relative coordinate and not and absolute coordinate
 * if an array contain coordinate the order is the following
 * 0 for x, 1 for z in 2D
 * 0 for x, 1 for y, 2 for z in 3D
 */

public class LodRegion implements Serializable {
    //x coord,
    private byte minLevelOfDetail;
    private static final byte POSSIBLE_LOD = 10;
    private int numberOfPoints;

    //For each of the following field the first slot is for the level of detail
    //Important: byte have a [-128, 127] range. When converting from or to int a 128 should be added or removed
    //If there is a bug with color then it's probably caused by this.
    //in the future other fields like transparency and light level could be added
    private byte[][][][] colors;

    private short[][][] height;

    private short[][][] depth;

    //a new node will have 0 as generationType
    //a node with 1 is node
    private byte[][][] generationType;

    private boolean[][][] nodeExistence;

    public final int regionPosX;
    public final int regionPosZ;

    public LodRegion(byte minimumLevelOfDetail, RegionPos regionPos) {
        this.regionPosX = regionPos.x;
        this.regionPosZ = regionPos.z;

        //Array of matrices of arrays
        colors = new byte[POSSIBLE_LOD][][][];

        //Arrays of matrices
        height = new short[POSSIBLE_LOD][][];
        depth = new short[POSSIBLE_LOD][][];
        generationType = new byte[POSSIBLE_LOD][][];
        nodeExistence = new boolean[POSSIBLE_LOD][][];


        //Initialize all the different matrices
        for (byte lod = minimumLevelOfDetail; lod <= LodUtil.REGION_DETAIL_LEVEL; lod++) {
            int size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - lod);
            colors[lod] = new byte[size][size][3];
            height[lod] = new short[size][size];
            depth[lod] = new short[size][size];
            generationType[lod] = new byte[size][size];
            nodeExistence[lod] = new boolean[size][size];

        }
    }

    /**
     * This method can be used to insert data into the LodRegion
     *
     * @param levelPos
     * @param dataPoint
     * @param generationType
     * @param update
     * @return
     */
    public boolean setData(LevelPos levelPos, LodDataPoint dataPoint, byte generationType, boolean update) {
        levelPos.regionModule();
        return setData(levelPos.detailLevel, levelPos.posX, levelPos.posZ, (byte) (dataPoint.color.getRed() - 128), (byte) (dataPoint.color.getGreen() - 128), (byte) (dataPoint.color.getBlue() - 128), dataPoint.height, dataPoint.depth, generationType, update);
    }

    /**
     * This method can be used to insert data into the LodRegion
     *
     * @param lod
     * @param posX
     * @param posZ
     * @param red
     * @param green
     * @param blue
     * @param height
     * @param depth
     * @param generationType
     * @param update
     * @return
     */
    private boolean setData(byte lod, int posX, int posZ, byte red, byte green, byte blue, short height, short depth, byte generationType, boolean update) {
        if ((this.generationType[lod][posX][posZ] == 0) || (generationType < this.generationType[lod][posX][posZ])) {

            //update the number of node present
            //if (this.generationType[lod][posX][posZ] == 0) numberOfPoints++;

            //add the node data
            this.colors[lod][posX][posZ][0] = red;
            this.colors[lod][posX][posZ][1] = green;
            this.colors[lod][posX][posZ][2] = blue;
            this.height[lod][posX][posZ] = height;
            this.depth[lod][posX][posZ] = depth;
            this.generationType[lod][posX][posZ] = generationType;
            this.nodeExistence[lod][posX][posZ] = true;

            //update could be stopped and a single big update could be done at the endnew
            LevelPos levelPos = new LevelPos(lod, posX, posZ);
            if (update) {
                for (byte tempLod = (byte) (lod + 1); tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod++) {
                    levelPos.convert(tempLod);
                    update(levelPos);
                }
            }
            //System.out.print("created ");
            //System.out.println(new LevelPos(lod,posX,posZ));
            return true; //added
        } else {
            //System.out.print("not created ");
            //System.out.println(new LevelPos(lod,posX,posZ));
            return false; //not added
        }
    }

    public LodDataPoint getData(ChunkPos chunkPos) {
        return getData(new LevelPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.x, chunkPos.z));
    }

    /**
     * This method will return the data in the position relative to the level of detail
     *
     * @param lod
     * @return the data at the relative pos and level
     */
    public LodDataPoint getData(byte lod, BlockPos blockPos) {
        int posX = Math.floorMod(blockPos.getX(), (int) Math.pow(2, lod));
        int posZ = Math.floorMod(blockPos.getZ(), (int) Math.pow(2, lod));
        return getData(new LevelPos(lod, posX, posZ));
    }

    /**
     * This method will return the data in the position relative to the level of detail
     *
     * @param levelPos
     * @return the data at the relative pos and level
     */
    public LodDataPoint getData(LevelPos levelPos) {
        levelPos.regionModule();
        return new LodDataPoint(
                height[levelPos.detailLevel][levelPos.posX][levelPos.posZ],
                depth[levelPos.detailLevel][levelPos.posX][levelPos.posZ],
                new Color(colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][0] + 128,
                        colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][1] + 128,
                        colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][2] + 128
                )
        );
    }

    /*
        private void updateArea(byte lod, int posX, int posZ){
        }
    */
    private void update(LevelPos levelPos) {

        levelPos.regionModule();
        boolean[][] children = getChildren(levelPos);
        int numberOfChildren = 0;

        /**TODO add the ability to change how the heigth and depth are determinated (for example min or max)**/
        byte minGenerationType = 10;
        int tempRed = 0;
        int tempGreen = 0;
        int tempBlue = 0;
        int tempHeight = 0;
        int tempDepth = 0;
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                if (children[x][z]) {
                    numberOfChildren++;
                    int newPosX = 2 * levelPos.posX + x;
                    int newPosZ = 2 * levelPos.posZ + z;
                    byte newLod = (byte) (levelPos.detailLevel - 1);

                    tempRed += colors[newLod][newPosX][newPosZ][0];
                    tempGreen += colors[newLod][newPosX][newPosZ][1];
                    tempBlue += colors[newLod][newPosX][newPosZ][2];
                    tempHeight += height[newLod][newPosX][newPosZ];
                    tempDepth += depth[newLod][newPosX][newPosZ];
                    minGenerationType = (byte) Math.min(minGenerationType, generationType[newLod][newPosX][newPosZ]);
                }
            }
        }

        if (numberOfChildren > 0) {
            colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][0] = (byte) (tempRed / numberOfChildren);
            colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][1] = (byte) (tempGreen / numberOfChildren);
            colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][2] = (byte) (tempBlue / numberOfChildren);
            height[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = (short) (tempHeight / numberOfChildren);
            depth[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = (short) (tempDepth / numberOfChildren);
            generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = minGenerationType;
            nodeExistence[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = true;
        }
    }

    private boolean[][] getChildren(LevelPos levelPos) {
        levelPos.regionModule();
        boolean[][] children = new boolean[2][2];
        int numberOfChild = 0;
        if (minLevelOfDetail == levelPos.detailLevel) {
            return children;
        }
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                children[x][z] = (nodeExistence[levelPos.detailLevel - 1][2 * levelPos.posX + x][2 * levelPos.posZ + z]);
            }
        }
        return children;
    }

    public boolean doesNodeExist(ChunkPos chunkPos) {
        return doesNodeExist(new LevelPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.x, chunkPos.z));
    }

    public boolean doesNodeExist(LevelPos levelPos) {
        levelPos.regionModule();
        return nodeExistence[levelPos.detailLevel][levelPos.posX][levelPos.posZ];
    }

    /**
     * This will be used to save a level
     *
     * @param lod
     * @return
     */
    public LevelContainer getLevel(byte lod) {
        return new LevelContainer(colors[lod], height[lod], depth[lod], generationType[lod]);
    }

    public void addLevel(byte lod, LevelContainer levelContainer) {
        if (lod < minLevelOfDetail - 1) {
            throw new IllegalArgumentException("addLevel requires a level that is at least the minimum level of the region -1 ");
        }
        if (lod == minLevelOfDetail - 1) minLevelOfDetail = lod;
        colors[lod] = levelContainer.colors;
        height[lod] = levelContainer.height;
        depth[lod] = levelContainer.depth;
        generationType[lod] = levelContainer.generationType;

    }

    public void removeDetailLevel(byte lod) {
        for (byte tempLod = 0; tempLod <= lod; tempLod++) {
            colors[tempLod] = new byte[0][0][0];
            height[tempLod] = new short[0][0];
            depth[tempLod] = new short[0][0];
            generationType[tempLod] = new byte[0][0];
        }
    }
}
