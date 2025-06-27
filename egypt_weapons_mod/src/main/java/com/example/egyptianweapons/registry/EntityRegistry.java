package com.example.egyptianweapons.registry;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.effect.SnakeHeadProjectileEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class EntityRegistry {
    public static final EntityType<SnakeHeadProjectileEntity> SNAKE_HEAD_PROJECTILE = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(EgyptianWeapons.MOD_ID, "snake_head_projectile"),
        FabricEntityTypeBuilder.<SnakeHeadProjectileEntity>create(SpawnGroup.MISC, SnakeHeadProjectileEntity::new)
            .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(1)
            .build()
    );

    public static void registerEntities() {
        // Registration happens in the static initializer
        EgyptianWeapons.LOGGER.info("Entity registration complete");
    }
}
