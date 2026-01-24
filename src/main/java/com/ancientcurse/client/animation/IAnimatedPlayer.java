package com.ancientcurse.client.animation;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;

/**
 * Interface for accessing the animation layer on player entities.
 * Implemented via mixin on AbstractClientPlayerEntity.
 */
public interface IAnimatedPlayer {
    /**
     * Get the combat animation layer for this player.
     * @return The ModifierLayer for combat animations
     */
    ModifierLayer<IAnimation> ancientcurse_getAnimationLayer();
}
