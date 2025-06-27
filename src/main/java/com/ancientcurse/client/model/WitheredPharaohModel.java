package com.ancientcurse.client.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.WitheredPharaohEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model for the Withered Pharaoh entity
 */
public class WitheredPharaohModel extends GeoModel<WitheredPharaohEntity> {
    @Override
    public Identifier getModelResource(WitheredPharaohEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/the_withered_pharaoh.geo.json");
    }

    @Override
    public Identifier getTextureResource(WitheredPharaohEntity entity) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/the_withered_pharaoh.png");
    }

    @Override
    public Identifier getAnimationResource(WitheredPharaohEntity entity) {
        return new Identifier(AncientCurse.MOD_ID, "animations/the_withered_pharaoh.animation.json");
    }
}
