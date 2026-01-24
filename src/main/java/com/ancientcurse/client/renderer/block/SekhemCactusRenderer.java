package com.ancientcurse.client.renderer.block;

import com.ancientcurse.block.SekhemCactusBlock;
import com.ancientcurse.block.SekhemCactusBlock.SekhemCactusPosition;
import com.ancientcurse.block.entity.SekhemCactusBlockEntity;
import com.ancientcurse.client.model.block.SekhemCactusModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SekhemCactusRenderer extends GeoBlockRenderer<SekhemCactusBlockEntity> {
    // Cache bone names as constants
    private static final String BONE_BOTTOM = "bottom";
    private static final String BONE_MIDDLE = "middle";
    private static final String BONE_MIDDLE2 = "middle2";
    private static final String BONE_TOP = "top";

    public SekhemCactusRenderer(BlockEntityRendererFactory.Context context) {
        super(new SekhemCactusModel());
    }

    @Override
    public void actuallyRender(MatrixStack poseStack, SekhemCactusBlockEntity animatable, BakedGeoModel model,
            RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        BlockState state = animatable.getCachedState();
        if (!(state.getBlock() instanceof SekhemCactusBlock)) {
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        SekhemCactusPosition position = state.get(SekhemCactusBlock.POSITION);
        poseStack.push();

        // Determine which bone to show based on position
        String visibleBone;
        double yOffset;
        switch (position) {
            case MIDDLE:
                visibleBone = BONE_MIDDLE;
                yOffset = -1;
                break;
            case MIDDLE2:
                visibleBone = BONE_MIDDLE2;
                yOffset = -2;
                break;
            case TOP:
                visibleBone = BONE_TOP;
                yOffset = -3;
                break;
            default: // BOTTOM
                visibleBone = BONE_BOTTOM;
                yOffset = 0;
                break;
        }

        // Set visibility for all bones - show only the relevant one
        setBoneHidden(model, BONE_BOTTOM, !BONE_BOTTOM.equals(visibleBone));
        setBoneHidden(model, BONE_MIDDLE, !BONE_MIDDLE.equals(visibleBone));
        setBoneHidden(model, BONE_MIDDLE2, !BONE_MIDDLE2.equals(visibleBone));
        setBoneHidden(model, BONE_TOP, !BONE_TOP.equals(visibleBone));

        if (yOffset != 0) {
            poseStack.translate(0, yOffset, 0);
        }

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.pop();
    }

    private static void setBoneHidden(BakedGeoModel model, String boneName, boolean hidden) {
        model.getBone(boneName).ifPresent(bone -> bone.setHidden(hidden));
    }
}
