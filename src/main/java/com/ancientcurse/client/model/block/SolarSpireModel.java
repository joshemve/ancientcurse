package com.ancientcurse.client.model.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.block.entity.SolarSpireBlockEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class SolarSpireModel extends GeoModel<SolarSpireBlockEntity> {
    
    @Override
    public Identifier getModelResource(SolarSpireBlockEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/solarspire.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(SolarSpireBlockEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/block/solarspire.png");
    }
    
    @Override
    public Identifier getAnimationResource(SolarSpireBlockEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/solarspire.animation.json");
    }
}