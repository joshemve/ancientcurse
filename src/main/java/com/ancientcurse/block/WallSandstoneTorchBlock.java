package com.ancientcurse.block;

import net.minecraft.block.WallTorchBlock;
import net.minecraft.particle.ParticleTypes;

public class WallSandstoneTorchBlock extends WallTorchBlock {
    public WallSandstoneTorchBlock(Settings settings) {
        super(settings, ParticleTypes.FLAME);
    }
}