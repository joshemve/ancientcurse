package com.ancientcurse.registry;

import com.ancientcurse.ModItems;
import com.ancientcurse.block.registry.FluidBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;

import java.util.Map;

/**
 * Custom cauldron behaviors for Soul Lava.
 */
public class ModCauldronBehavior {
    /**
     * Map for Soul Lava cauldron interactions.
     * Similar to LAVA_CAULDRON_BEHAVIOR.
     */
    public static final Map<Item, CauldronBehavior> SOUL_LAVA_CAULDRON_BEHAVIOR = CauldronBehavior.createMap();

    /**
     * Registers cauldron behaviors for Soul Lava.
     */
    public static void registerBehaviors() {
        // Behavior for filling an empty cauldron with Soul Lava
        CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.put(ModItems.SOUL_LAVA_BUCKET,
                (state, world, pos, player, hand, stack) -> {
                    return CauldronBehavior.fillCauldron(world, pos, player, hand, stack,
                            FluidBlocks.SOUL_LAVA_CAULDRON.getDefaultState(), SoundEvents.ITEM_BUCKET_EMPTY_LAVA);
                });

        // Behavior for emptying a Soul Lava cauldron with an empty bucket
        SOUL_LAVA_CAULDRON_BEHAVIOR.put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            return CauldronBehavior.emptyCauldron(state, world, pos, player, hand, stack,
                    new ItemStack(ModItems.SOUL_LAVA_BUCKET), (statex) -> true, SoundEvents.ITEM_BUCKET_FILL_LAVA);
        });
    }
}
