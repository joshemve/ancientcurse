# Ancient Curse World Generation Documentation

## Overview

The Ancient Curse mod implements custom world generation that creates an Ancient Egyptian-themed desert world. This document explains the technical implementation.

## World Generation Architecture

### World Preset
- **File**: `data/ancientcurse/worldgen/world_preset/ancient_curse.json`
- **Purpose**: Defines a new world type selectable from the world creation screen
- **Key Features**:
  - Uses custom noise settings for terrain generation
  - Configures biome source to use only desert biomes
  - Maintains vanilla Nether and End dimensions

### Noise Settings
- **File**: `data/ancientcurse/worldgen/noise_settings/ancient_curse_overworld.json`
- **Based On**: Vanilla `minecraft:overworld` settings
- **Modifications**:
  - Sea level lowered from Y=63 to Y=45
  - Custom surface rules for ancient_desert biome
  - Smooth sand placement instead of regular sand
  - Sandstone subsurface layer (30 blocks deep)

### Custom Biomes

#### ancient_desert
- **File**: `data/ancientcurse/worldgen/biome/ancient_desert.json`
- **Features**:
  - Temperature: 2.0 (very hot)
  - No precipitation
  - Custom sky/fog colors for desert atmosphere
  - Spawns: Camels, Husks, standard hostile mobs
  - Surface configured through noise settings (not in biome file)

#### nile_river (WIP)
- **File**: `data/ancientcurse/worldgen/biome/nile_river.json`
- **Features**:
  - Lush river biome with tropical fish
  - Green grass and foliage colors
  - Spawns: Frogs, Camels, Drowned

## Surface Rules Implementation

Surface rules in the noise settings control block placement:

```json
{
  "type": "minecraft:condition",
  "if_true": {
    "type": "minecraft:biome",
    "biome_is": ["ancientcurse:ancient_desert"]
  },
  "then_run": {
    "type": "minecraft:sequence",
    "sequence": [
      {
        "type": "minecraft:condition",
        "if_true": {
          "type": "minecraft:stone_depth",
          "offset": 0,
          "surface_type": "ceiling"
        },
        "then_run": {
          "type": "minecraft:block",
          "result_state": {"Name": "minecraft:sandstone"}
        }
      },
      {
        "type": "minecraft:block",
        "result_state": {"Name": "ancientcurse:smooth_sand"}
      }
    ]
  }
}
```

## Custom Blocks

### SmoothSandBlock
- **Class**: `com.ancientcurse.block.SmoothSandBlock`
- **Extends**: `SandBlock`
- **Features**:
  - No gravity (unlike regular sand)
  - Fixed texture orientation (no random rotation)
  - Same material properties as sand

## Biome Modifications

The `BiomeModifier` class removes certain features:
- Surface lava lakes removed from desert biomes
- Reduces water feature generation

## Known Issues and Future Improvements

1. **Performance**: Complex surface rules can cause lag - current implementation uses vanilla base with minimal modifications
2. **Biome Distribution**: Currently only generates ancient_desert biome
3. **Features**: Custom structures (pyramids, oases) not yet implemented
4. **Nile River**: Biome exists but not yet integrated into generation

## Adding New Biomes

1. Create biome JSON in `data/ancientcurse/worldgen/biome/`
2. Add surface rules to noise settings
3. Add biome to world preset's biome source
4. Register any custom features/structures

## Debugging Tips

- Check logs for "Unbound values in registry" errors
- Verify all referenced blocks/features are registered
- Use vanilla files as reference (copy from minecraft.jar)
- Test with minimal changes first