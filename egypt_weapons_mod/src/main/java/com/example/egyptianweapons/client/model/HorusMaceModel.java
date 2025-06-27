package com.example.egyptianweapons.client.model;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.items.SmitingMaceOfHorus;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model class for the Smiting Mace of Horus.
 * This class is responsible for providing the model, texture, and animation resources
 * for the Horus Mace item with optimized resource loading and caching.
 */
public class HorusMaceModel extends GeoModel<SmitingMaceOfHorus> {
    @Override
    public Identifier getModelResource(SmitingMaceOfHorus animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "geo/cursed_mace.geo.json");
    }

    @Override
    public Identifier getTextureResource(SmitingMaceOfHorus animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/cursed_mace.png");
    }

    @Override
    public Identifier getAnimationResource(SmitingMaceOfHorus animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "animations/cursed_mace.animation.json");
    }
}
