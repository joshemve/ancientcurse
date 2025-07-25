package com.ancientcurse.client.model.armor;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.CeremonialChestwrapItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class CeremonialChestwrapModel extends GeoModel<CeremonialChestwrapItem> {
    @Override
    public Identifier getModelResource(CeremonialChestwrapItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/ceremonial_chestwrap.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(CeremonialChestwrapItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/armor/ceremonial_chestwrap.png");
    }
    
    @Override
    public Identifier getAnimationResource(CeremonialChestwrapItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/ceremonial_chestwrap.animation.json");
    }
}