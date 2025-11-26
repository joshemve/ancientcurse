package com.ancientcurse.item.renderer;

import com.ancientcurse.item.armor.ScarabMaskItem;
import com.ancientcurse.item.model.ScarabMaskItemModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renderer for Scarab Mask item to display 3D model in inventory/hand
 */
public class ScarabMaskItemRenderer extends GeoItemRenderer<ScarabMaskItem> {
    public ScarabMaskItemRenderer() {
        super(new ScarabMaskItemModel());
    }
}
