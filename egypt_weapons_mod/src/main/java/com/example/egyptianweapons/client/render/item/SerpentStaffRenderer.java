package com.example.egyptianweapons.client.render.item;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.model.SerpentStaffModel;
import com.example.egyptianweapons.items.SerpentStaff;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renderer for the Staff of Thoth (Serpent Staff).
 * This handles the rendering of the item in various contexts (inventory, hand, etc.)
 */
public class SerpentStaffRenderer extends GeoItemRenderer<SerpentStaff> {
    public SerpentStaffRenderer() {
        super(new SerpentStaffModel());
    }

    @Override
    public Identifier getTextureLocation(SerpentStaff animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/serpent_staff.png");
    }
}
