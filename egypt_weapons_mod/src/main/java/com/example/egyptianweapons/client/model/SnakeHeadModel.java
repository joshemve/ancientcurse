package com.example.egyptianweapons.client.model;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.items.SnakeHead;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model class for the Snake Head.
 * This class is responsible for providing the model, texture, and animation resources
 * for the Snake Head item.
 */
public class SnakeHeadModel extends GeoModel<SnakeHead> {
    @Override
    public Identifier getModelResource(SnakeHead animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "geo/viper_head.geo.json");
    }

    @Override
    public Identifier getTextureResource(SnakeHead animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/viper_head.png");
    }

    @Override
    public Identifier getAnimationResource(SnakeHead animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "animations/viper_head.animation.json");
    }
}
