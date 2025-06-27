package com.example.egyptianweapons.client.render.item;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.model.StaffOfRaModel;
import com.example.egyptianweapons.items.StaffOfRa;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renderer for Staff of Ra item.
 */
public class StaffOfRaRenderer extends GeoItemRenderer<StaffOfRa> {
    public StaffOfRaRenderer() {
        super(new StaffOfRaModel());
    }

    @Override
    public Identifier getTextureLocation(StaffOfRa animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/staff_of_souls.png");
    }
}
