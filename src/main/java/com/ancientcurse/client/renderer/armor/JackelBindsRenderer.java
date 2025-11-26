package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.client.model.armor.JackelBindsModel;
import com.ancientcurse.item.armor.JackelBindsItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class JackelBindsRenderer extends GeoArmorRenderer<JackelBindsItem> {
    public JackelBindsRenderer() {
        super(new JackelBindsModel());
    }
}
