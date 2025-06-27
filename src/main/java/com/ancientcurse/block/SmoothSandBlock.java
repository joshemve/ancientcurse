package com.ancientcurse.block;

import net.minecraft.block.SandBlock;

/**
 * Custom sand block with consistent texture orientation.
 * The texture itself should be designed to tile seamlessly in all directions.
 */
public class SmoothSandBlock extends SandBlock {
    
    public SmoothSandBlock(int color, Settings settings) {
        super(color, settings);
    }
    
    // The block inherits all behavior from SandBlock
    // Texture consistency should be handled by the texture file itself
    // by making it seamlessly tileable
}