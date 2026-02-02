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
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;

/**
 * Salt Dust Block - Similar to redstone dust but white/salt colored
 * Can be placed on surfaces like redstone dust
 * When 7+ salt blocks are connected, creates a protection zone against cursed
 * earth spread
 */
public class SaltDustBlock extends Block {

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
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {
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
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos,
            net.minecraft.util.math.random.Random random) {
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
        builder.add(WIRE_CONNECTION_NORTH, WIRE_CONNECTION_EAST, WIRE_CONNECTION_SOUTH, WIRE_CONNECTION_WEST,
                ACTIVATED);
    }

    /**
     * Checks the connected salt network and updates protection status
     */
    private void checkAndUpdateProtectionNetwork(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.isOf(this))
            return;

        // Find all connected salt blocks
        Set<BlockPos> connectedSalt = findConnectedSalt(world, pos);

        // Only trigger if a complete loop is formed
        boolean shouldBeActivated = isLoopFormed(world, connectedSalt);

        // Check if ANY block in the network is already activated (not just the triggering block)
        boolean networkAlreadyActivated = isNetworkActivated(world, connectedSalt);

        if (shouldBeActivated && !networkAlreadyActivated) {
            // Activate the network with effects (first time activation)
            activateNetwork(world, connectedSalt);
        } else if (shouldBeActivated && networkAlreadyActivated) {
            // Network already active - just silently update any new blocks without effects
            silentlyActivateNewBlocks(world, connectedSalt);
        } else if (!shouldBeActivated && networkAlreadyActivated) {
            // Deactivate the network (loop broken)
            deactivateNetwork(world, connectedSalt);
        }
    }

    /**
     * Checks if any block in the network is already activated
     */
    private boolean isNetworkActivated(World world, Set<BlockPos> network) {
        for (BlockPos pos : network) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(this) && state.get(ACTIVATED)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Silently activates new blocks added to an already-active network (no effects)
     */
    private void silentlyActivateNewBlocks(ServerWorld world, Set<BlockPos> saltPositions) {
        for (BlockPos pos : saltPositions) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(this) && !state.get(ACTIVATED)) {
                world.setBlockState(pos, state.with(ACTIVATED, true), Block.NOTIFY_ALL);
            }
        }
    }

    /**
     * Finds all salt blocks connected to the given position using flood-fill
     * Includes vertical connections supported by the block's connection logic
     */
    private Set<BlockPos> findConnectedSalt(World world, BlockPos startPos) {
        Set<BlockPos> connected = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        toCheck.add(startPos);

        // Limit search to prevent server lag on massive formations
        int maxSearch = 500;

        while (!toCheck.isEmpty() && connected.size() < maxSearch) {
            BlockPos current = toCheck.poll();
            if (connected.contains(current))
                continue;

            BlockState state = world.getBlockState(current);
            if (!state.isOf(this))
                continue;

            connected.add(current);

            // Use the shared neighbor logic for consistency
            for (BlockPos neighbor : getConnectedNeighbors(world, current)) {
                if (!connected.contains(neighbor)) {
                    toCheck.add(neighbor);
                }
            }
        }

        return connected;
    }

    /**
     * Gets all valid connected neighbors for a specific salt block
     * Based on the visual connection logic in getConnectionType
     */
    private List<BlockPos> getConnectedNeighbors(World world, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // Connect to other salt dust blocks on the same level
            if (neighborState.isOf(this)) {
                neighbors.add(neighborPos);
                continue;
            }

            // Check if salt dust is on top of the block next to this one (going up)
            if (world.getBlockState(neighborPos.up()).isOf(this)) {
                neighbors.add(neighborPos.up());
                continue;
            }

            // Check if salt dust is below the neighbor position (going down)
            if (world.getBlockState(neighborPos.down()).isOf(this) && !world.getBlockState(pos.up()).isOf(this)) {
                neighbors.add(neighborPos.down());
            }
        }
        return neighbors;
    }

    /**
     * Checks if the connected salt network forms at least one closed loop (cycle)
     */
    private boolean isLoopFormed(World world, Set<BlockPos> network) {
        if (network.size() < 4)
            return false; // Minimum 4 blocks for any loop (2x2)

        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos pos : network) {
            if (!visited.contains(pos)) {
                if (hasCycleDFS(world, pos, null, visited, network))
                    return true;
            }
        }
        return false;
    }

    /**
     * DFS cycle detection for undirected graph
     */
    private boolean hasCycleDFS(World world, BlockPos current, BlockPos parent, Set<BlockPos> visited,
            Set<BlockPos> network) {
        visited.add(current);

        for (BlockPos neighbor : getConnectedNeighbors(world, current)) {
            // Only consider salt blocks that were identified as part of the connected
            // component
            if (!network.contains(neighbor))
                continue;

            if (!visited.contains(neighbor)) {
                if (hasCycleDFS(world, neighbor, current, visited, network))
                    return true;
            } else if (!neighbor.equals(parent)) {
                // Found a visited neighbor that isn't the node we just came from -> cycle
                return true;
            }
        }
        return false;
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
                (minZ + maxZ) / 2);

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
                        1, 0, 0.05, 0, 0.02);
            }

            // Enchantment glyphs
            world.spawnParticles(
                    ParticleTypes.ENCHANT,
                    x, y + 0.5, z,
                    5, 0.3, 0.2, 0.3, 0.1);
        }

        // Extra dramatic effect at center
        world.spawnParticles(
                ParticleTypes.FLASH,
                center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
                1, 0, 0, 0, 0);

        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                20, 1.0, 0.5, 1.0, 0.02);
    }
}