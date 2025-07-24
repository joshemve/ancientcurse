package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.client.model.armor.VeilOfAnubisModel;
import com.ancientcurse.item.armor.VeilOfAnubisItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class VeilOfAnubisRenderer extends GeoArmorRenderer<VeilOfAnubisItem> {
    public VeilOfAnubisRenderer() {
        super(new VeilOfAnubisModel());
    }
}