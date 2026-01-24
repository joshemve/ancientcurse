package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.block.SoulLavaBlock;
import com.ancientcurse.registry.ModFluids;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FluidBlocks {
    public static final Block SOUL_LAVA = new SoulLavaBlock(
            ModFluids.STILL_SOUL_LAVA,
            FabricBlockSettings.copyOf(Blocks.LAVA)
                    .noCollision()
                    .ticksRandomly()
                    .strength(100.0F)
                    .luminance(state -> 15)
                    .dropsNothing());

    public static final Block SOUL_LAVA_CAULDRON = new com.ancientcurse.block.SoulLavaCauldronBlock(
            FabricBlockSettings.copyOf(Blocks.LAVA_CAULDRON),
            com.ancientcurse.registry.ModCauldronBehavior.SOUL_LAVA_CAULDRON_BEHAVIOR);

    public static void registerBlocks() {
        Registry.register(
                Registries.BLOCK,
                new Identifier(AncientCurse.MOD_ID, "soul_lava"),
                SOUL_LAVA);

        Registry.register(
                Registries.BLOCK,
                new Identifier(AncientCurse.MOD_ID, "soul_lava_cauldron"),
                SOUL_LAVA_CAULDRON);
    }
}
