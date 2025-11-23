package com.ancientcurse;

import com.ancientcurse.block.registry.ConstructionBlocks;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.item.ItemConvertible;

/**
 * Handles fuel registration for all mod blocks
 *
 * Fuel burn times (in ticks, 20 ticks = 1 second):
 * - Logs: 300 ticks (15 seconds) - same as vanilla logs
 * - Planks: 300 ticks (15 seconds) - same as vanilla planks
 * - Fences: 300 ticks (15 seconds) - same as vanilla fences
 * - Fence Gates: 300 ticks (15 seconds) - same as vanilla fence gates
 * - Stairs: 300 ticks (15 seconds) - same as vanilla stairs
 * - Slabs: 150 ticks (7.5 seconds) - same as vanilla slabs
 * - Reed Thatch: 100 ticks (5 seconds) - burns faster like dried plant material
 */
public class ModFuelRegistry {

    /**
     * Register all fuel values for mod blocks
     */
    public static void registerFuels() {
        AncientCurse.LOGGER.info("Registering fuel values for wood blocks");

        FuelRegistry registry = FuelRegistry.INSTANCE;

        // Sycamore wood set
        registry.add(ModBlocks.SYCAMORE_FIG_LOG, 300);
        registry.add(ModBlocks.SYCAMORE_PLANKS, 300);
        registry.add(ModBlocks.SYCAMORE_FENCE, 300);
        registry.add(ModBlocks.SYCAMORE_FENCE_GATE, 300);
        registry.add(ConstructionBlocks.SYCAMORE_STAIRS, 300);
        registry.add(ConstructionBlocks.SYCAMORE_SLAB, 150);

        // Date Palm wood set
        registry.add(ModBlocks.DATE_PALM_LOG, 300);
        registry.add(ModBlocks.DATE_PALM_PLANKS, 300);
        registry.add(ModBlocks.DATE_PALM_FENCE, 300);
        registry.add(ModBlocks.DATE_PALM_FENCE_GATE, 300);
        registry.add(ConstructionBlocks.DATE_PALM_STAIRS, 300);
        registry.add(ConstructionBlocks.DATE_PALM_SLAB, 150);

        // Cursed wood set - burns but with same values as normal wood
        // This makes sense because cursed wood is still wood structurally
        registry.add(ModBlocks.CURSED_LOG, 300);
        registry.add(ModBlocks.CURSED_WOOD_PLANK, 300);

        // Reed-based blocks - burns faster than wood
        registry.add(ModBlocks.DRIED_REED_THATCH, 100);

        AncientCurse.LOGGER.info("Registered fuel values for all wood blocks");
    }
}
