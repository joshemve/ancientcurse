package com.ancientcurse.client.renderer.block;

import com.ancientcurse.block.SekhemCactusBlock;
import com.ancientcurse.block.entity.SekhemCactusBlockEntity;
import com.ancientcurse.client.model.block.SekhemCactusModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SekhemCactusRenderer extends GeoBlockRenderer<SekhemCactusBlockEntity> {
    public SekhemCactusRenderer(BlockEntityRendererFactory.Context context) {
        super(new SekhemCactusModel());
    }

    @Override
    public void actuallyRender(MatrixStack poseStack, SekhemCactusBlockEntity animatable, BakedGeoModel model,
            RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        BlockState state = animatable.getCachedState();
        if (state.getBlock() instanceof SekhemCactusBlock) {
            SekhemCactusBlock.SekhemCactusPosition position = state.get(SekhemCactusBlock.POSITION);

            // Setup pose for this segment - only show the relevant bone
            poseStack.push();

            // Hide all segment bones first, then show only the current one
            model.getBone("bottom").ifPresent(bone -> bone.setHidden(true));
            model.getBone("middle").ifPresent(bone -> bone.setHidden(true));
            model.getBone("middle2").ifPresent(bone -> bone.setHidden(true));
            model.getBone("top").ifPresent(bone -> bone.setHidden(true));

            switch (position) {
                case BOTTOM:
                    model.getBone("bottom").ifPresent(bone -> bone.setHidden(false));
                    break;
                case MIDDLE:
                    model.getBone("middle").ifPresent(bone -> bone.setHidden(false));
                    poseStack.translate(0, -1, 0);
                    break;
                case MIDDLE2:
                    model.getBone("middle2").ifPresent(bone -> bone.setHidden(false));
                    poseStack.translate(0, -2, 0);
                    break;
                case TOP:
                    model.getBone("top").ifPresent(bone -> bone.setHidden(false));
                    poseStack.translate(0, -3, 0);
                    break;
            }

            // 2. Render Main Model
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);

            poseStack.pop();
            return;
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
    }
}
