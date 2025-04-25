package com.ancientcurse.mixin;

import net.minecraft.client.texture.StatusEffectSpriteManager;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin for status effect sprites
 * We're now using the standard Minecraft status effect system
 */
@Mixin(StatusEffectSpriteManager.class)
public class StatusEffectSpriteManagerMixin {
    
    // This mixin is now empty as we're using the standard Minecraft status effect system
    // The textures are loaded from the mob_effect folder automatically
    
}
