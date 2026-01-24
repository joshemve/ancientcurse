package com.ancientcurse.block;

import com.ancientcurse.system.CursedEarthManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Salt Dust Block - Similar to redstone dust but white/salt colored
 * Can be placed on surfaces like redstone dust
 * When 7+ salt blocks are connected, creates a protection zone against cursed earth spread
 */
public class SaltDustBlock extends Block {

    // Minimum connected salt blocks needed to activate protection
    private static final int MIN_SALT_FOR_PROTECTION = 7;
    // Protection radius beyond the salt formation
    private static final int PROTECTION_BUFFER = 3;
    // How long protection lasts (in ticks) - refreshed when salt network changes
    private static final int PROTECTION_DURATION = 72000; // 1 hour

    // Property to track if this salt is part of an active protection network
    public static final BooleanProperty ACTIVATED = BooleanProperty.of("activated");
    public static final EnumProperty<WireConnection> WIRE_CONNECTION_NORTH = Properties.NORTH_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> WIRE_CONNECTION_EAST = Properties.EAST_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> WIRE_CONNECTION_SOUTH = Properties.SOUTH_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> WIRE_CONNECTION_WEST = Properties.WEST_WIRE_CONNECTION;
    
    // Flat shape on the ground, like redstone dust (0 to 0.25 pixels tall)
    private static final VoxelShape DOT_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 0.25, 16.0);
    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    
    static {
        // Initialize shapes for different connection combinations
        for (int i = 0; i < 16; i++) {
            SHAPES[i] = DOT_SHAPE;
        }
    }
    
    public SaltDustBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
            .with(WIRE_CONNECTION_NORTH, WireConnection.NONE)
            .with(WIRE_CONNECTION_EAST, WireConnection.NONE)
            .with(WIRE_CONNECTION_SOUTH, WireConnection.NONE)
            .with(WIRE_CONNECTION_WEST, WireConnection.NONE)
            .with(ACTIVATED, false));
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return DOT_SHAPE;
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos downPos = pos.down();
        BlockState downState = world.getBlockState(downPos);
        return downState.isSideSolidFullSquare(world, downPos, Direction.UP) || downState.isOf(Blocks.HOPPER);
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getConnectionState(ctx.getWorld(), this.getDefaultState(), ctx.getBlockPos());
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !this.canPlaceAt(state, world, pos)) {
            return Blocks.AIR.getDefaultState();
        }

        // Update connections when neighbors change
        if (direction != Direction.DOWN) {
            BlockState newState = this.getConnectionState(world, state, pos);

            // Schedule protection check on server
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.scheduleBlockTick(pos, this, 1);
            }

            return newState;
        }

        return state;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient && !oldState.isOf(this)) {
            // Check protection network when salt is placed
            world.scheduleBlockTick(pos, this, 1);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClient && !newState.isOf(this) && state.get(ACTIVATED)) {
            // Salt was removed - check if nearby salt networks need to deactivate
            checkAndUpdateNearbyNetworks((ServerWorld) world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        checkAndUpdateProtectionNetwork(world, pos);
    }

    /**
     * Calculate connection state for all 4 horizontal directions
     */
    private BlockState getConnectionState(BlockView world, BlockState state, BlockPos pos) {
        return state
            .with(WIRE_CONNECTION_NORTH, this.getConnectionType(world, pos, Direction.NORTH))
            .with(WIRE_CONNECTION_SOUTH, this.getConnectionType(world, pos, Direction.SOUTH))
            .with(WIRE_CONNECTION_EAST, this.getConnectionType(world, pos, Direction.EAST))
            .with(WIRE_CONNECTION_WEST, this.getConnectionType(world, pos, Direction.WEST));
    }

    /**
     * Determine connection type in a specific direction
     */
    private WireConnection getConnectionType(BlockView world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.offset(direction);
        BlockState neighborState = world.getBlockState(neighborPos);

        // Connect to other salt dust blocks on the same level
        if (neighborState.isOf(this)) {
            return WireConnection.SIDE;
        }

        // Check if salt dust is on top of the block next to this one (going up)
        BlockState upState = world.getBlockState(neighborPos.up());
        if (upState.isOf(this)) {
            return WireConnection.UP;
        }

        // Check if salt dust is below the neighbor position (going down)
        BlockState downState = world.getBlockState(neighborPos.down());
        if (downState.isOf(this) && !world.getBlockState(pos.up()).isOf(this)) {
            return WireConnection.SIDE;
        }

        return WireConnection.NONE;
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WIRE_CONNECTION_NORTH, WIRE_CONNECTION_EAST, WIRE_CONNECTION_SOUTH, WIRE_CONNECTION_WEST, ACTIVATED);
    }

    /**
     * Checks the connected salt network and updates protection status
     */
    private void checkAndUpdateProtectionNetwork(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.isOf(this)) return;

        // Find all connected salt blocks
        Set<BlockPos> connectedSalt = findConnectedSalt(world, pos);
        int saltCount = connectedSalt.size();

        boolean shouldBeActivated = saltCount >= MIN_SALT_FOR_PROTECTION;
        boolean currentlyActivated = state.get(ACTIVATED);

        if (shouldBeActivated && !currentlyActivated) {
            // Activate the network!
            activateNetwork(world, connectedSalt);
        } else if (!shouldBeActivated && currentlyActivated) {
            // Deactivate the network
            deactivateNetwork(world, connectedSalt);
        }
    }

    /**
     * Finds all salt blocks connected to the given position using flood-fill
     * Only checks horizontal connections (no vertical jumping)
     */
    private Set<BlockPos> findConnectedSalt(World world, BlockPos startPos) {
        Set<BlockPos> connected = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        toCheck.add(startPos);

        // Limit search to prevent server lag on massive formations
        int maxSearch = 500;

        while (!toCheck.isEmpty() && connected.size() < maxSearch) {
            BlockPos current = toCheck.poll();
            if (connected.contains(current)) continue;

            BlockState state = world.getBlockState(current);
            if (!state.isOf(this)) continue;

            connected.add(current);

            // Check all 4 horizontal directions only (same level)
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos neighbor = current.offset(dir);

                // Only add if not already checked
                if (!connected.contains(neighbor)) {
                    toCheck.add(neighbor);
                }
            }
        }

        return connected;
    }

    /**
     * Activates a salt protection network
     */
    private void activateNetwork(ServerWorld world, Set<BlockPos> saltPositions) {
        // Calculate bounds of the salt formation
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : saltPositions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        // Calculate center of the formation
        BlockPos center = new BlockPos(
            (minX + maxX) / 2,
            (minY + maxY) / 2,
            (minZ + maxZ) / 2
        );

        // Calculate radius to cover entire formation + buffer
        int radiusX = (maxX - minX) / 2 + PROTECTION_BUFFER;
        int radiusZ = (maxZ - minZ) / 2 + PROTECTION_BUFFER;
        int radius = Math.max(radiusX, radiusZ);

        // Update all salt blocks to activated state
        for (BlockPos pos : saltPositions) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(this) && !state.get(ACTIVATED)) {
                world.setBlockState(pos, state.with(ACTIVATED, true), Block.NOTIFY_ALL);
            }
        }

        // Register protection with CursedEarthManager
        CursedEarthManager.getInstance().createSaltCircle(world, center, radius, PROTECTION_DURATION);

        // Spawn activation particles and play sound
        spawnActivationEffects(world, saltPositions, center);

        // Log activation
        com.ancientcurse.AncientCurse.LOGGER.info("Salt protection network activated at {} with {} blocks, radius {}",
            center, saltPositions.size(), radius);
    }

    /**
     * Deactivates a salt protection network
     */
    private void deactivateNetwork(ServerWorld world, Set<BlockPos> saltPositions) {
        for (BlockPos pos : saltPositions) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(this) && state.get(ACTIVATED)) {
                world.setBlockState(pos, state.with(ACTIVATED, false), Block.NOTIFY_ALL);
            }
        }
    }

    /**
     * When salt is removed, check if nearby networks need to be recalculated
     */
    private void checkAndUpdateNearbyNetworks(ServerWorld world, BlockPos removedPos) {
        // Check adjacent positions for salt that might need network recalculation
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos neighbor = removedPos.offset(dir);
            BlockState neighborState = world.getBlockState(neighbor);
            if (neighborState.isOf(this)) {
                // Schedule a check for this neighbor's network
                world.scheduleBlockTick(neighbor, this, 1);
            }

            // Also check up/down neighbors
            BlockPos upNeighbor = neighbor.up();
            if (world.getBlockState(upNeighbor).isOf(this)) {
                world.scheduleBlockTick(upNeighbor, this, 1);
            }
            BlockPos downNeighbor = neighbor.down();
            if (world.getBlockState(downNeighbor).isOf(this)) {
                world.scheduleBlockTick(downNeighbor, this, 1);
            }
        }
    }

    /**
     * Spawns magical activation particles and plays sound
     */
    private void spawnActivationEffects(ServerWorld world, Set<BlockPos> saltPositions, BlockPos center) {
        // Play activation sound
        world.playSound(null, center, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.5f);
        world.playSound(null, center, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 1.2f);

        // Spawn particles along all salt blocks
        for (BlockPos pos : saltPositions) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.1;
            double z = pos.getZ() + 0.5;

            // Rising white particles
            for (int i = 0; i < 3; i++) {
                world.spawnParticles(
                    ParticleTypes.END_ROD,
                    x + (world.random.nextDouble() - 0.5) * 0.5,
                    y + world.random.nextDouble() * 0.5,
                    z + (world.random.nextDouble() - 0.5) * 0.5,
                    1, 0, 0.05, 0, 0.02
                );
            }

            // Enchantment glyphs
            world.spawnParticles(
                ParticleTypes.ENCHANT,
                x, y + 0.5, z,
                5, 0.3, 0.2, 0.3, 0.1
            );
        }

        // Extra dramatic effect at center
        world.spawnParticles(
            ParticleTypes.FLASH,
            center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
            1, 0, 0, 0, 0
        );

        world.spawnParticles(
            ParticleTypes.SOUL_FIRE_FLAME,
            center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
            20, 1.0, 0.5, 1.0, 0.02
        );
    }
} 