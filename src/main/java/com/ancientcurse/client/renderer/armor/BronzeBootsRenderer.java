package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.client.model.armor.BronzeBootsModel;
import com.ancientcurse.item.armor.BronzeBootsItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BronzeBootsRenderer extends GeoArmorRenderer<BronzeBootsItem> {
    public BronzeBootsRenderer() {
        super(new BronzeBootsModel());
    }
}