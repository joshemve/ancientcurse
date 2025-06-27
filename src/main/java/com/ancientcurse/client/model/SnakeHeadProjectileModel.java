package com.ancientcurse.client.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.SnakeHeadProjectileEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class SnakeHeadProjectileModel extends GeoModel<SnakeHeadProjectileEntity> {
    
    @Override
    public Identifier getModelResource(SnakeHeadProjectileEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/snake_head.geo.json");
    }

    @Override
    public Identifier getTextureResource(SnakeHeadProjectileEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/snake_head.png");
    }

    @Override
    public Identifier getAnimationResource(SnakeHeadProjectileEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/snake_head.animation.json");
    }
} 