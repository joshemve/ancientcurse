package com.ancientcurse.item.armor;

import com.ancientcurse.client.renderer.armor.JackelBindsRenderer;
import com.ancientcurse.material.JackelBindsArmorMaterial;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * Jackel Binds - Restraining handcuffs that can be placed on players
 * - Right-click on a player to attempt to bind them
 * - Has a windup/drawback before successfully latching
 * - Replaces their chestplate with the binds
 * - Applies negative effects while worn (slowness, mining fatigue, no attacking)
 */
public class JackelBindsItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Binding mechanics
    private static final int BINDING_TIME_TICKS = 40; // 2 seconds to bind
    private static final double BINDING_RANGE = 2.5; // Must stay close to bind

    public JackelBindsItem() {
        super(new JackelBindsArmorMaterial(), Type.CHESTPLATE, new FabricItemSettings().maxCount(1));
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new JackelBindsRenderProvider());
    }

    private static class JackelBindsRenderProvider implements RenderProvider {
        private JackelBindsRenderer armorRenderer;

        @Override
        public BipedEntityModel<LivingEntity> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, BipedEntityModel<LivingEntity> original) {
            if (this.armorRenderer == null)
                this.armorRenderer = new JackelBindsRenderer();

            this.armorRenderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
            return this.armorRenderer;
        }
    }

    private final java.util.function.Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    @Override
    public java.util.function.Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<JackelBindsItem> animationState) {
        animationState.getController().setAnimation(RawAnimation.begin().thenLoop("animation.jackel_binds.idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * Override the default armor equipping behavior to prevent self-equipping
     */
    @Override
    public net.minecraft.util.TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // Do NOT allow equipping on yourself - only works on other players via useOnEntity
        // Play rope sound to indicate it can't be used on yourself
        if (!world.isClient) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 0.6f, 0.8f);
        }
        return net.minecraft.util.TypedActionResult.fail(user.getStackInHand(hand));
    }

    /**
     * Called when right-clicking on an entity
     */
    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof PlayerEntity target)) {
            return ActionResult.PASS;
        }

        World world = user.getWorld();

        // Can't bind yourself
        if (target == user) {
            if (!world.isClient) {
                user.sendMessage(Text.literal("You cannot bind yourself!").formatted(Formatting.RED), true);
            }
            return ActionResult.FAIL;
        }

        // Check if target already has binds
        ItemStack targetChestplate = target.getEquippedStack(EquipmentSlot.CHEST);
        if (targetChestplate.getItem() instanceof JackelBindsItem) {
            if (!world.isClient) {
                user.sendMessage(Text.literal("Target is already bound!").formatted(Formatting.YELLOW), true);
            }
            return ActionResult.FAIL;
        }

        // Start binding process - user must hold right click
        if (!world.isClient) {
            // Apply slowness to the user while they're trying to bind (drawback)
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, BINDING_TIME_TICKS + 20, 2, false, false));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, BINDING_TIME_TICKS + 20, 2, false, false));

            // Store binding data in NBT
            stack.getOrCreateNbt().putUuid("BindingTarget", target.getUuid());
            stack.getOrCreateNbt().putLong("BindingStartTime", world.getTime());
            stack.getOrCreateNbt().putDouble("BindingStartX", user.getX());
            stack.getOrCreateNbt().putDouble("BindingStartY", user.getY());
            stack.getOrCreateNbt().putDouble("BindingStartZ", user.getZ());

            user.sendMessage(Text.literal("Attempting to bind " + target.getName().getString() + "... Hold right-click!").formatted(Formatting.GOLD), true);

            // Play starting sound (rope tightening)
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 1.0f, 0.6f);

            // Start using the item (enables holding mechanic)
            user.setCurrentHand(hand);
        }

        return ActionResult.SUCCESS;
    }

    /**
     * Returns the maximum use time for holding right-click (in ticks)
     */
    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000; // Allow holding for a long time (1 hour in ticks)
    }

    /**
     * Returns the use action animation
     */
    @Override
    public net.minecraft.util.UseAction getUseAction(ItemStack stack) {
        return net.minecraft.util.UseAction.BOW; // Bow animation looks good for restraining
    }

    /**
     * Called when player stops using the item (releases right-click)
     */
    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient || !(user instanceof PlayerEntity player)) return;

        // If binding was in progress, cancel it
        if (stack.hasNbt() && stack.getNbt().containsUuid("BindingTarget")) {
            long bindingStartTime = stack.getNbt().getLong("BindingStartTime");
            long elapsed = world.getTime() - bindingStartTime;

            // Only cancel if binding wasn't completed yet
            if (elapsed < BINDING_TIME_TICKS) {
                cancelBinding(stack, player, "Binding cancelled - you released right-click!");
            }
        }
    }

    /**
     * Called every tick while the item is in the player's inventory
     */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClient || !(entity instanceof PlayerEntity user)) return;
        if (!stack.hasNbt() || !stack.getNbt().containsUuid("BindingTarget")) return;

        // Check if binding is in progress
        long bindingStartTime = stack.getNbt().getLong("BindingStartTime");
        long currentTime = world.getTime();
        long elapsed = currentTime - bindingStartTime;

        // Check if the item is still being held and selected (must hold right-click continuously)
        ItemStack activeStack = user.getActiveItem();
        boolean isHoldingItem = user.isUsingItem() && activeStack == stack;

        if (!isHoldingItem && elapsed < BINDING_TIME_TICKS) {
            cancelBinding(stack, user, "Binding cancelled - you must hold right-click!");
            return;
        }

        // Timeout after 5 seconds if not completed
        if (elapsed > 100) {
            cancelBinding(stack, user, "Binding timed out!");
            return;
        }

        // Check if user moved too much
        double startX = stack.getNbt().getDouble("BindingStartX");
        double startY = stack.getNbt().getDouble("BindingStartY");
        double startZ = stack.getNbt().getDouble("BindingStartZ");
        double distMoved = Math.sqrt(
                Math.pow(user.getX() - startX, 2) +
                        Math.pow(user.getY() - startY, 2) +
                        Math.pow(user.getZ() - startZ, 2)
        );

        if (distMoved > 1.5) {
            cancelBinding(stack, user, "You moved too much! Binding failed.");
            return;
        }

        // Find target
        PlayerEntity target = world.getServer().getPlayerManager().getPlayer(stack.getNbt().getUuid("BindingTarget"));
        if (target == null || !target.isAlive()) {
            cancelBinding(stack, user, "Target lost!");
            return;
        }

        // Check distance to target
        if (user.distanceTo(target) > BINDING_RANGE) {
            cancelBinding(stack, user, "Target moved too far away!");
            return;
        }

        // Spawn progress particles
        if (elapsed % 5 == 0 && world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    3, 0.3, 0.5, 0.3, 0.01);
        }

        // Check if binding is complete
        if (elapsed >= BINDING_TIME_TICKS) {
            completeBinding(stack, user, target, (ServerWorld) world);
        }
    }

    private void cancelBinding(ItemStack stack, PlayerEntity user, String reason) {
        stack.getNbt().remove("BindingTarget");
        stack.getNbt().remove("BindingStartTime");
        stack.getNbt().remove("BindingStartX");
        stack.getNbt().remove("BindingStartY");
        stack.getNbt().remove("BindingStartZ");

        user.sendMessage(Text.literal(reason).formatted(Formatting.RED), true);

        // Play fail sound (rope loosening)
        user.getWorld().playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLOCK_WOOL_BREAK, SoundCategory.PLAYERS, 1.0f, 0.7f);
    }

    private void completeBinding(ItemStack stack, PlayerEntity user, PlayerEntity target, ServerWorld world) {
        // Clear binding state
        stack.getNbt().remove("BindingTarget");
        stack.getNbt().remove("BindingStartTime");
        stack.getNbt().remove("BindingStartX");
        stack.getNbt().remove("BindingStartY");
        stack.getNbt().remove("BindingStartZ");

        // Get target's current chestplate
        ItemStack targetChestplate = target.getEquippedStack(EquipmentSlot.CHEST);

        // Drop their chestplate on the ground
        if (!targetChestplate.isEmpty()) {
            target.dropStack(targetChestplate);
        }

        // Remove binds from user's hand and place on target
        ItemStack bindsToApply = stack.copy();
        bindsToApply.setCount(1);
        stack.decrement(1);

        target.equipStack(EquipmentSlot.CHEST, bindsToApply);

        // Close any open screen on the target (server-side)
        if (target instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            serverPlayer.closeHandledScreen();
        }

        // Visual and audio feedback (rope binding sound)
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 1.2f, 0.5f);

        // Minimal particles - just the sound is the main feedback
        world.spawnParticles(ParticleTypes.SOUL,
                target.getX(), target.getY() + 1.0, target.getZ(),
                5, 0.3, 0.3, 0.3, 0.02);

        // Notify both players
        user.sendMessage(Text.literal("Successfully bound " + target.getName().getString() + "!").formatted(Formatting.GREEN), true);
        target.sendMessage(Text.literal("You have been bound by " + user.getName().getString() + "!").formatted(Formatting.RED), true);
    }

    /**
     * Apply effects while wearing the binds - no particles, direct restrictions via mixins
     */
    public static void applyBindEffects(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity player)) return;

        ItemStack chestpiece = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chestpiece.getItem() instanceof JackelBindsItem)) return;

        // Apply only slowness without particles (ambient=true, showParticles=false, showIcon=true)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, true, false, true));

        // Can't sprint while bound
        if (player.isSprinting()) {
            player.setSprinting(false);
        }

        // Close any open screens (except chat) - handled by mixin on client side
    }

    /**
     * Check if a player is currently bound
     */
    public static boolean isPlayerBound(PlayerEntity player) {
        if (player == null) return false;
        ItemStack chestpiece = player.getEquippedStack(EquipmentSlot.CHEST);
        return chestpiece.getItem() instanceof JackelBindsItem;
    }

    /**
     * Prevent the player from easily removing the binds
     * This is called from a mixin or event handler
     */
    public static boolean canRemoveBinds(PlayerEntity player) {
        // Binds can only be removed with a special item or by another player
        // For now, they can be removed in creative mode
        return player.isCreative();
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<net.minecraft.text.Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("Hold right-click on player to bind").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Punch repeatedly to escape").formatted(Formatting.DARK_GRAY));
    }
}
