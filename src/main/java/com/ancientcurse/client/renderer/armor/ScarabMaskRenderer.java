package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.client.model.armor.ScarabMaskModel;
import com.ancientcurse.item.armor.ScarabMaskItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ScarabMaskRenderer extends GeoArmorRenderer<ScarabMaskItem> {
    public ScarabMaskRenderer() {
        super(new ScarabMaskModel());
    }
}