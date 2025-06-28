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

        /* --- 1. SCALE / TRANSLATE BASED ON STATE --- */
        int aggro = beetle.getAggressionLevel();
        float scale = 1f + aggro * 0.02f;          // linear 0‑>0.20
        if (scale > MAX_AGGRO_SCALE) scale = MAX_AGGRO_SCALE;
        pose.scale(scale, scale, scale);

        if (beetle.isDefensive()) {
            pose.translate(0, -0.10, 0);
            pose.scale(1f, 0.9f, 1f);              // crouch effect
        }

        /* --- 2. MICRO SHAKE WHEN VERY AGGRESSIVE --- */
        if (beetle.isHighlyAggressive() && (beetle.age & (SHAKE_INTERVAL_TICKS-1)) == 0) {
            float amp = MAX_SHAKE_AMPLITUDE;
            pose.translate(
                    (beetle.getRandom().nextFloat() - 0.5f) * amp,
                    0,
                    (beetle.getRandom().nextFloat() - 0.5f) * amp);
        }

        /* --- 3. COLOUR MANIPULATION --- */
        float r = red, g = green, b = blue, a = alpha;

        // hurt flash: lastDamageTime set in entity onDamage
        if (beetle.age - beetle.lastDamageTime < 10) {
            r = 1f; g = 0.3f; b = 0.3f;
        }

        // aggression tint
        if (aggro > 0) {
            float f = aggro / 10f;                   // 0‑>1
            r = clamp(r + f * 0.4f);
            g = clamp(g - f * 0.2f);
            b = clamp(b - f * 0.2f);
        }

        // defensive blue‑shift & slight transparency
        if (beetle.isDefensive()) {
            b = clamp(b + 0.2f);
            a *= 0.9f;
        }

        super.preRender(pose, beetle, model, buffers, buffer, reRender, tickDelta, light, overlay, r, g, b, a);
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
