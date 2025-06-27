package com.ancientcurse.item.renderer;

import com.ancientcurse.item.HorusMaceItem;
import com.ancientcurse.item.model.HorusMaceModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renderer for the Horus Mace item using GeckoLib
 */
public class HorusMaceRenderer extends GeoItemRenderer<HorusMaceItem> {
    public HorusMaceRenderer() {
        super(new HorusMaceModel());
    }
}