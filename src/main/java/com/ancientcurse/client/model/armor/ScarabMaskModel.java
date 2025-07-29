package com.ancientcurse.client.model.armor;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.armor.ScarabMaskItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class ScarabMaskModel extends GeoModel<ScarabMaskItem> {
    @Override
    public Identifier getModelResource(ScarabMaskItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/scarab_mask.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(ScarabMaskItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/armor/scarab_mask.png");
    }
    
    @Override
    public Identifier getAnimationResource(ScarabMaskItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/scarab_mask.animation.json");
    }
}