package com.example.egyptianweapons.client.render.entity;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.effect.SnakeHeadProjectileEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class SnakeHeadProjectileRenderer extends GeoEntityRenderer<SnakeHeadProjectileEntity> {
    public SnakeHeadProjectileRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new SnakeHeadProjectileModel());
    }

    @Override
    protected void applyRotations(SnakeHeadProjectileEntity entity, MatrixStack matrixStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(entity, matrixStack, ageInTicks, rotationYaw, partialTicks);
        
        Vec3d motion = entity.getVelocity();
        float pitch = (float) Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z));
        float yaw = (float) Math.atan2(motion.x, motion.z);
        
        // Convert radians to degrees
        pitch = pitch * (180F / (float)Math.PI);
        yaw = yaw * (180F / (float)Math.PI);

        // Apply the rotations
        matrixStack.multiply(new Quaternionf().rotationXYZ(
            pitch * ((float)Math.PI / 180F),
            yaw * ((float)Math.PI / 180F),
            0f
        ));
    }
}

class SnakeHeadProjectileModel extends GeoModel<SnakeHeadProjectileEntity> {
    @Override
    public Identifier getModelResource(SnakeHeadProjectileEntity animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "geo/snake_head.geo.json");
    }

    @Override
    public Identifier getTextureResource(SnakeHeadProjectileEntity animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/entity/snake_head.png");
    }

    @Override
    public Identifier getAnimationResource(SnakeHeadProjectileEntity animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "animations/snake_head.animation.json");
    }
}
