package com.ancientcurse.client.model.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.block.SekhemCactusBlock;
import com.ancientcurse.block.entity.SekhemCactusBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class SekhemCactusModel extends GeoModel<SekhemCactusBlockEntity> {
    @Override
    public Identifier getModelResource(SekhemCactusBlockEntity animatable) {
        if (animatable.getCachedState()
                .get(SekhemCactusBlock.MODEL_VARIANT) == SekhemCactusBlock.SekhemCactusModelVariant.MODEL_2) {
            return new Identifier(AncientCurse.MOD_ID, "geo/sekhem_cactus_2.geo.json");
        }
        return new Identifier(AncientCurse.MOD_ID, "geo/sekhem_cactus.geo.json");
    }

    @Override
    public Identifier getTextureResource(SekhemCactusBlockEntity animatable) {
        BlockState state = animatable.getCachedState();
        // Model 2 only uses healthy texture
        if (state.get(SekhemCactusBlock.MODEL_VARIANT) == SekhemCactusBlock.SekhemCactusModelVariant.MODEL_2) {
            return new Identifier(AncientCurse.MOD_ID, "textures/block/sekhem_cactus_healthy.png");
        }

        // Model 1 uses three variants
        if (state.get(SekhemCactusBlock.VARIANT) == SekhemCactusBlock.SekhemCactusVariant.DEFAULT) {
            return new Identifier(AncientCurse.MOD_ID, "textures/block/sekhem_cactus_block.png");
        }
        if (state.get(SekhemCactusBlock.VARIANT) == SekhemCactusBlock.SekhemCactusVariant.DRY) {
            return new Identifier(AncientCurse.MOD_ID, "textures/block/sekhem_cactus_dry.png");
        }
        return new Identifier(AncientCurse.MOD_ID, "textures/block/sekhem_cactus_healthy.png");
    }

    @Override
    public Identifier getAnimationResource(SekhemCactusBlockEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/sekhem_cactus.animation.json");
    }
}
