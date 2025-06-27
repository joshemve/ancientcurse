# Ancient Curse

A Minecraft Fabric mod that adds an Ancient Egyptian-themed world generation with custom biomes, items, blocks, and gameplay mechanics.

## Features Included

- **Ancient Curse World Type**: Custom world generation preset with desert-focused terrain
- **Ancient Desert Biome**: Custom biome featuring smooth sand surfaces and reduced water generation
- **Nile River Biome**: Lush river biome with unique flora and fauna
- **Custom Blocks**: Including smooth sand, fertile Nile silt, river sand, and various Egyptian-themed blocks
- **Special Items**: Ancient Egyptian artifacts including the Staff of Ra
- **Custom Entities**: Including Djeserhath boss mob
- **Egyptian Plants**: Papyrus reeds, lotus flowers, and more

## Installation

1. Make sure you have Fabric installed for Minecraft 1.20.1
2. Download the latest release from the releases page
3. Place the .jar file in your mods folder
4. Launch Minecraft and enjoy!

## Development

This mod is built using the Fabric toolchain for Minecraft 1.20.1.

To set up the development environment:
```
./gradlew genSources
./gradlew runClient
```

## Requirements

- Minecraft 1.20.1
- Fabric Loader 0.14.22+
- Fabric API 0.87.0+
- GeckoLib 4.2.3+

## World Generation

### Creating an Ancient Curse World

1. Click "Create New World" in Minecraft
2. Click "More World Options"
3. Select "Ancient Curse" from the World Type dropdown
4. Configure other settings as desired
5. Create the world

The Ancient Curse world type features:
- Desert-only generation with custom ancient_desert biome
- Smooth sand surfaces instead of regular sand
- Reduced water generation (sea level at Y=45 instead of Y=63)
- Flat terrain suitable for building Egyptian structures
- All vanilla cave generation and ore distribution

### Custom Biomes

- **ancient_desert**: Main desert biome with smooth sand surface
- **nile_river**: River biome with lush vegetation (work in progress)

## In-Game Commands

- `/give @p ancientcurse:staff_of_ra` - Gives you the Staff of Ra
- `/ra_staff` - Alternative command to get the Staff of Ra (requires op permission)
- `/summon ancientcurse:djeserhath` - Summons the Djeserhath boss mob

## Customizing the Mod

### Adding New Items

1. Create a new model in Blockbench and export as GeckoLib format
2. Place the model file in `src/main/resources/assets/ancientcurse/models/entity/`
3. Place the animation file in `src/main/resources/assets/ancientcurse/animations/`
4. Place the texture in `src/main/resources/assets/ancientcurse/textures/item/`
5. Create a standard item model JSON in `src/main/resources/assets/ancientcurse/models/item/`
6. Register your new item in the `ModItems` class

### Animation Tips

- All animations are defined in the `.animation.json` file
- Reference animations in your code by their exact name (e.g., `animation.model.sun`)
- You can switch animations based on different events (right-click, equip, etc.)

## Troubleshooting

If animations don't work:
1. Check the Minecraft log for errors
2. Verify file paths in your model class
3. Ensure animation names match exactly
4. Make sure your textures are in the correct location

## License

This project is licensed under the MIT License - see the LICENSE file for details. 