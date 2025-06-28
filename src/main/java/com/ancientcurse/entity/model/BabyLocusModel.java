package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.BabyLocusEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class BabyLocusModel extends GeoModel<BabyLocusEntity> {
    @Override
    public Identifier getModelResource(BabyLocusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/locus.geo.json");
    }

    @Override
    public Identifier getTextureResource(BabyLocusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/locus.png");
    }

    @Override
    public Identifier getAnimationResource(BabyLocusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/locus.animation.json");
    }
} 