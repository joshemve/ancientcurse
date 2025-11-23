package com.ancientcurse.world.gen;

import com.ancientcurse.AncientCurse;
import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.ConfiguredFeature;

/**
 * Returns the configured feature for date palm tree growth when the sapling grows.
 */
public class DatePalmSaplingGenerator extends SaplingGenerator {

    @Override
    protected RegistryKey<ConfiguredFeature<?, ?>> getTreeFeature(Random random, boolean bees) {
        if (bees && random.nextInt(10) == 0) {
            return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE,
                    new Identifier(AncientCurse.MOD_ID, "date_palm_tree_bees"));
        }

        int roll = random.nextInt(100);
        if (roll < 60) {
            return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE,
                    new Identifier(AncientCurse.MOD_ID, "date_palm_tree"));
        } else if (roll < 85) {
            return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE,
                    new Identifier(AncientCurse.MOD_ID, "date_palm_tree_tall"));
        } else {
            return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE,
                    new Identifier(AncientCurse.MOD_ID, "date_palm_tree_bent"));
        }
    }
}
