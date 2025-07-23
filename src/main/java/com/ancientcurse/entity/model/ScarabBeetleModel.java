package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.ScarabBeetleEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * Scarab Beetle GeckoLib model with proper 6-legged insect locomotion.
 * Uses 3-segment leg structure (top->middle->bottom) for realistic movement.
 */
public class ScarabBeetleModel extends GeoModel<ScarabBeetleEntity> {

    /* ------------------ RESOURCES ------------------ */
    private static final Identifier MODEL = new Identifier(AncientCurse.MOD_ID, "geo/scarab_beetle.geo.json");
    private static final Identifier TEXTURE_NORMAL = new Identifier(AncientCurse.MOD_ID, "textures/entity/scarab_beetle.png");
    private static final Identifier TEXTURE_GOLD = new Identifier(AncientCurse.MOD_ID, "textures/entity/scarab_beetle_golden.png");
    private static final Identifier ANIM = new Identifier(AncientCurse.MOD_ID, "animations/scarab_beetle.animation.json");

    // All animations handled by GeckoLib from JSON files

    @Override public Identifier getModelResource(ScarabBeetleEntity e) { return MODEL; }
    @Override public Identifier getTextureResource(ScarabBeetleEntity e) { return e.isTamed() ? TEXTURE_GOLD : TEXTURE_NORMAL; }
    @Override public Identifier getAnimationResource(ScarabBeetleEntity e) { return ANIM; }

    @Override
    public void setCustomAnimations(ScarabBeetleEntity beetle, long id, AnimationState<ScarabBeetleEntity> state) {
        super.setCustomAnimations(beetle, id, state);
        // Let GeckoLib handle all animations from JSON - no procedural code
    }

    // All procedural animations removed - GeckoLib handles everything from JSON
}
