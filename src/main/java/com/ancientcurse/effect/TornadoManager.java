package com.ancientcurse.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class TornadoManager {
    private static final List<SandstormTornado> activeTornadoes = new ArrayList<>();

    public static void spawnTornado(World world, BlockPos pos, LivingEntity target) {
        activeTornadoes.add(new SandstormTornado(world, pos, target));
    }

    public static void tick(MinecraftServer server) {
        activeTornadoes.removeIf(tornado -> {
            tornado.tick(server);
            return tornado.age >= 70; // Remove after 3.5 seconds (70 ticks)
        });
    }

    public static void clearAll() {
        activeTornadoes.forEach(SandstormTornado::cleanup);
        activeTornadoes.clear();
    }
} 