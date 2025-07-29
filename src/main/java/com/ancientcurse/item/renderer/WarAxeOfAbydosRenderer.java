package com.ancientcurse.item.renderer;

import com.ancientcurse.item.WarAxeOfAbydosItem;
import com.ancientcurse.item.model.WarAxeOfAbydosModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class WarAxeOfAbydosRenderer extends GeoItemRenderer<WarAxeOfAbydosItem> {
    public WarAxeOfAbydosRenderer() {
        super(new WarAxeOfAbydosModel());
    }
}