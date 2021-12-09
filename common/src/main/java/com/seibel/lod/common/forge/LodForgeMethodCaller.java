package com.seibel.lod.common.forge;

import com.seibel.lod.common.wrappers.minecraft.MinecraftWrapper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Random;

public interface LodForgeMethodCaller {
    List<BakedQuad> getQuads(MinecraftWrapper mc, Block block, BlockState blockState, Direction direction, Random random);
}