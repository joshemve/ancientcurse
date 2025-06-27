package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.SpitBallEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for the Djeserhath's spit ball projectile
 */
public class SpitBallRenderer extends GeoEntityRenderer<SpitBallEntity> {
    public SpitBallRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new SpitBallModel());
        this.shadowRadius = 0.25f; // Small shadow
    }

    @Override
    public RenderLayer getRenderType(@Nullable SpitBallEntity animatable, @Nullable Identifier texture, @Nullable VertexConsumerProvider bufferSource, float partialTick) {
        // Use additive rendering for a glowing effect
        return RenderLayer.getEntityTranslucent(texture);
    }
    
    @Override
    public void render(SpitBallEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, 
                       @Nullable VertexConsumerProvider buffer, int packedLight) {
        // Scale down the model slightly
        matrixStack.scale(0.75f, 0.75f, 0.75f);
        
        // Make it emit light
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, 15728880); // Max light value
    }

    /**
     * Model for the spit ball projectile
     */
    private static class SpitBallModel extends GeoModel<SpitBallEntity> {
        @Override
        public Identifier getModelResource(SpitBallEntity entity) {
            return new Identifier(AncientCurse.MOD_ID, "geo/spit_ball.geo.json");
        }

        @Override
        public Identifier getTextureResource(SpitBallEntity entity) {
            return new Identifier(AncientCurse.MOD_ID, "textures/entity/spit_ball.png");
        }

        @Override
        public Identifier getAnimationResource(SpitBallEntity entity) {
            return new Identifier(AncientCurse.MOD_ID, "animations/spit_ball.animation.json");
        }
    }
}
