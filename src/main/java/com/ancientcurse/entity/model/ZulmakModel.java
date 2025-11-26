package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.ZulmakEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Zulmak GeckoLib model
 *
 * Model resources:
 * - Geometry: geo/zulmak.geo.json
 * - Texture: textures/entity/zulmak.png
 * - Animations: animations/zulmak.animation.json
 */
public class ZulmakModel extends GeoModel<ZulmakEntity> {

    /* ------------------ RESOURCES ------------------ */
    private static final Identifier MODEL = new Identifier(AncientCurse.MOD_ID, "geo/zulmak.geo.json");
    private static final Identifier TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/zulmak.png");
    private static final Identifier ANIM = new Identifier(AncientCurse.MOD_ID, "animations/zulmak.animation.json");

    @Override
    public Identifier getModelResource(ZulmakEntity entity) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(ZulmakEntity entity) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(ZulmakEntity entity) {
        return ANIM;
    }
}
