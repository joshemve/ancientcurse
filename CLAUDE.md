# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ancient Curse is a Minecraft 1.20.1 Fabric mod that adds Ancient Egyptian-themed content including custom world generation, biomes, blocks, entities, and items. The mod uses GeckoLib for 3D animations and implements complex world generation systems.

## Development Commands

### Essential Build Commands
```bash
# Setup development environment
./gradlew genSources

# Run client for testing
./gradlew runClient

# Build the mod
./gradlew build

# Run data generation (for recipes, loot tables, etc.)
./gradlew runDatagenClient

# Quick build and run (Windows)
build.bat

# Complete setup (Windows)
super_simple_setup.bat
```

### Debugging Commands
```bash
# Clean build
./gradlew clean build

# Build with verbose output
./gradlew build --info
```

## Architecture Overview

### Registry System Architecture
The mod uses a sophisticated registry system to organize content:

- **BlockRegistry**: Central coordinator in `com.ancientcurse.block.registry.BlockRegistry`
- **Specialized Registries**: Content organized by type:
  - `CursedPlantBlocks` - Supernatural plant blocks
  - `EgyptianPlantBlocks` - Historical plant varieties  
  - `DeshretBlocks` - Red desert-themed blocks
  - `NecrostoneBlocks` - Undead/tomb blocks
  - `PillarBlocks` - Structural elements
  - `ConstructionBlocks` - Building materials

**Critical Rule**: Never add blocks directly to `ModBlocks`. Always use the appropriate specialized registry class.

### World Generation Architecture
- **Custom World Type**: "Ancient Curse" selectable in world creation
- **Noise Settings**: `ancient_desert_flat.json` creates flat Egyptian-style terrain
- **Custom Biomes**: `ancient_desert`, `deshret_desert`, `nile_river`
- **Density Functions**: Custom terrain shaping in `data/ancientcurse/worldgen/density_function/`
- **Surface Rules**: Block placement logic in noise settings

### Client-Side Architecture
**Single Client Initializer Rule**: Only `com.ancientcurse.client.AncientCurseClient` implements `ClientModInitializer`. This prevents transparency rendering conflicts and duplicate tooltips.

### Animation System (GeckoLib)
- **3D Models**: `.geo.json` files in `assets/ancientcurse/models/entity/`
- **Animations**: `.animation.json` files in `assets/ancientcurse/animations/`
- **Textures**: PNG files in `assets/ancientcurse/textures/item/` or `/entity/`
- **Staff of Ra**: Flagship animated item with rotating sun element

### Animation Timing Synchronization (CRITICAL)

**The Problem**: Animation durations in Java code must EXACTLY match the `animation_length` in JSON files. Mismatches cause damage to occur at wrong times, animations to cut off early, or visual desyncs.

**The Rule**: The animation JSON file is the **source of truth**. Java constants must be derived from it.

**Formula**: `duration_ticks = animation_length_seconds * 20`

**When modifying entity animations**:
1. Open the `.animation.json` file in Blockbench
2. Note the `animation_length` value (in seconds)
3. Multiply by 20 to get ticks
4. Update the corresponding Java constant

**Ra Entity Animation Timing** (example pattern to follow):
```
Animation JSON (source)     →  Java Constant
ra.melee: 3.0s             →  MELEE_DURATION = 60
ra.flying_ground_smack: 3.0s →  GROUND_SMACK_DURATION = 60
ra.sun_beam_slice: 2.0s    →  SUN_BEAM_SLICE_DURATION = 40
```

**Finding Damage Frames**:
1. Open animation in Blockbench
2. Scrub timeline to the frame where impact/damage should occur visually
3. Note the timestamp (e.g., 2.25 seconds)
4. Convert to ticks: `2.25 * 20 = 45`
5. Set `DAMAGE_FRAME = 45` in the AI goal

**Files that must stay in sync for Ra**:
- `ra.animation.json` - Source of truth for animation lengths
- `RaEntity.java` - Duration constants for animation state management
- `RaGroundSmackGoal.java` - Duration and damage frame for ground smack
- `RaSunBeamGoal.java` - Beam timing phases
- `SunBeamSliceLayer.java` - Client-side beam rendering timing

**Verification Checklist**:
- [ ] Animation JSON `animation_length` matches Java duration constant
- [ ] `DAMAGE_FRAME` corresponds to visual impact in animation
- [ ] All files referencing the same animation have matching timing values
- [ ] Comments in code reference the source JSON file

### Player Animation System (PlayerAnimator)
The mod uses the PlayerAnimator library for player body animations (spin attacks, emotes, etc.).

**Dependencies** (in build.gradle):
```gradle
repositories {
    maven { url 'https://maven.kosmx.dev/' }
}
dependencies {
    modImplementation "dev.kosmx.player-anim:player-animation-lib-fabric:1.0.2-rc1+1.20"
    include "dev.kosmx.player-anim:player-animation-lib-fabric:1.0.2-rc1+1.20"
}
```

**Animation Files**: Place in `assets/ancientcurse/player_animation/` folder
- PlayerAnimator auto-loads all JSON files from this folder
- Use **Bedrock animation format** with `format_version: "1.8.0"`
- Animation name inside JSON must match the lookup key (e.g., `"waraxe_spin_attack"`)

**Smooth Interpolation**: Use Catmull-Rom splines for smooth curves:
```json
{
  "format_version": "1.8.0",
  "animations": {
    "your_animation_name": {
      "loop": false,
      "animation_length": 1.0,
      "bones": {
        "body": {
          "rotation": {
            "0.0": {"post": [0, 0, 0], "lerp_mode": "catmullrom"},
            "0.5": {"post": [0, -180, 0], "lerp_mode": "catmullrom"},
            "1.0": {"post": [0, -360, 0], "lerp_mode": "catmullrom"}
          }
        }
      }
    }
  }
}
```

**Bone Names**: `body`, `head`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg`

**Key Components**:
- `IAnimatedPlayer` - Interface for players with animation layer
- `PlayerAnimationMixin` - Injects ModifierLayer into AbstractClientPlayerEntity
- `PlayerAnimationHandler` - Plays animations via `PlayerAnimationRegistry.getAnimation()`
- `CurseZonePackets` - Network sync for server→client animation broadcast

**Playing Animations from Server**:
```java
CurseZonePackets.sendPlayerAnimation(serverWorld, player, "animation_name");
```

**Common Issues**:
- Animation NOT FOUND: Check animation name in JSON matches lookup key
- Choppy animation: Use `lerp_mode: "catmullrom"` instead of linear interpolation
- Reverse spin at end: End at final rotation (e.g., -360) not 0 to avoid snap-back

### Entity System
- **Boss Entities**: `DjeserhathEntity`, `WitheredPharaohEntity`
- **Hostile Mobs**: `LocusEntity`, `BabyLocusEntity`, `ScarabBeetleEntity`
- **Neutral Entities**: `AnubisEntity`, `ThothEntity`, `SunGolemEntity`
- **Projectiles**: `SnakeHeadProjectileEntity`, `ThothMagicBallEntity`

All entities use GeckoLib for animations and have corresponding model/renderer classes.

## Critical Technical Details

### Transparency Rendering
For transparent blocks:
1. Use `.nonOpaque().notSolid()` in block settings
2. Register with `BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout())`
3. Register each block only ONCE to avoid conflicts

### World Generation Debug
- Check logs for "Unbound values in registry" errors
- Verify all referenced blocks/features are registered
- Use vanilla files as reference when creating custom worldgen
- Test noise settings with minimal changes first

### Performance Systems
- **Cursed Earth Manager**: Optimizes large-scale block changes
- **Curse Zone System**: Area-based effects with client/server sync
- **Block Reference Manager**: Tracks original block states for restoration

### Known Issues
- **Ocean Generation**: Fixed with custom density functions in `ancient_desert_flat.json`
- **Feature Missing Errors**: Biomes reference only existing placed features
- **Intrusive Holders**: Use `INTRUSIVE_HOLDERS_FIX.md` for registry fixes

## Directory Structure Significance

### World Generation Data
```
data/ancientcurse/worldgen/
├── biome/                    # Custom biome definitions
├── density_function/         # Terrain shaping functions
├── noise_settings/          # Complete terrain generation configs
├── world_preset/            # World type definitions
└── placed_feature/          # Feature placement rules
```

### Java Package Structure
```
com.ancientcurse/
├── world/                   # World generation code
│   ├── biome/              # Biome registration and modification
│   ├── gen/                # Feature and surface rule generation
│   └── dimension/          # Custom dimension handling
├── block/registry/         # Organized block registration system
├── entity/                 # All mob and projectile entities
├── client/                 # Client-only rendering and UI code
└── system/                 # Performance and management systems
```

## Dependencies and Versions
- **Minecraft**: 1.20.1
- **Fabric Loader**: 0.14.21+
- **Fabric API**: 0.87.0+1.20.1
- **GeckoLib**: 4.2.3 (entity/item 3D animations)
- **PlayerAnimator**: 1.0.2-rc1+1.20 (player body animations)

## Testing and Validation
- **World Generation**: Create new "Ancient Curse" world type
- **Custom Blocks**: Check creative menu "Ancient Curse" tab
- **Animations**: Test Staff of Ra for rotating sun animation
- **Biomes**: Use `/locate biome ancientcurse:ancient_desert`
- **Commands**: `/ra_staff`, `/summon ancientcurse:djeserhath`

## Integration Notes
This mod follows vanilla patterns for maximum compatibility. The world generation system integrates with vanilla cave generation, ore distribution, and structure placement while creating Egyptian-themed flat desert terrain.