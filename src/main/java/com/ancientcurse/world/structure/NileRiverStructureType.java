package com.ancientcurse.world.structure;

import com.ancientcurse.AncientCurse;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Optional;

/**
 * Custom structure type for generating the Nile River
 */
public class NileRiverStructureType implements StructureType<NileRiverStructureType.Config> {
    
    public static final Codec<Config> CODEC = Structure.Config.CODEC.codec().xmap(
        Config::new,
        config -> config.config
    );
    
    public static final NileRiverStructureType INSTANCE = new NileRiverStructureType();
    public static final StructureType<Config> NILE_RIVER = register("nile_river", INSTANCE);
    
    private static <T extends Structure> StructureType<T> register(String id, StructureType<T> structureType) {
        return Registry.register(Registries.STRUCTURE_TYPE, new Identifier(AncientCurse.MOD_ID, id), structureType);
    }
    
    public static void registerStructureTypes() {
        AncientCurse.LOGGER.info("Registering structure types for " + AncientCurse.MOD_ID);
        // Registration happens through static initialization
    }
    
    @Override
    public Codec<Config> codec() {
        return CODEC;
    }
    
    public static class Config extends Structure {
        private final Structure.Config config;
        
        public Config(Structure.Config config) {
            super(config);
            this.config = config;
        }
        
        @Override
        public Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
            BlockPos blockPos = context.chunkPos().getStartPos();
            
            // Get the surface height for the river placement
            int surfaceY = context.chunkGenerator().getHeightInGround(
                blockPos.getX(), blockPos.getZ(), Heightmap.Type.WORLD_SURFACE_WG, 
                context.world(), context.noiseConfig()
            );
            
            // Place the river at surface level
            BlockPos structurePos = new BlockPos(blockPos.getX(), surfaceY, blockPos.getZ());
            
            return Optional.of(new Structure.StructurePosition(structurePos, collector -> {
                collector.addPiece(new NileRiverStructure.Piece(structurePos));
            }));
        }
        
        @Override
        public StructureType<?> getType() {
            return NILE_RIVER;
        }
    }
}
