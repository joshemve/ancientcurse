package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.client.model.armor.BronzeLeggingsModel;
import com.ancientcurse.item.armor.BronzeLeggingsItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BronzeLeggingsRenderer extends GeoArmorRenderer<BronzeLeggingsItem> {
    public BronzeLeggingsRenderer() {
        super(new BronzeLeggingsModel());
    }
}