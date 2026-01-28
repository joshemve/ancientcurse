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

    // Zulmak spinning particle - orbits around Zulmak's spinning blocks (purple)
    public static final DefaultParticleType ZULMAK_PARTICLE = FabricParticleTypes.simple();

    // Zulmak shield particle - cyan/teal particles when blocking (greenish-blue)
    public static final DefaultParticleType ZULMAK_SHIELD_PARTICLE = FabricParticleTypes.simple();

    // Ra sun orb fire particle - base glow (golden/orange)
    public static final DefaultParticleType ORB_FIRE = FabricParticleTypes.simple();

    // Ra sun orb flare particle - high-speed escaping flares (golden/white)
    public static final DefaultParticleType ORB_FLARE = FabricParticleTypes.simple();

    // Sand dust particle for sandstorm effect
    public static final DefaultParticleType SAND_DUST = FabricParticleTypes.simple();

    /**
     * Registers all mod particles
     */
    public static void registerParticles() {
        AncientCurse.LOGGER.info("Registering particles for " + AncientCurse.MOD_ID);

        Registry.register(Registries.PARTICLE_TYPE, new Identifier(AncientCurse.MOD_ID, "zulmak_particle"),
                ZULMAK_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(AncientCurse.MOD_ID, "zulmak_shield_particle"),
                ZULMAK_SHIELD_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(AncientCurse.MOD_ID, "orb_fire"), ORB_FIRE);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(AncientCurse.MOD_ID, "orb_flare"), ORB_FLARE);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(AncientCurse.MOD_ID, "sand_dust"), SAND_DUST);

        AncientCurse.LOGGER.info("Particles registered");
    }
}
