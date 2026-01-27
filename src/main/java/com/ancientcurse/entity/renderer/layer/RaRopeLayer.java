package com.ancientcurse.entity.renderer.layer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.RaEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders a 3D rope overlay when Ra is hibernating (stunned).
 * Acts like an armor layer, scaling slightly larger than the model.
 */
public class RaRopeLayer extends GeoRenderLayer<RaEntity> {
    private static final Identifier ROPE_TEXTURE = new Identifier(AncientCurse.MOD_ID,
            "textures/entity/ra_rope_overlay.png");

    public RaRopeLayer(GeoEntityRenderer<RaEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(MatrixStack poseStack, RaEntity entity, BakedGeoModel bakedModel, RenderLayer renderType,
            VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick,
            int packedLight, int packedOverlay) {

        if (!entity.isHibernating()) {
            return;
        }

        // Get the rope render layer (cutout for transparency)
        RenderLayer ropeLayer = RenderLayer.getEntityCutoutNoCull(ROPE_TEXTURE);
        VertexConsumer ropeBuffer = bufferSource.getBuffer(ropeLayer);

        poseStack.push();
        // Scale up slightly to sit on top of the model (armor effect)
        float scale = 1.02f;
        poseStack.scale(scale, scale, scale);

        // Hide staff bones during rope rendering to avoid broken UV issues
        // (Staff has 8x upscaled UVs which don't match the rope texture)
        boolean staffHidden = bakedModel.getBone("staff_of_ra").map(b -> {
            boolean wasHidden = b.isHidden();
            b.setHidden(true);
            return wasHidden;
        }).orElse(false);

        boolean staff2Hidden = bakedModel.getBone("staffofra").map(b -> {
            boolean wasHidden = b.isHidden();
            b.setHidden(true);
            return wasHidden;
        }).orElse(false);

        // Re-render the model with the rope texture
        getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, ropeLayer,
                ropeBuffer, partialTick, packedLight, OverlayTexture.DEFAULT_UV,
                1.0f, 1.0f, 1.0f, 1.0f);

        // Restore staff visibility
        bakedModel.getBone("staff_of_ra").ifPresent(b -> b.setHidden(staffHidden));
        bakedModel.getBone("staffofra").ifPresent(b -> b.setHidden(staff2Hidden));

        poseStack.pop();
    }
}
