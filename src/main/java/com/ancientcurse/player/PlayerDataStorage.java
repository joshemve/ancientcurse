package com.ancientcurse.player;

import net.minecraft.nbt.NbtCompound;

/**
 * Interface for accessing persistent player data.
 * Implemented by PlayerEntity through mixin.
 */
public interface PlayerDataStorage {
    /**
     * Get the persistent NBT data for this player.
     * This data is saved and loaded with the player.
     * 
     * @return The persistent NBT compound
     */
    NbtCompound ancientcurse$getPersistentData();
}