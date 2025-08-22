package com.ancientcurse.client.model.armor;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.armor.BronzeLeggingsItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class BronzeLeggingsModel extends GeoModel<BronzeLeggingsItem> {
    @Override
    public Identifier getModelResource(BronzeLeggingsItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/bronze_leggings.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(BronzeLeggingsItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/armor/bronze_leggings.png");
    }
    
    @Override
    public Identifier getAnimationResource(BronzeLeggingsItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/bronze_leggings.animation.json");
    }
}