package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.SunShardProjectileEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class SunShardProjectileRenderer extends EntityRenderer<SunShardProjectileEntity> {
    private static final Identifier CRYSTAL_TEXTURE = new Identifier(AncientCurse.MOD_ID,
            "textures/block/sun_crystal.png");

    public SunShardProjectileRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(SunShardProjectileEntity entity) {
        return CRYSTAL_TEXTURE;
    }

    @Override
    public void render(SunShardProjectileEntity entity, float yaw, float tickDelta, MatrixStack poseStack,
            VertexConsumerProvider bufferSource, int light) {
        poseStack.push();

        // Face the direction of travel
        poseStack.multiply(RotationAxis.POSITIVE_Y
                .rotationDegrees(MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) - 90.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Z
                .rotationDegrees(MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch())));

        // Spinning effect
        float rotation = (entity.age + tickDelta) * 0.2f;
        poseStack.multiply(RotationAxis.POSITIVE_X.rotation(rotation));

        VertexConsumer vertices = bufferSource.getBuffer(RenderLayer.getBeaconBeam(CRYSTAL_TEXTURE, true));

        MatrixStack.Entry entry = poseStack.peek();
        Matrix4f posMatrix = entry.getPositionMatrix();
        Matrix3f normMatrix = entry.getNormalMatrix();

        float r = 1.0f;
        float g = 0.9f;
        float b = 0.4f;
        float a = 1.0f;
        float texS = 16.0f;

        // Draw the shard (Same 3-part model as SunShardLayer)
        // Main Body [3x8x3] -> center [0,0,0]
        drawBox(posMatrix, normMatrix, vertices, 0.09375f, 0.25f, 0.09375f, 0, 0, r, g, b, a, 0, 0, 3, 8, texS);

        // Top Tip [1x2x1] -> offset +0.3125
        drawBox(posMatrix, normMatrix, vertices, 0.03125f, 0.0625f, 0.03125f, 0, 0.3125f, r, g, b, a, 9, 0, 10, 2,
                texS);

        // Bottom Tip [1x2x1] -> offset -0.3125
        drawBox(posMatrix, normMatrix, vertices, 0.03125f, 0.0625f, 0.03125f, 0, -0.3125f, r, g, b, a, 9, 8, 10, 10,
                texS);

        // Core glow
        drawBox(posMatrix, normMatrix, vertices, 0.12f, 0.3f, 0.12f, 0, 0, r, g, b, a * 0.4f, 0, 0, 3, 8, texS);

        poseStack.pop();
        super.render(entity, yaw, tickDelta, poseStack, bufferSource, light);
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
                .light(15728880)
                .normal(normMatrix, 0, 1, 0)
                .next();
    }
}
