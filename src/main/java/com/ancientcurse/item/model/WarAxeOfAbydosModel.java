package com.ancientcurse.item.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.WarAxeOfAbydosItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class WarAxeOfAbydosModel extends GeoModel<WarAxeOfAbydosItem> {
    @Override
    public Identifier getModelResource(WarAxeOfAbydosItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/waraxe_of_abydos.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(WarAxeOfAbydosItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/item/waraxe_of_abydos.png");
    }
    
    @Override
    public Identifier getAnimationResource(WarAxeOfAbydosItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/waraxe_of_abydos.animation.json");
    }
}