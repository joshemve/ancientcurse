package com.example.egyptianweapons.client.render.item;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.model.HorusMaceModel;
import com.example.egyptianweapons.items.SmitingMaceOfHorus;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renderer for Smiting Mace of Horus item.
 */
public class HorusMaceRenderer extends GeoItemRenderer<SmitingMaceOfHorus> {
    public HorusMaceRenderer() {
        super(new HorusMaceModel());
    }

    @Override
    public Identifier getTextureLocation(SmitingMaceOfHorus animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/cursed_mace.png");
    }
}
