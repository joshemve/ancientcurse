package com.ancientcurse.client.model.armor;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.armor.BronzeChestplateItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class BronzeChestplateModel extends GeoModel<BronzeChestplateItem> {
    @Override
    public Identifier getModelResource(BronzeChestplateItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/bronze_chestplate.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(BronzeChestplateItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/armor/bronze_chestplate.png");
    }
    
    @Override
    public Identifier getAnimationResource(BronzeChestplateItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/bronze_chestplate.animation.json");
    }
}