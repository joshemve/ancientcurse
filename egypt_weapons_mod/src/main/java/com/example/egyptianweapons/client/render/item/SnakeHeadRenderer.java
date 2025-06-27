package com.example.egyptianweapons.client.render.item;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.model.SnakeHeadModel;
import com.example.egyptianweapons.items.SnakeHead;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renderer for Snake Head item.
 */
public class SnakeHeadRenderer extends GeoItemRenderer<SnakeHead> {
    public SnakeHeadRenderer() {
        super(new SnakeHeadModel());
    }

    @Override
    public Identifier getTextureLocation(SnakeHead animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/viper_head.png");
    }
}
