package com.ancientcurse.item;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import com.ancientcurse.item.renderer.PurificationStaffRenderer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Staff used by players to cleanse cursed earth in a wave-based pattern
 */
public class PurificationStaffItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    // Physics state for the dangling piece
    public static class DangleState {
        public float angle, vel;
        public float angleX, velX;
        public float lastYaw, lastPitch;
        public double lastX, lastY, lastZ;
        public float bobTimer; // For rhythmic sway
        public double lastSmoothX, lastSmoothY, lastSmoothZ;
        public float lastSmoothYaw, lastSmoothPitch;
        public boolean initialized = false;
        public boolean wasInGui = false;
    }

    private final Long2ObjectMap<DangleState> dangleStates = new Long2ObjectOpenHashMap<>();

    public DangleState getOrCreateDangleState(long instanceId) {
        return dangleStates.computeIfAbsent(instanceId, id -> new DangleState());
    }

    private static final int CLEANSE_RADIUS = 8; // Reduced from 16
    private static final int CHANNEL_TIME = 60; // 3 seconds (reduced from 5)
    private static final int ANKH_COST = 10; // Reduced cost
    private static final int SALT_RADIUS = 2; // 5x5 area
    private static final int DURABILITY_COST = 5; // Uses 5 durability per cleanse

    // Active cleansing waves
    private static final Map<UUID, CleansingWave> activeWaves = new HashMap<>();

    public PurificationStaffItem(Settings settings) {
        super(settings.maxCount(1).maxDamage(100));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private PurificationStaffRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new PurificationStaffRenderer();
                return this.renderer;
            }
        });
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        // No default animations needed for now, handled by procedural physics
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot,
            boolean selected) {
        if (world.isClient && selected && entity instanceof PlayerEntity player) {
            // Spawn enchantment particles around the staff/player
            if (world.random.nextFloat() < 0.15f) { // Fairly often
                double angle = world.random.nextDouble() * Math.PI * 2;
                double dist = 0.5 + world.random.nextDouble() * 0.5;
                double px = player.getX() + Math.cos(angle) * dist;
                double pz = player.getZ() + Math.sin(angle) * dist;
                double py = player.getY() + 0.5 + world.random.nextDouble() * 1.5;

                world.addParticle(ParticleTypes.ENCHANT, px, py, pz,
                        (player.getX() - px) * 0.1,
                        (player.getY() + 1.2 - py) * 0.1,
                        (player.getZ() - pz) * 0.1);
            }
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient) {
            // Check if player is standing in valid salt circle
            if (!isInSaltCircle(world, player.getBlockPos())) {
                player.sendMessage(Text.translatable("item.ancientcurse.purification_staff.no_salt_circle")
                        .formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }

            // Check ankh points
            // TODO: Check ankh points when system is implemented

            // Start channeling
            player.setCurrentHand(hand);
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                    SoundCategory.PLAYERS, 0.5f, 1.0f);
        }

        return TypedActionResult.consume(stack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player))
            return;
        if (!world.isClient) {
            int useTime = this.getMaxUseTime(stack) - remainingUseTicks;

            // Show channeling particles
            if (useTime % 5 == 0) {
                ((ServerWorld) world).spawnParticles(
                        ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1, player.getZ(),
                        5, 0.5, 0.5, 0.5, 0.02);
            }

            // Complete channeling
            if (useTime >= CHANNEL_TIME) {
                startCleansingWave(world, player, stack);
                player.stopUsingItem();
            }
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000; // Arbitrary large number
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        return stack;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player))
            return;
        if (!world.isClient) {
            int useTime = this.getMaxUseTime(stack) - remainingUseTicks;
            if (useTime < CHANNEL_TIME) {
                player.sendMessage(Text.translatable("item.ancientcurse.purification_staff.interrupted")
                        .formatted(Formatting.YELLOW), true);
            }
        }
    }

    private boolean isInSaltCircle(World world, BlockPos center) {
        // Check for salt dust blocks in a square pattern at player ground level
        for (int x = -SALT_RADIUS; x <= SALT_RADIUS; x++) {
            for (int z = -SALT_RADIUS; z <= SALT_RADIUS; z++) {
                // Check the border of the square
                if (Math.abs(x) == SALT_RADIUS || Math.abs(z) == SALT_RADIUS) {
                    BlockPos checkPos = center.add(x, 0, z); // Check same level as player feet
                    BlockState state = world.getBlockState(checkPos);

                    // Check for salt dust blocks
                    if (!state.isOf(ModBlocks.SALT_DUST)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void startCleansingWave(World world, PlayerEntity player, ItemStack stack) {
        // Create new cleansing wave
        CleansingWave wave = new CleansingWave(world, player.getBlockPos(), CLEANSE_RADIUS);
        activeWaves.put(player.getUuid(), wave);

        // Consume resources
        stack.damage(DURABILITY_COST, player, p -> p.sendToolBreakStatus(player.getActiveHand()));
        // TODO: Consume ankh points

        // Effects
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
                SoundCategory.PLAYERS, 1.0f, 1.0f);

        player.sendMessage(Text.translatable("item.ancientcurse.purification_staff.wave_started")
                .formatted(Formatting.GREEN), true);
    }

    /**
     * Process active cleansing waves (called from server tick event)
     */
    public static void tickCleansingWaves(ServerWorld world) {
        Iterator<Map.Entry<UUID, CleansingWave>> iterator = activeWaves.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, CleansingWave> entry = iterator.next();
            CleansingWave wave = entry.getValue();

            if (wave.world == world) {
                wave.tick();

                if (wave.isComplete()) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.ancientcurse.purification_staff.tooltip")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.ancientcurse.purification_staff.tooltip1")
                .formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.translatable("item.ancientcurse.purification_staff.tooltip2")
                .formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.translatable("item.ancientcurse.purification_staff.tooltip3")
                .formatted(Formatting.DARK_PURPLE));
    }

    /**
     * Handles the wave-based cleansing logic
     */
    private static class CleansingWave {
        private final World world;
        private final BlockPos origin;
        private final int maxRadius;
        private Queue<BlockPos> currentWave = new LinkedList<>();
        private Queue<BlockPos> nextWave = new LinkedList<>();
        private Set<BlockPos> processed = new HashSet<>();
        private int ticksExisted = 0;
        private int blocksCleansed = 0;
        private static final int BLOCKS_PER_TICK = 10;

        public CleansingWave(World world, BlockPos origin, int maxRadius) {
            this.world = world;
            this.origin = origin;
            this.maxRadius = maxRadius;

            // Start with origin
            currentWave.add(origin.up()); // Start at player level
        }

        public void tick() {
            ticksExisted++;

            // Process current wave
            for (int i = 0; i < BLOCKS_PER_TICK && !currentWave.isEmpty(); i++) {
                BlockPos pos = currentWave.poll();

                if (processed.contains(pos))
                    continue;
                processed.add(pos);

                // Check distance
                if (pos.getSquaredDistance(origin) > maxRadius * maxRadius)
                    continue;

                // Try to cleanse
                if (cleanseBlock(pos)) {
                    blocksCleansed++;

                    // Add neighbors to next wave
                    for (BlockPos neighbor : getHorizontalNeighbors(pos)) {
                        if (!processed.contains(neighbor)) {
                            nextWave.add(neighbor);
                        }
                    }
                }
            }

            // Move to next wave if current is empty
            if (currentWave.isEmpty() && !nextWave.isEmpty()) {
                currentWave = nextWave;
                nextWave = new LinkedList<>();

                // Wave expansion effect
                if (world instanceof ServerWorld serverWorld) {
                    for (BlockPos pos : currentWave) {
                        if (world.random.nextFloat() < 0.3f) {
                            serverWorld.spawnParticles(
                                    ParticleTypes.END_ROD,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    1, 0, 0, 0, 0.05);
                        }
                    }
                }
            }
        }

        private boolean cleanseBlock(BlockPos pos) {
            BlockState state = world.getBlockState(pos);

            // Check if it's cursed earth
            if (state.getBlock() == ModBlocks.CURSED_EARTH) {
                // TODO: Restore original block from stored data
                // For now, just replace with grass
                world.setBlockState(pos, net.minecraft.block.Blocks.GRASS_BLOCK.getDefaultState());

                // Particles
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                            3, 0.25, 0.25, 0.25, 0);
                }

                return true;
            }

            return false;
        }

        private List<BlockPos> getHorizontalNeighbors(BlockPos pos) {
            return Arrays.asList(
                    pos.north(),
                    pos.south(),
                    pos.east(),
                    pos.west());
        }

        public boolean isComplete() {
            return currentWave.isEmpty() && nextWave.isEmpty();
        }
    }
}