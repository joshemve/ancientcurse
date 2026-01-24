package com.ancientcurse.mixin.client;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.client.animation.IAnimatedPlayer;
import com.mojang.authlib.GameProfile;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add animation layer support to client player entities.
 * This enables custom player animations for combat moves like the War Axe of Abydos spin attack.
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class PlayerAnimationMixin extends PlayerEntity implements IAnimatedPlayer {

    /**
     * The animation layer for combat/special ability animations.
     * Priority 1000 ensures our animations override most others.
     */
    @Unique
    private final ModifierLayer<IAnimation> ancientcurse_animationLayer = new ModifierLayer<>();

    public PlayerAnimationMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    /**
     * Register the animation layer when the client player entity is initialized.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void ancientcurse_initAnimationLayer(ClientWorld world, GameProfile profile, CallbackInfo ci) {
        try {
            AncientCurse.LOGGER.info("[PlayerAnimationMixin] Initializing animation layer for player: " + profile.getName());
            var animationStack = PlayerAnimationAccess.getPlayerAnimLayer((AbstractClientPlayerEntity) (Object) this);
            if (animationStack != null) {
                // Priority 1000 - higher than most other mods
                animationStack.addAnimLayer(1000, ancientcurse_animationLayer);
                AncientCurse.LOGGER.info("[PlayerAnimationMixin] Successfully added animation layer to stack for: " + profile.getName());
            } else {
                AncientCurse.LOGGER.warn("[PlayerAnimationMixin] Animation stack is null for: " + profile.getName());
            }
        } catch (Exception e) {
            AncientCurse.LOGGER.error("[PlayerAnimationMixin] Failed to initialize animation layer", e);
        }
    }

    @Override
    public ModifierLayer<IAnimation> ancientcurse_getAnimationLayer() {
        return ancientcurse_animationLayer;
    }
}
