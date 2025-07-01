package com.ancientcurse.client.render.item;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.client.model.SerpentStaffModel;
import com.ancientcurse.item.SerpentStaffItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renderer for the Serpent Staff.
 * This handles the rendering of the item in various contexts (inventory, hand, etc.)
 */
public class SerpentStaffRenderer extends GeoItemRenderer<SerpentStaffItem> {
    public SerpentStaffRenderer() {
        super(new SerpentStaffModel());
    }

    @Override
    public Identifier getTextureLocation(SerpentStaffItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/item/serpent_staff.png");
    }
} 