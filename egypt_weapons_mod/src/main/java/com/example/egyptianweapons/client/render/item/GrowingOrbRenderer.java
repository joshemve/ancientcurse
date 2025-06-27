package com.example.egyptianweapons.client.render.item;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.model.GrowingOrbModel;
import com.example.egyptianweapons.items.GrowingOrb;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renderer for Growing Orb item.
 */
public class GrowingOrbRenderer extends GeoItemRenderer<GrowingOrb> {
    public GrowingOrbRenderer() {
        super(new GrowingOrbModel());
    }

    @Override
    public Identifier getTextureLocation(GrowingOrb animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/soul_orb.png");
    }
}
