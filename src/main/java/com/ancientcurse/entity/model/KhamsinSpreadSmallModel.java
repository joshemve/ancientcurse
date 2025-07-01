package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.KhamsinSpreadSmallEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class KhamsinSpreadSmallModel extends GeoModel<KhamsinSpreadSmallEntity> {
    
    // Model, texture, and animation resource paths
    private static final Identifier MODEL = new Identifier(AncientCurse.MOD_ID, "geo/khamsin_spread_small.geo.json");
    private static final Identifier TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/khamsin_spread_small.png");
    private static final Identifier ANIMATION = new Identifier(AncientCurse.MOD_ID, "animations/khamsin_spread_small.animation.json");

    @Override 
    public Identifier getModelResource(KhamsinSpreadSmallEntity entity) { 
        return MODEL; 
    }

    @Override 
    public Identifier getTextureResource(KhamsinSpreadSmallEntity entity) { 
        // The actual darkening effect is handled in the renderer
        return TEXTURE;
    }

    @Override 
    public Identifier getAnimationResource(KhamsinSpreadSmallEntity entity) { 
        return ANIMATION; 
    }
} 