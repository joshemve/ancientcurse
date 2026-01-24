package com.ancientcurse.fluid;

import com.ancientcurse.block.registry.FluidBlocks;
import com.ancientcurse.ModItems;
import com.ancientcurse.registry.ModFluids;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.fluid.FlowableFluid;

public abstract class SoulLavaFluid extends FlowableFluid {
    @Override
    public Fluid getStill() {
        return ModFluids.STILL_SOUL_LAVA;
    }

    @Override
    public Fluid getFlowing() {
        return ModFluids.FLOWING_SOUL_LAVA;
    }

    @Override
    public Item getBucketItem() {
        return ModItems.SOUL_LAVA_BUCKET;
    }

    @Override
    public boolean matchesType(Fluid fluid) {
        return fluid == ModFluids.STILL_SOUL_LAVA || fluid == ModFluids.FLOWING_SOUL_LAVA;
    }

    @Override
    public java.util.Optional<net.minecraft.sound.SoundEvent> getBucketFillSound() {
        return java.util.Optional.of(net.minecraft.sound.SoundEvents.ITEM_BUCKET_FILL_LAVA);
    }

    @Override
    public net.minecraft.particle.ParticleEffect getParticle() {
        return net.minecraft.particle.ParticleTypes.DRIPPING_LAVA;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid,
            Direction direction) {
        return direction == Direction.DOWN && !fluid.isIn(net.minecraft.registry.tag.FluidTags.WATER);
    }

    @Override
    protected int getFlowSpeed(WorldView world) {
        return 2;
    }

    @Override
    protected int getLevelDecreasePerBlock(WorldView world) {
        return 2;
    }

    @Override
    public int getTickRate(WorldView world) {
        return 30;
    }

    @Override
    protected float getBlastResistance() {
        return 100.0F;
    }

    @Override
    protected boolean isInfinite(net.minecraft.world.World world) {
        return false;
    }

    @Override
    protected void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
        final net.minecraft.block.entity.BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos)
                : null;
        net.minecraft.block.Block.dropStacks(state, world, pos, blockEntity);
    }

    @Override
    protected BlockState toBlockState(FluidState state) {
        return FluidBlocks.SOUL_LAVA.getDefaultState().with(Properties.LEVEL_15, getBlockStateLevel(state));
    }

    @Override
    public boolean isStill(FluidState state) {
        return false;
    }

    @Override
    public int getLevel(FluidState state) {
        return state.get(LEVEL);
    }

    public static class Flowing extends SoulLavaFluid {
        @Override
        protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getLevel(FluidState state) {
            return state.get(LEVEL);
        }

        @Override
        public boolean isStill(FluidState state) {
            return false;
        }
    }

    public static class Still extends SoulLavaFluid {
        @Override
        public int getLevel(FluidState state) {
            return 8;
        }

        @Override
        public boolean isStill(FluidState state) {
            return true;
        }
    }
}
