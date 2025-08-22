package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.client.model.armor.BronzeChestplateModel;
import com.ancientcurse.item.armor.BronzeChestplateItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BronzeChestplateRenderer extends GeoArmorRenderer<BronzeChestplateItem> {
    public BronzeChestplateRenderer() {
        super(new BronzeChestplateModel());
    }
}