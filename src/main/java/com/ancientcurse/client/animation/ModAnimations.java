package com.ancientcurse.client.animation;

import com.ancientcurse.AncientCurse;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Handles player animation registration for the mod.
 * Animations are auto-loaded by PlayerAnimator from assets/ancientcurse/player_animation/
 *
 * IMPORTANT: The animation name inside the JSON file determines the registry key!
 * For example, if the animation name is "waraxe_spin_attack", the registry key is:
 * ancientcurse:waraxe_spin_attack
 */
@Environment(EnvType.CLIENT)
public class ModAnimations {

    /**
     * Called during client initialization to log animation registration.
     * PlayerAnimator auto-loads .json files from assets/<modid>/player_animation/ folder.
     */
    public static void register() {
        AncientCurse.LOGGER.info("[ModAnimations] Player animations will be loaded from assets/ancientcurse/player_animation/");
        AncientCurse.LOGGER.info("[ModAnimations] Animation files should have animation names that match the desired registry key");
        AncientCurse.LOGGER.info("[ModAnimations] Available animations: waraxe_spin_attack (registry: ancientcurse:waraxe_spin_attack)");
    }
}
