package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.client.model.armor.CeremonialChestwrapModel;
import com.ancientcurse.item.CeremonialChestwrapItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class CeremonialChestwrapRenderer extends GeoArmorRenderer<CeremonialChestwrapItem> {
    public CeremonialChestwrapRenderer() {
        super(new CeremonialChestwrapModel());
    }
}