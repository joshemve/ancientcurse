package com.ancientcurse.mixin;

import com.ancientcurse.player.PlayerDataStorage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements PlayerDataStorage {
    @Unique
    private final NbtCompound ancientcurse$persistentData = new NbtCompound();

    @Override
    public NbtCompound ancientcurse$getPersistentData() {
        return ancientcurse$persistentData;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void ancientcurse$writeCustomData(NbtCompound nbt, CallbackInfo ci) {
        nbt.put("AncientCurseData", ancientcurse$persistentData);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void ancientcurse$readCustomData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("AncientCurseData", 10)) { // 10 = Compound tag
            ancientcurse$persistentData.copyFrom(nbt.getCompound("AncientCurseData"));
        }
    }
}