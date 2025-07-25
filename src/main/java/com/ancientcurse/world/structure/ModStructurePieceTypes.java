package com.ancientcurse.world.structure;

import com.ancientcurse.AncientCurse;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;

/**
 * Registry for custom structure piece types
 */
public class ModStructurePieceTypes {
    
    public static final StructurePieceType NILE_RIVER = register("nile_river", 
        (context, nbt) -> new NileRiverStructure.Piece(nbt));
    
    private static StructurePieceType register(String id, StructurePieceType structurePieceType) {
        return Registry.register(Registries.STRUCTURE_PIECE, new Identifier(AncientCurse.MOD_ID, id), structurePieceType);
    }
    
    public static void registerStructurePieceTypes() {
        AncientCurse.LOGGER.info("Registering structure piece types for " + AncientCurse.MOD_ID);
        // Registration happens through static initialization
    }
}
