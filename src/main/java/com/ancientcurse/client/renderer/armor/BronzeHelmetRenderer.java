package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.client.model.armor.BronzeHelmetModel;
import com.ancientcurse.item.armor.BronzeHelmetItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BronzeHelmetRenderer extends GeoArmorRenderer<BronzeHelmetItem> {
    public BronzeHelmetRenderer() {
        super(new BronzeHelmetModel());
    }
}