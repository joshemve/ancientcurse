package com.ancientcurse.world.structure;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.nbt.NbtCompound;

/**
 * Custom structure that generates a continuous Nile River running north-south through the desert
 */
public class NileRiverStructure {
    
    public static class Piece extends StructurePiece {
        private static final int RIVER_WIDTH = 8;
        private static final int RIVER_LENGTH = 64;
        private static final int FERTILE_BANK_WIDTH = 4;
        
        public Piece(BlockPos pos) {
            super(ModStructurePieceTypes.NILE_RIVER, 0, new BlockBox(
                pos.getX() - RIVER_WIDTH/2 - FERTILE_BANK_WIDTH, 
                pos.getY() - 3, 
                pos.getZ() - RIVER_LENGTH/2,
                pos.getX() + RIVER_WIDTH/2 + FERTILE_BANK_WIDTH, 
                pos.getY() + 3, 
                pos.getZ() + RIVER_LENGTH/2
            ));
        }
        
        public Piece(NbtCompound nbt) {
            super(ModStructurePieceTypes.NILE_RIVER, nbt);
        }
        
        @Override
        protected void writeNbt(StructureContext context, NbtCompound nbt) {
            // No additional NBT data needed for this structure
        }
        
        @Override
        public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, 
                           ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, 
                           ChunkPos chunkPos, BlockPos pivot) {
            
            // Generate the river running north-south
            generateRiverSegment(world, random, chunkBox, pivot);
        }
        
        private void generateRiverSegment(StructureWorldAccess world, Random random, 
                                        BlockBox chunkBox, BlockPos pivot) {
            
            int startX = pivot.getX() - RIVER_WIDTH/2 - FERTILE_BANK_WIDTH;
            int endX = pivot.getX() + RIVER_WIDTH/2 + FERTILE_BANK_WIDTH;
            int startZ = pivot.getZ() - RIVER_LENGTH/2;
            int endZ = pivot.getZ() + RIVER_LENGTH/2;
            
            for (int x = startX; x <= endX; x++) {
                for (int z = startZ; z <= endZ; z++) {
                    if (!chunkBox.contains(x, pivot.getY(), z)) continue;
                    
                    int distanceFromCenter = Math.abs(x - pivot.getX());
                    int groundY = world.getTopY() - 1;
                    
                    // Find the actual ground level
                    while (groundY > pivot.getY() - 10 && world.getBlockState(new BlockPos(x, groundY, z)).isAir()) {
                        groundY--;
                    }
                    
                    if (distanceFromCenter <= RIVER_WIDTH/2) {
                        // River channel - place water and river bed
                        generateRiverChannel(world, x, groundY, z, random);
                    } else if (distanceFromCenter <= RIVER_WIDTH/2 + FERTILE_BANK_WIDTH) {
                        // Fertile banks - place Nile silt and vegetation
                        generateFertileBanks(world, x, groundY, z, random);
                    }
                }
            }
        }
        
        private void generateRiverChannel(StructureWorldAccess world, int x, int groundY, int z, Random random) {
            BlockPos pos = new BlockPos(x, groundY, z);
            
            // Dig out the river channel (2-3 blocks deep)
            int depth = 2 + random.nextInt(2);
            for (int y = groundY; y > groundY - depth; y--) {
                world.setBlockState(new BlockPos(x, y, z), Blocks.WATER.getDefaultState(), 3);
            }
            
            // Place river bed material
            world.setBlockState(new BlockPos(x, groundY - depth, z), 
                ModBlocks.NILE_RIVER_SAND.getDefaultState(), 3);
            world.setBlockState(new BlockPos(x, groundY - depth - 1, z), 
                ModBlocks.FERTILE_NILE_SILT.getDefaultState(), 3);
        }
        
        private void generateFertileBanks(StructureWorldAccess world, int x, int groundY, int z, Random random) {
            BlockPos pos = new BlockPos(x, groundY, z);
            
            // Replace top layer with fertile silt
            world.setBlockState(pos, ModBlocks.FERTILE_NILE_SILT.getDefaultState(), 3);
            
            // Add some vegetation
            if (random.nextFloat() < 0.3f) {
                BlockPos abovePos = pos.up();
                if (world.getBlockState(abovePos).isAir()) {
                    // Add papyrus or other Nile vegetation
                    if (random.nextFloat() < 0.6f) {
                        world.setBlockState(abovePos, Blocks.TALL_GRASS.getDefaultState(), 3);
                    } else {
                        world.setBlockState(abovePos, Blocks.FERN.getDefaultState(), 3);
                    }
                }
            }
            
            // Occasionally place date palm trees
            if (random.nextFloat() < 0.05f) {
                generatePalmTree(world, pos.up(), random);
            }
        }
        
        private void generatePalmTree(StructureWorldAccess world, BlockPos pos, Random random) {
            // Simple palm tree generation
            int height = 4 + random.nextInt(3);
            
            // Trunk
            for (int y = 0; y < height; y++) {
                world.setBlockState(pos.up(y), ModBlocks.DATE_PALM_LOG.getDefaultState(), 3);
            }
            
            // Leaves/fronds at the top
            BlockPos topPos = pos.up(height);
            world.setBlockState(topPos, ModBlocks.DATE_PALM_LEAVES.getDefaultState(), 3);
            
            // Add fronds around the top
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    if (random.nextFloat() < 0.7f) {
                        world.setBlockState(topPos.add(dx, 0, dz), 
                            ModBlocks.DATE_PALM_LEAVES.getDefaultState(), 3);
                    }
                }
            }
        }
    }
}
