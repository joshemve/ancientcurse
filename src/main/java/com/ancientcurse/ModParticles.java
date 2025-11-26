package com.ancientcurse;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers all custom particles for the Ancient Curse mod
 */
public class ModParticles {

    // Zulmak spinning particle - orbits around Zulmak's spinning blocks
    public static final DefaultParticleType ZULMAK_PARTICLE = FabricParticleTypes.simple();

    /**
     * Registers all mod particles
     */
    public static void registerParticles() {
        AncientCurse.LOGGER.info("Registering particles for " + AncientCurse.MOD_ID);

        Registry.register(Registries.PARTICLE_TYPE, new Identifier(AncientCurse.MOD_ID, "zulmak_particle"), ZULMAK_PARTICLE);

        AncientCurse.LOGGER.info("Particles registered");
    }
}
