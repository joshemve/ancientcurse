package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.RaEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * High-performance render layer for Ra's orbiting Sun Shards.
 * Logic is handled client-side for ultra-smooth motion.
 */
public class SunShardLayer extends GeoRenderLayer<RaEntity> {
    private static final Identifier CRYSTAL_TEXTURE = new Identifier(AncientCurse.MOD_ID,
            "textures/block/sun_crystal.png");
    private static final Identifier CRYSTAL_MODEL = new Identifier(AncientCurse.MOD_ID,
            "models/block/sun_crystal.json");

    // We'll manualy draw the crystal quads to ensure full control over "glow" and
    // additive blending
    // Using a simple box-rendering approach that matches the provided JSON
    // structure

    public SunShardLayer(GeoEntityRenderer<RaEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(MatrixStack poseStack, RaEntity entity, BakedGeoModel bakedModel,
            RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
            float partialTick, int packedLight, int packedOverlay) {

        int shardTicks = entity.getShardAttackTicks();
        if (shardTicks <= 0)
            return;

        // Use beacon beam layer for that brilliant divine glow
        VertexConsumer vertices = bufferSource.getBuffer(RenderLayer.getBeaconBeam(CRYSTAL_TEXTURE, true));

        float time = entity.age + partialTick;
        int shardsToRender = calculateVisibleShards(shardTicks);

        for (int i = 0; i < shardsToRender; i++) {
            poseStack.push();

            // Orbit math: 5 points evenly spread
            float orbitAngle = (time * 0.15f) + (i * (float) (Math.PI * 2 / 5));
            float orbitRadius = 3.2f + MathHelper.sin(time * 0.1f + i) * 0.3f;

            // Floating height oscillation
            float verticalPos = (float) entity.getHeight() * 0.8f + MathHelper.cos(time * 0.12f + i) * 0.4f;

            double offsetX = MathHelper.cos(orbitAngle) * orbitRadius;
            double offsetZ = MathHelper.sin(orbitAngle) * orbitRadius;

            poseStack.translate(offsetX, verticalPos, offsetZ);

            // Self-rotation
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(time * 0.08f + i));
            poseStack.multiply(RotationAxis.POSITIVE_Z.rotation(time * 0.04f));

            // Constant scale (+45% bigger, no growth animation)
            float scale = 1.45f;
            poseStack.scale(scale, scale, scale);

            renderCrystal(poseStack, vertices, 1.0f, 0.9f, 0.4f, 1.0f);

            poseStack.pop();
        }
    }

    private int calculateVisibleShards(int ticksRemaining) {
        if (ticksRemaining > 30)
            return 5;
        if (ticksRemaining > 24)
            return 4;
        if (ticksRemaining > 18)
            return 3;
        if (ticksRemaining > 12)
            return 2;
        if (ticksRemaining > 6)
            return 1;
        return 0;
    }

    /**
     * Renders a divine version of the sun_crystal matching the JSON structure.
     */
    private void renderCrystal(MatrixStack poseStack, VertexConsumer vertices, float r, float g, float b, float a) {
        Matrix4f posMatrix = poseStack.peek().getPositionMatrix();
        Matrix3f normMatrix = poseStack.peek().getNormalMatrix();

        float texS = 16.0f; // Texture size for UV normalization

        // Main Body [3x8x3] from [6.5, 4, 6.5] to [9.5, 12, 9.5]
        // Center is [8, 8, 8], so relative to local origin it's centered.
        // Size: 3/16, 8/16, 3/16 -> 0.1875, 0.5, 0.1875
        drawBox(posMatrix, normMatrix, vertices, 0.09375f, 0.25f, 0.09375f, 0, 0, r, g, b, a,
                0, 0, 3, 8, texS);

        // Top Tip [1x2x1] from [7.5, 12, 7.5] to [8.5, 14, 8.5]
        // Center is [8, 13, 8], so offset by +5 units (5/16 = 0.3125)
        poseStack.push();
        poseStack.translate(0, 0.3125f, 0);
        drawBox(posMatrix, normMatrix, vertices, 0.03125f, 0.0625f, 0.03125f, 0, 0, r, g, b, a,
                9, 0, 10, 2, texS);
        poseStack.pop();

        // Bottom Tip [1x2x1] from [7.5, 2, 7.5] to [8.5, 4, 8.5]
        // Center is [8, 3, 8], so offset by -5 units (-5/16 = -0.3125)
        poseStack.push();
        poseStack.translate(0, -0.3125f, 0);
        drawBox(posMatrix, normMatrix, vertices, 0.03125f, 0.0625f, 0.03125f, 0, 0, r, g, b, a,
                9, 8, 10, 10, texS);
        poseStack.pop();
    }

    private void drawBox(Matrix4f posMatrix, Matrix3f normMatrix, VertexConsumer vertices,
            float hw, float hh, float hd, float ox, float oy, float r, float g, float b, float a,
            float u1, float v1, float u2, float v2, float s) {

        float uMin = u1 / s;
        float vMin = v1 / s;
        float uMax = u2 / s;
        float vMax = v2 / s;

        // FRONT
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, -hh + oy, hd, r, g, b, a, uMin, vMax);
        addVertex(posMatrix, normMatrix, vertices, hw + ox, -hh + oy, hd, r, g, b, a, uMax, vMax);
        addVertex(posMatrix, normMatrix, vertices, hw + ox, hh + oy, hd, r, g, b, a, uMax, vMin);
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, hh + oy, hd, r, g, b, a, uMin, vMin);

        // BACK
        addVertex(posMatrix, normMatrix, vertices, hw + ox, -hh + oy, -hd, r, g, b, a, uMin, vMax);
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, -hh + oy, -hd, r, g, b, a, uMax, vMax);
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, hh + oy, -hd, r, g, b, a, uMax, vMin);
        addVertex(posMatrix, normMatrix, vertices, hw + ox, hh + oy, -hd, r, g, b, a, uMin, vMin);

        // RIGHT
        addVertex(posMatrix, normMatrix, vertices, hw + ox, -hh + oy, hd, r, g, b, a, uMin, vMax);
        addVertex(posMatrix, normMatrix, vertices, hw + ox, -hh + oy, -hd, r, g, b, a, uMax, vMax);
        addVertex(posMatrix, normMatrix, vertices, hw + ox, hh + oy, -hd, r, g, b, a, uMax, vMin);
        addVertex(posMatrix, normMatrix, vertices, hw + ox, hh + oy, hd, r, g, b, a, uMin, vMin);

        // LEFT
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, -hh + oy, -hd, r, g, b, a, uMin, vMax);
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, -hh + oy, hd, r, g, b, a, uMax, vMax);
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, hh + oy, hd, r, g, b, a, uMax, vMin);
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, hh + oy, -hd, r, g, b, a, uMin, vMin);

        // UP
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, hh + oy, -hd, r, g, b, a, uMin, vMin);
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, hh + oy, hd, r, g, b, a, uMin, vMax);
        addVertex(posMatrix, normMatrix, vertices, hw + ox, hh + oy, hd, r, g, b, a, uMax, vMax);
        addVertex(posMatrix, normMatrix, vertices, hw + ox, hh + oy, -hd, r, g, b, a, uMax, vMin);

        // DOWN
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, -hh + oy, hd, r, g, b, a, uMin, vMax);
        addVertex(posMatrix, normMatrix, vertices, -hw + ox, -hh + oy, -hd, r, g, b, a, uMin, vMin);
        addVertex(posMatrix, normMatrix, vertices, hw + ox, -hh + oy, -hd, r, g, b, a, uMax, vMin);
        addVertex(posMatrix, normMatrix, vertices, hw + ox, -hh + oy, hd, r, g, b, a, uMax, vMax);
    }

    private void addVertex(Matrix4f posMatrix, Matrix3f normMatrix, VertexConsumer vertices,
            float x, float y, float z, float r, float g, float b, float a, float u, float v) {
        vertices.vertex(posMatrix, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880) // Full brightness for glow
                .normal(normMatrix, 0, 1, 0)
                .next();
    }
}
