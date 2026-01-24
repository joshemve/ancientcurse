package com.ancientcurse.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;

/**
 * Custom SpawnEggItem for boss entities that displays an enchanted glint.
 */
public class BossSpawnEggItem extends SpawnEggItem {
    public BossSpawnEggItem(EntityType<? extends MobEntity> type, int primaryColor, int secondaryColor,
            Settings settings) {
        super(type, primaryColor, secondaryColor, settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
