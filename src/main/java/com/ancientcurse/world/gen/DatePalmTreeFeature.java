package com.ancientcurse.world.gen;

import com.ancientcurse.AncientCurse;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import java.util.Optional;

/**
 * A feature that places a date palm tree from NBT structures.
 */
public class DatePalmTreeFeature extends Feature<DefaultFeatureConfig> {
    public DatePalmTreeFeature() {
        super(DefaultFeatureConfig.CODEC);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos pos = context.getOrigin();
        Random random = context.getRandom();
        StructureTemplateManager structureTemplateManager = world.toServerWorld().getStructureTemplateManager();

        // Randomly choose between date_palm_tree_0 and date_palm_tree_1
        int treeId = random.nextInt(2);
        Identifier structureId = new Identifier(AncientCurse.MOD_ID, "date_palm_tree_" + treeId);
        Optional<StructureTemplate> optional = structureTemplateManager.getTemplate(structureId);

        if (optional.isEmpty()) {
            AncientCurse.LOGGER.error("Could not find structure template " + structureId);
            return false;
        }

        StructureTemplate structureTemplate = optional.get();
        BlockRotation rotation = BlockRotation.random(random);

        // Center the pivot so the tree rotates around the sapling position
        net.minecraft.util.math.Vec3i size = structureTemplate.getSize();
        BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);

        StructurePlacementData structurePlacementData = new StructurePlacementData()
                .setRotation(rotation)
                .setMirror(BlockMirror.NONE)
                .setIgnoreEntities(false)
                .addProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR); // Don't remove existing blocks with structure
                                                                         // air

        // Calculate placement so the pivot point lands on the origin (pos)
        // Lower by 1 block to bury the base
        BlockPos relativePivot = StructureTemplate.transform(structurePlacementData, pivot);
        BlockPos placementPos = pos.subtract(relativePivot).down();

        return structureTemplate.place(world, placementPos, placementPos, structurePlacementData, random, 2);
    }
}
