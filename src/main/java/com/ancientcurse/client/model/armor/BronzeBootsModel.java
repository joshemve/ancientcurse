package com.ancientcurse.client.model.armor;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.armor.BronzeBootsItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class BronzeBootsModel extends GeoModel<BronzeBootsItem> {
    @Override
    public Identifier getModelResource(BronzeBootsItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/bronze_boots.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(BronzeBootsItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/armor/bronze_boots.png");
    }
    
    @Override
    public Identifier getAnimationResource(BronzeBootsItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/bronze_boots.animation.json");
    }
}