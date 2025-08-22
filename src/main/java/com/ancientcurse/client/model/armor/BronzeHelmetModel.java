package com.ancientcurse.client.model.armor;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.armor.BronzeHelmetItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class BronzeHelmetModel extends GeoModel<BronzeHelmetItem> {
    @Override
    public Identifier getModelResource(BronzeHelmetItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/bronze_helmet.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(BronzeHelmetItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/armor/bronze_helmet.png");
    }
    
    @Override
    public Identifier getAnimationResource(BronzeHelmetItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/bronze_helmet.animation.json");
    }
}