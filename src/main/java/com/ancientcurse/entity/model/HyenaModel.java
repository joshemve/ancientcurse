package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.HyenaEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Hyena entity.
 * Handles texture variants and animation resources.
 */
public class HyenaModel extends GeoModel<HyenaEntity> {

    /* ---------- RESOURCE PATHS ---------- */
    private static final Identifier MODEL = new Identifier(AncientCurse.MOD_ID, "geo/hyena.geo.json");
    private static final Identifier ANIMATION = new Identifier(AncientCurse.MOD_ID, "animations/hyena.animation.json");

    // Texture variants
    private static final Identifier[] TEXTURES = {
            new Identifier(AncientCurse.MOD_ID, "textures/entity/hyena/hyena.default.png"),  // VARIANT_DEFAULT
            new Identifier(AncientCurse.MOD_ID, "textures/entity/hyena/hyena.sandy.png"),    // VARIANT_SANDY
            new Identifier(AncientCurse.MOD_ID, "textures/entity/hyena/hyena.rusty.png"),    // VARIANT_RUSTY
            new Identifier(AncientCurse.MOD_ID, "textures/entity/hyena/hyena.red.png"),      // VARIANT_RED
            new Identifier(AncientCurse.MOD_ID, "textures/entity/hyena/hyena.leucistic.png"),// VARIANT_LEUCISTIC
            new Identifier(AncientCurse.MOD_ID, "textures/entity/hyena/hyena.black.png")     // VARIANT_BLACK
    };

    @Override
    public Identifier getModelResource(HyenaEntity entity) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(HyenaEntity entity) {
        int variant = entity.getVariant();
        if (variant >= 0 && variant < TEXTURES.length) {
            return TEXTURES[variant];
        }
        return TEXTURES[0]; // Default fallback
    }

    @Override
    public Identifier getAnimationResource(HyenaEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(HyenaEntity entity, long instanceId, AnimationState<HyenaEntity> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        // All animations are handled by GeckoLib from the JSON file
        // No additional procedural animations needed
    }
}
