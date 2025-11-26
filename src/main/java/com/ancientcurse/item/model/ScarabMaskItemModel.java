package com.ancientcurse.item.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.armor.ScarabMaskItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model for Scarab Mask item rendering (3D inventory/hand display)
 * Uses the same geo/texture as the armor renderer
 */
public class ScarabMaskItemModel extends GeoModel<ScarabMaskItem> {
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
