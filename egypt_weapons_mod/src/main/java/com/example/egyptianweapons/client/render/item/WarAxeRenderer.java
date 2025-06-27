package com.example.egyptianweapons.client.render.item;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.model.WarAxeModel;
import com.example.egyptianweapons.items.WarAxe;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renderer for War Axe item.
 */
public class WarAxeRenderer extends GeoItemRenderer<WarAxe> {
    public WarAxeRenderer() {
        super(new WarAxeModel());
    }

    @Override
    public Identifier getTextureLocation(WarAxe animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/war_axe.png");
    }
}
