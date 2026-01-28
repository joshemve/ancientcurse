package com.ancientcurse.client.animation;

import com.ancientcurse.AncientCurse;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Handles player animations for combat moves and special abilities.
 * Client-side only - uses PlayerAnimator library.
 */
@Environment(EnvType.CLIENT)
public class PlayerAnimationHandler {
    // Animation identifiers
    public static final Identifier WARAXE_SPIN_ATTACK = new Identifier(AncientCurse.MOD_ID, "waraxe_spin_attack");

    /**
     * Plays an animation on a player.
     *
     * @param player The player to animate
     * @param animationId The animation identifier
     * @return true if animation was successfully started
     */
    public static boolean playAnimation(AbstractClientPlayerEntity player, Identifier animationId) {
        if (player == null) {
            AncientCurse.LOGGER.info("[PlayerAnimation] Player is null, cannot play animation");
            return false;
        }

        AncientCurse.LOGGER.info("[PlayerAnimation] Attempting to play animation: " + animationId + " on player: " + player.getName().getString());

        try {
            // Get the animation layer from our mixin
            if (player instanceof IAnimatedPlayer animatedPlayer) {
                AncientCurse.LOGGER.info("[PlayerAnimation] Player implements IAnimatedPlayer interface");
                ModifierLayer<IAnimation> animationLayer = animatedPlayer.ancientcurse_getAnimationLayer();

                if (animationLayer != null) {
                    AncientCurse.LOGGER.info("[PlayerAnimation] Animation layer found, looking up animation in registry...");
                    // Get the animation from registry
                    KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(animationId);
                    if (animation != null) {
                        AncientCurse.LOGGER.info("[PlayerAnimation] Animation found! Setting animation on layer...");

                        // Stop any currently playing animation first to ensure clean transition
                        animationLayer.setAnimation(null);

                        // Create animation player
                        // The animation JSON now ends at 0° rotation (same as default pose)
                        // This prevents the reverse spin that was caused by -360° → 0° interpolation
                        KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(animation);
                        // Use THIRD_PERSON_MODEL so player can see their own animation in F5 view
                        animPlayer.setFirstPersonMode(dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.THIRD_PERSON_MODEL);
                        animationLayer.setAnimation(animPlayer);

                        AncientCurse.LOGGER.info("[PlayerAnimation] SUCCESS - Animation " + animationId + " is now playing on " + player.getName().getString());
                        return true;
                    } else {
                        AncientCurse.LOGGER.warn("[PlayerAnimation] Animation NOT FOUND in registry: " + animationId);
                        AncientCurse.LOGGER.warn("[PlayerAnimation] Make sure the file exists at: assets/ancientcurse/player_animation/waraxe_spin_attack.json");
                    }
                } else {
                    AncientCurse.LOGGER.warn("[PlayerAnimation] Animation layer is NULL for player: " + player.getName().getString());
                    AncientCurse.LOGGER.warn("[PlayerAnimation] The mixin may not have initialized the layer properly");
                }
            } else {
                AncientCurse.LOGGER.warn("[PlayerAnimation] Player does NOT implement IAnimatedPlayer - mixin not applied!");
                AncientCurse.LOGGER.warn("[PlayerAnimation] Player class: " + player.getClass().getName());
            }
        } catch (Exception e) {
            AncientCurse.LOGGER.error("[PlayerAnimation] Exception while playing animation: " + animationId, e);
        }

        return false;
    }

    /**
     * Stops any currently playing animation on a player.
     *
     * @param player The player to stop animating
     */
    public static void stopAnimation(AbstractClientPlayerEntity player) {
        if (player == null) {
            return;
        }

        try {
            if (player instanceof IAnimatedPlayer animatedPlayer) {
                ModifierLayer<IAnimation> animationLayer = animatedPlayer.ancientcurse_getAnimationLayer();
                if (animationLayer != null) {
                    animationLayer.setAnimation(null);
                }
            }
        } catch (Exception e) {
            AncientCurse.LOGGER.warn("Failed to stop animation: " + e.getMessage());
        }
    }

    /**
     * Plays the War Axe of Abydos spin attack animation.
     *
     * @param player The player performing the attack
     * @return true if animation was successfully started
     */
    public static boolean playWaraxeSpinAttack(AbstractClientPlayerEntity player) {
        return playAnimation(player, WARAXE_SPIN_ATTACK);
    }

    /**
     * Checks if an animation is currently playing on a player.
     *
     * @param player The player to check
     * @return true if an animation is playing
     */
    public static boolean isAnimationPlaying(AbstractClientPlayerEntity player) {
        if (player == null) {
            return false;
        }

        try {
            if (player instanceof IAnimatedPlayer animatedPlayer) {
                ModifierLayer<IAnimation> animationLayer = animatedPlayer.ancientcurse_getAnimationLayer();
                if (animationLayer != null) {
                    return animationLayer.isActive();
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return false;
    }
}
