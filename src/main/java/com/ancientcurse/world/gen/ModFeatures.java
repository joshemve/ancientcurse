package com.ancientcurse.world.gen;

import com.ancientcurse.AncientCurse;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

/**
 * Registry class for custom world generation features.
 */
public class ModFeatures {
    public static final Feature<DefaultFeatureConfig> DATE_PALM_TREE = new DatePalmTreeFeature();

    public static void registerFeatures() {
        Registry.register(Registries.FEATURE, new Identifier(AncientCurse.MOD_ID, "date_palm_tree"), DATE_PALM_TREE);
        AncientCurse.LOGGER.info("Registered custom worldgen features for " + AncientCurse.MOD_ID);
    }
}
