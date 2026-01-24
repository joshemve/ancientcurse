package com.ancientcurse.item.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.PurificationStaffItem;
import com.ancientcurse.item.model.PurificationStaffModel;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class PurificationStaffRenderer extends GeoItemRenderer<PurificationStaffItem> {
    private static final Identifier GLOW_TEXTURE = new Identifier(AncientCurse.MOD_ID,
            "textures/item/purification_staff.png");

    public PurificationStaffRenderer() {
        super(new PurificationStaffModel());

        // Add a custom glow layer for the center piece
        addRenderLayer(new StaffGlowLayer(this));
    }

    /**
     * Custom glow layer to handle emissive rendering for the staff center
     */
    private class StaffGlowLayer extends GeoRenderLayer<PurificationStaffItem> {
        public StaffGlowLayer(GeoItemRenderer<PurificationStaffItem> renderer) {
            super(renderer);
        }

        @Override
        public void render(MatrixStack matrixStack, PurificationStaffItem animatable, BakedGeoModel bakedModel,
                RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                float partialTick, int packedLight, int packedOverlay) {

            // Render the whole texture as an emissive layer (the texture itself should have
            // black/alpha elsewhere)
            // Or we can just render everything at full light which creates a glow effect
            VertexConsumer emissiveBuffer = bufferSource
                    .getBuffer(RenderLayer.getEntityTranslucentEmissive(GLOW_TEXTURE));

            // Re-render with full brightness (15728640 is max light)
            getRenderer().reRender(bakedModel, matrixStack, bufferSource, animatable, renderType,
                    emissiveBuffer, partialTick, 15728640, OverlayTexture.DEFAULT_UV,
                    1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}
