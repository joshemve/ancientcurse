package com.ancientcurse.client.model.armor;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.armor.VeilOfAnubisItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class VeilOfAnubisModel extends GeoModel<VeilOfAnubisItem> {
    @Override
    public Identifier getModelResource(VeilOfAnubisItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/veil_of_anubis.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(VeilOfAnubisItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/armor/veil_of_anubis.png");
    }
    
    @Override
    public Identifier getAnimationResource(VeilOfAnubisItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/veil_of_anubis.animation.json");
    }
}