package com.ancientcurse.client.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.SerpentStaffItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class SerpentStaffModel extends GeoModel<SerpentStaffItem> {
    
    @Override
    public Identifier getModelResource(SerpentStaffItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/serpent_staff.geo.json");
    }

    @Override
    public Identifier getTextureResource(SerpentStaffItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/item/serpent_staff.png");
    }

    @Override
    public Identifier getAnimationResource(SerpentStaffItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/serpent_staff.animation.json");
    }
} 