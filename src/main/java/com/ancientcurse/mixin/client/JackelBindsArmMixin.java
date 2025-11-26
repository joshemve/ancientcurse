package com.ancientcurse.mixin.client;

import com.ancientcurse.item.armor.JackelBindsItem;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to force arm positions when wearing Jackel Binds in third person view.
 * This modifies the player's BipedEntityModel arm rotations after vanilla animations.
 */
@Mixin(BipedEntityModel.class)
public class JackelBindsArmMixin<T extends LivingEntity> {

    // Arm rotation values from animation file (converted to radians)
    // bipedRightArm: [-44.45536, -10.37735, -6.04138]
    // bipedLeftArm: [-42.93565, 15.01996, 15.56447]
    private static final float RIGHT_ARM_PITCH = (float) Math.toRadians(-44.45536);
    private static final float RIGHT_ARM_YAW = (float) Math.toRadians(-10.37735);
    private static final float RIGHT_ARM_ROLL = (float) Math.toRadians(-6.04138);

    private static final float LEFT_ARM_PITCH = (float) Math.toRadians(-42.93565);
    private static final float LEFT_ARM_YAW = (float) Math.toRadians(15.01996);
    private static final float LEFT_ARM_ROLL = (float) Math.toRadians(15.56447);

    /**
     * Inject after setAngles to override arm positions for bound players.
     * This runs after vanilla/modded animations have set the arm angles.
     */
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void ancientcurse$forceBindsArmPosition(T entity, float limbAngle, float limbDistance,
                                                     float animationProgress, float headYaw, float headPitch,
                                                     CallbackInfo ci) {
        // Check if the entity is wearing Jackel Binds
        ItemStack chestpiece = entity.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chestpiece.getItem() instanceof JackelBindsItem)) {
            return; // Not wearing binds, don't modify
        }

        // Get the model parts
        BipedEntityModel<?> model = (BipedEntityModel<?>) (Object) this;

        // Force right arm into bound position
        // Note: In BipedEntityModel, pitch is X rotation, yaw is Y, roll is Z
        model.rightArm.pitch = RIGHT_ARM_PITCH;
        model.rightArm.yaw = RIGHT_ARM_YAW;
        model.rightArm.roll = RIGHT_ARM_ROLL;

        // Force left arm into bound position
        model.leftArm.pitch = LEFT_ARM_PITCH;
        model.leftArm.yaw = LEFT_ARM_YAW;
        model.leftArm.roll = LEFT_ARM_ROLL;
    }
}
