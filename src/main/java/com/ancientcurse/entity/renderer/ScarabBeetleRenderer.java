package com.ancientcurse.entity.renderer;

import com.ancientcurse.entity.ScarabBeetleEntity;
import com.ancientcurse.entity.model.ScarabBeetleModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Compact GeckoLib renderer for the Scarab Beetle.
 * <p>Visual tweaks are done <em>before</em> calling {@link GeoEntityRenderer#preRender}
 * so downstream render passes inherit the same transform and colour.</p>
 */
public class ScarabBeetleRenderer extends GeoEntityRenderer<ScarabBeetleEntity> {

    /* ---------- TUNABLE CONSTANTS ---------- */
    private static final float SHADOW_RADIUS      = 0.4f;
    private static final float MAX_AGGRO_SCALE    = 1.20f;   // 20 % up‑scale at aggression 10
    private static final float MAX_SHAKE_AMPLITUDE= 0.05f;
    private static final int   SHAKE_INTERVAL_TICKS = 4;     // every 0.2 s

    public ScarabBeetleRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new ScarabBeetleModel());
        this.shadowRadius = SHADOW_RADIUS;
    }

    /* ------------ RENDER LAYER ------------ */
    public RenderLayer getRenderType(ScarabBeetleEntity beetle, Identifier texture) {
        // translucent to support subtle alpha in defensive posture
        return RenderLayer.getEntityTranslucent(texture);
    }

    /* ------------ PRE‑RENDER ------------- */
    @Override
    public void preRender(MatrixStack pose, ScarabBeetleEntity beetle, BakedGeoModel model,
                          VertexConsumerProvider buffers, VertexConsumer buffer,
                          boolean reRender, float tickDelta, int light, int overlay,
                          float red, float green, float blue, float alpha) {

        /* --- 1. SCALE BASED ON TAMED STATE --- */
        float scale = beetle.isTamed() ? 1.1f : 1.0f; // Slightly larger when tamed
        pose.scale(scale, scale, scale);

        if (beetle.isSitting()) {
            pose.translate(0, -0.10, 0);
            pose.scale(1f, 0.9f, 1f);              // crouch effect when sitting
        }

        /* --- 2. COLOUR MANIPULATION --- */
        float r = red, g = green, b = blue, a = alpha;

        // Remove hurt color tint - let Minecraft handle it with overlay
        // The overlay parameter already handles the red flash when hurt
        
        // Tamed beetles have a slight golden tint
        if (beetle.isTamed()) {
            r = clamp(r + 0.1f);
            g = clamp(g + 0.08f);
        }

        super.preRender(pose, beetle, model, buffers, buffer, reRender, tickDelta, light, overlay, r, g, b, a);
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
