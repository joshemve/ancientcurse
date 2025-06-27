package com.example.egyptianweapons.client.model;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.items.SerpentStaff;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model class for the Staff of Thoth (Serpent Staff).
 * This class is responsible for providing the model, texture, and animation resources
 * for the Serpent Staff item.
 */
public class SerpentStaffModel extends GeoModel<SerpentStaff> {
    @Override
    public Identifier getModelResource(SerpentStaff animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "geo/serpent_staff.geo.json");
    }

    @Override
    public Identifier getTextureResource(SerpentStaff animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/serpent_staff.png");
    }

    @Override
    public Identifier getAnimationResource(SerpentStaff animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "animations/serpent_staff.animation.json");
    }
}
