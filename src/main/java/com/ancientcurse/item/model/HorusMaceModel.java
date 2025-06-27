package com.ancientcurse.item.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.HorusMaceItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model for the Horus Mace GeckoLib item
 */
public class HorusMaceModel extends GeoModel<HorusMaceItem> {
    @Override
    public Identifier getModelResource(HorusMaceItem object) {
        return new Identifier(AncientCurse.MOD_ID, "geo/horus_mace.geo.json");
    }

    @Override
    public Identifier getTextureResource(HorusMaceItem object) {
        return new Identifier(AncientCurse.MOD_ID, "textures/item/horus_mace.png");
    }

    @Override
    public Identifier getAnimationResource(HorusMaceItem animatable) {
        // No animations for the item itself
        return new Identifier(AncientCurse.MOD_ID, "animations/empty.animation.json");
    }
}