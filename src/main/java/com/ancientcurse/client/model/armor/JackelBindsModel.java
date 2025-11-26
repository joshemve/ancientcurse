package com.ancientcurse.client.model.armor;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.armor.JackelBindsItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class JackelBindsModel extends GeoModel<JackelBindsItem> {
    @Override
    public Identifier getModelResource(JackelBindsItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/jackel_binds.geo.json");
    }

    @Override
    public Identifier getTextureResource(JackelBindsItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/armor/jackel_binds.png");
    }

    @Override
    public Identifier getAnimationResource(JackelBindsItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/jackel_binds.animation.json");
    }
}
