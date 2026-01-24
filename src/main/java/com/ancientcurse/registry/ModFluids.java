package com.ancientcurse.registry;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.fluid.SoulLavaFluid;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModFluids {
    public static final FlowableFluid STILL_SOUL_LAVA = Registry.register(
            Registries.FLUID,
            new Identifier(AncientCurse.MOD_ID, "soul_lava"),
            new SoulLavaFluid.Still());
    public static final FlowableFluid FLOWING_SOUL_LAVA = Registry.register(
            Registries.FLUID,
            new Identifier(AncientCurse.MOD_ID, "flowing_soul_lava"),
            new SoulLavaFluid.Flowing());

    public static void registerFluids() {
        AncientCurse.LOGGER.info("Registering fluids for " + AncientCurse.MOD_ID);
    }
}
