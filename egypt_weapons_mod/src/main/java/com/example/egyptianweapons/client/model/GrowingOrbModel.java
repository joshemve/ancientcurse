package com.example.egyptianweapons.client.model;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.items.GrowingOrb;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model class for the Growing Orb.
 * This class is responsible for providing the model, texture, and animation resources
 * for the Growing Orb item.
 */
public class GrowingOrbModel extends GeoModel<GrowingOrb> {
    @Override
    public Identifier getModelResource(GrowingOrb animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "geo/soul_orb.geo.json");
    }

    @Override
    public Identifier getTextureResource(GrowingOrb animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/soul_orb.png");
    }

    @Override
    public Identifier getAnimationResource(GrowingOrb animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "animations/soul_orb.animation.json");
    }
}
