package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.BabyLocusEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Baby Locus Entity
 * 
 * Reuses the same geometry and animations as the adult Locus
 * but scaled down in the renderer
 */
public class BabyLocusModel extends GeoModel<BabyLocusEntity> {

    @Override
    public Identifier getModelResource(BabyLocusEntity animatable) {
        // Reuse the adult locus model - scaling is handled in renderer
        return new Identifier(AncientCurse.MOD_ID, "geo/locus.geo.json");
    }

    @Override
    public Identifier getTextureResource(BabyLocusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/baby_locus.png");
    }

    @Override
    public Identifier getAnimationResource(BabyLocusEntity animatable) {
        // Reuse adult animations - they work well for babies too
        return new Identifier(AncientCurse.MOD_ID, "animations/locus.animation.json");
    }
} 