package com.ancientcurse.block;

import com.ancientcurse.AncientCurse; // Replace with your actual mod main class import
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SarcophagusBlock extends HorizontalFacingBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BooleanProperty.of("open");

    // Define shapes for closed and open states
    private static final VoxelShape SHAPE_CLOSED = VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.875, 0.9375);
    private static final VoxelShape SHAPE_OPEN = VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.5, 0.9375);

    public SarcophagusBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(OPEN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(OPEN) ? SHAPE_OPEN : SHAPE_CLOSED;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        // First toggle the sarcophagus to open state
        if (!state.get(OPEN)) {
            // Play opening sound
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN,
                    SoundCategory.BLOCKS, 1.0f, 0.6f);

            // Set to open state
            world.setBlockState(pos, state.with(OPEN, true));

            // Schedule teleportation after a delay - using vanilla task scheduling
            final BlockPos finalPos = pos.toImmutable();
            ((ServerWorld)world).getServer().execute(() -> {
                try {
                    // Wait for 1 second (20 ticks)
                    Thread.sleep(1000);

                    // Make sure player is still there and alive
                    if (player.isAlive() && player.getWorld() == world) {
                        teleportPlayerToDimension(player);

                        // Play particle effect
                        spawnTeleportParticles((ServerWorld)world, finalPos);
                    }
                } catch (InterruptedException e) {
                    // Do nothing on interruption
                }
            });

            return ActionResult.SUCCESS;
        } else {
            // If already open, close it
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_CLOSE,
                    SoundCategory.BLOCKS, 1.0f, 0.6f);
            world.setBlockState(pos, state.with(OPEN, false));
            return ActionResult.SUCCESS;
        }
    }

    private void spawnTeleportParticles(ServerWorld world, BlockPos pos) {
        // Create a swirling sand effect around the sarcophagus
        for (int i = 0; i < 50; i++) {
            double offsetX = (world.getRandom().nextDouble() - 0.5) * 2;
            double offsetY = world.getRandom().nextDouble() * 2;
            double offsetZ = (world.getRandom().nextDouble() - 0.5) * 2;

            world.spawnParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    5,  // count
                    offsetX, offsetY, offsetZ,  // offset
                    0.1  // speed
            );

            world.spawnParticles(
                    ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    10,  // count
                    offsetX, offsetY, offsetZ,  // offset
                    0.05  // speed
            );
        }
    }

    private void teleportPlayerToDimension(PlayerEntity player) {
        if (!(player.getWorld() instanceof ServerWorld)) return;

        ServerWorld currentWorld = (ServerWorld) player.getWorld();
        ServerWorld targetWorld;

        // Import your mod's dimension registry key at the top of the file
        // For example: import static com.yourname.yourmodid.YourMod.ANCIENT_EGYPT_DIMENSION;
        // And replace ANCIENT_EGYPT_DIMENSION references with your actual registry key

        if (currentWorld.getRegistryKey() == World.OVERWORLD) {
            // Teleport to Egypt dimension
            targetWorld = currentWorld.getServer().getWorld(AncientCurse.ANCIENT_EGYPT_DIMENSION); // Replace YourModMainClass with your actual class
            player.sendMessage(Text.translatable("block.yourmodid.sarcophagus.teleport_to_egypt"), true);
        } else {
            // Teleport back to overworld
            targetWorld = currentWorld.getServer().getWorld(World.OVERWORLD);
            player.sendMessage(Text.translatable("block.yourmodid.sarcophagus.teleport_to_overworld"), true);
        }

        if (targetWorld != null) {
            // Play a sound at the player's location before teleporting
            currentWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 1.0f, 0.6f);

            // Teleport the player
            player.moveToWorld(targetWorld);

            // Play another sound in the target world
            targetWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.6f);
        }
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
}