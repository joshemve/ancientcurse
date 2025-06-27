package com.example.egyptianweapons.client.model;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.items.WarAxe;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model class for the War Axe.
 * This class is responsible for providing the model, texture, and animation resources
 * for the War Axe item.
 */
public class WarAxeModel extends GeoModel<WarAxe> {
    @Override
    public Identifier getModelResource(WarAxe animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "geo/war_axe.geo.json");
    }

    @Override
    public Identifier getTextureResource(WarAxe animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/war_axe.png");
    }

    @Override
    public Identifier getAnimationResource(WarAxe animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "animations/war_axe.animation.json");
    }
}
