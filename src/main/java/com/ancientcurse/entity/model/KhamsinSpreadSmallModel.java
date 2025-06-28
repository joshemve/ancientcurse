package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.KhamsinSpreadSmallEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class KhamsinSpreadSmallModel extends GeoModel<KhamsinSpreadSmallEntity> {
    
    // Model, texture, and animation resource paths
    private static final Identifier MODEL = new Identifier(AncientCurse.MOD_ID, "geo/khamsin_spread_small.geo.json");
    private static final Identifier TEXTURE_NORMAL = new Identifier(AncientCurse.MOD_ID, "textures/entity/khamsin_spread_small.png");
    private static final Identifier TEXTURE_DARK = new Identifier(AncientCurse.MOD_ID, "textures/entity/khamsin_spread_small_dark.png");
    private static final Identifier ANIM = new Identifier(AncientCurse.MOD_ID, "animations/khamsin_spread_small.animation.json");

    @Override 
    public Identifier getModelResource(KhamsinSpreadSmallEntity entity) { 
        return MODEL; 
    }

    @Override 
    public Identifier getTextureResource(KhamsinSpreadSmallEntity entity) { 
        // Use dark texture when dormant, normal when activated with smooth transitions
        if (entity.isActivated()) {
            // Use pulse intensity for smoother texture transitions
            // Show normal texture when pulse intensity is above 0.4 (smoother than boolean)
            float pulseIntensity = entity.getPulseIntensity();
            return pulseIntensity > 0.4f ? TEXTURE_NORMAL : TEXTURE_DARK;
        } else {
            // Always use dark texture when dormant/unactivated
            return TEXTURE_DARK;
        }
    }

    @Override 
    public Identifier getAnimationResource(KhamsinSpreadSmallEntity entity) { 
        return ANIM; 
    }
} 