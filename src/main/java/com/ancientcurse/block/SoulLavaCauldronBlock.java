package com.ancientcurse.block;

import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;

/**
 * A cauldron filled with Soul Lava.
 * Emits light and damages entities that step inside.
 */
public class SoulLavaCauldronBlock extends AbstractCauldronBlock {
    public SoulLavaCauldronBlock(Settings settings, Map<net.minecraft.item.Item, CauldronBehavior> behaviorMap) {
        super(settings, behaviorMap);
    }

    @Override
    public boolean isFull(BlockState state) {
        return true;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (this.isEntityTouchingFluid(state, pos, entity)) {
            entity.setOnFireFor(15);
            if (entity.damage(world.getDamageSources().lava(), 4.0F)) {
                entity.playSound(net.minecraft.sound.SoundEvents.BLOCK_LAVA_EXTINGUISH, 0.4F,
                        2.0F + world.random.nextFloat() * 0.4F);
            }
        }
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return 3;
    }
}
