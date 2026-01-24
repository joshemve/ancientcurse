package com.ancientcurse.client.model.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.block.SekhemCactusBlock;
import com.ancientcurse.block.entity.SekhemCactusBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class SekhemCactusModel extends GeoModel<SekhemCactusBlockEntity> {
    // Cache all Identifiers as static finals to avoid allocation every frame
    private static final Identifier MODEL_1 = new Identifier(AncientCurse.MOD_ID, "geo/sekhem_cactus.geo.json");
    private static final Identifier MODEL_2 = new Identifier(AncientCurse.MOD_ID, "geo/sekhem_cactus_2.geo.json");
    private static final Identifier TEXTURE_DEFAULT = new Identifier(AncientCurse.MOD_ID, "textures/block/sekhem_cactus_block.png");
    private static final Identifier TEXTURE_DRY = new Identifier(AncientCurse.MOD_ID, "textures/block/sekhem_cactus_dry.png");
    private static final Identifier TEXTURE_HEALTHY = new Identifier(AncientCurse.MOD_ID, "textures/block/sekhem_cactus_healthy.png");
    private static final Identifier ANIMATION = new Identifier(AncientCurse.MOD_ID, "animations/sekhem_cactus.animation.json");

    @Override
    public Identifier getModelResource(SekhemCactusBlockEntity animatable) {
        if (animatable.getCachedState()
                .get(SekhemCactusBlock.MODEL_VARIANT) == SekhemCactusBlock.SekhemCactusModelVariant.MODEL_2) {
            return MODEL_2;
        }
        return MODEL_1;
    }

    @Override
    public Identifier getTextureResource(SekhemCactusBlockEntity animatable) {
        BlockState state = animatable.getCachedState();
        // Model 2 only uses healthy texture
        if (state.get(SekhemCactusBlock.MODEL_VARIANT) == SekhemCactusBlock.SekhemCactusModelVariant.MODEL_2) {
            return TEXTURE_HEALTHY;
        }

        // Model 1 uses three variants
        SekhemCactusBlock.SekhemCactusVariant variant = state.get(SekhemCactusBlock.VARIANT);
        if (variant == SekhemCactusBlock.SekhemCactusVariant.DEFAULT) {
            return TEXTURE_DEFAULT;
        }
        if (variant == SekhemCactusBlock.SekhemCactusVariant.DRY) {
            return TEXTURE_DRY;
        }
        return TEXTURE_HEALTHY;
    }

    @Override
    public Identifier getAnimationResource(SekhemCactusBlockEntity animatable) {
        return ANIMATION;
    }
}
