package com.ancientcurse.entity;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
import com.ancientcurse.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Ra Entity - The Egyptian God of the Sun
 * A powerful boss entity with wings and the ability to wield the Staff of Ra
 * Ready for future development with additional animations and attacks
 */
public class RaEntity extends HostileEntity implements GeoEntity {

    /* ---------- FIELDS ---------- */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ServerBossBar bossBar;

    /* ---------- CONSTRUCTOR ---------- */
    public RaEntity(EntityType<? extends RaEntity> entityType, World world) {
        super(entityType, world);
    }

    /* ---------- INITIALIZATION ---------- */
    @Override
    protected void initGoals() {
        // AI goals for future development
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0d));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
    }

    /* ---------- ATTRIBUTES ---------- */
    public static DefaultAttributeContainer.Builder createRaAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.5);
    }

    /* ---------- BOSS BAR ---------- */
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        if (this.bossBar == null) {
            this.bossBar = new ServerBossBar(Text.literal("Ra"), BossBar.Color.YELLOW, BossBar.Style.NOTCHED_20);
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        }
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        if (this.bossBar != null) {
            this.bossBar.removePlayer(player);
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.bossBar != null) {
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        }
    }

    /* ---------- DAMAGE & DEATH ---------- */
    @Override
    public void onDeath(DamageSource damageSource) {
        if (!this.getWorld().isClient && damageSource.getAttacker() instanceof PlayerEntity) {
            this.dropLoot(damageSource, true);
        }
        super.onDeath(damageSource);
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);
        if (causedByPlayer && this.random.nextFloat() < 0.8f) {
            this.dropStack(new ItemStack(ModItems.STAFF_OF_RA, 1));
        }
    }

    /* ---------- SOUNDS ---------- */
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_GENERIC_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_GENERIC_DEATH;
    }

    /* ---------- GECKOLIB ANIMATION ---------- */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<RaEntity> event) {
        // Play idle animation
        event.getController().setAnimation(RawAnimation.begin().then("ra.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
