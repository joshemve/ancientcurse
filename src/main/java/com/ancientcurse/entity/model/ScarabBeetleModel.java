package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.ScarabBeetleEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Scarab Beetle entity.
 * References the geo and animation files for a detailed beetle with legs, pinchers, and antennas.
 */
public class ScarabBeetleModel extends GeoModel<ScarabBeetleEntity> {
    
    @Override
    public Identifier getModelResource(ScarabBeetleEntity entity) {
        return new Identifier(AncientCurse.MOD_ID, "geo/scarab_beetle.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(ScarabBeetleEntity entity) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/scarab_beetle.png");
    }
    
    @Override
    public Identifier getAnimationResource(ScarabBeetleEntity entity) {
        return new Identifier(AncientCurse.MOD_ID, "animations/scarab_beetle.animation.json");
    }
} 