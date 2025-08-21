package com.ancientcurse.system;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Objects;

/**
 * Dimension-aware key records to prevent cross-dimension collisions
 */
public class DimensionAwareKeys {
    
    /**
     * Dimension-aware BlockPos for tracking positions across dimensions
     */
    public record DimPos(RegistryKey<World> dimension, BlockPos pos) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DimPos other)) return false;
            return Objects.equals(dimension, other.dimension) && Objects.equals(pos, other.pos);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(dimension, pos);
        }
        
        @Override
        public String toString() {
            return dimension.getValue() + "@" + pos.toShortString();
        }
    }
    
    /**
     * Dimension-aware ChunkPos for tracking chunks across dimensions
     */
    public record DimChunk(RegistryKey<World> dimension, ChunkPos chunk) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DimChunk other)) return false;
            return Objects.equals(dimension, other.dimension) && Objects.equals(chunk, other.chunk);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(dimension, chunk);
        }
        
        @Override
        public String toString() {
            return dimension.getValue() + "@[" + chunk.x + "," + chunk.z + "]";
        }
    }
}