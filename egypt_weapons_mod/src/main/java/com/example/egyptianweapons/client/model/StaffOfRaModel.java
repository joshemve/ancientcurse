package com.example.egyptianweapons.client.model;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.items.StaffOfRa;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model class for the Staff of Ra.
 * This class is responsible for providing the model, texture, and animation resources
 * for the Staff of Ra item.
 */
public class StaffOfRaModel extends GeoModel<StaffOfRa> {
    @Override
    public Identifier getModelResource(StaffOfRa animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "geo/staff_of_souls.geo.json");
    }

    @Override
    public Identifier getTextureResource(StaffOfRa animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/staff_of_souls.png");
    }

    @Override
    public Identifier getAnimationResource(StaffOfRa animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "animations/staff_of_souls.animation.json");
    }
}
