# Ancient Curse

A Minecraft Fabric mod that adds an Ancient Egyptian dimension with custom biomes, items, blocks, and gameplay mechanics.

## Features Included

- Ancient Egyptian dimension with custom terrain generation
- Nile River biome with unique flora and fauna
- Special items inspired by Ancient Egyptian artifacts
- Custom blocks including fertile Nile silt, river sand, and more
- Custom crafting recipes and gameplay mechanics

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

## In-Game Commands

- `/give @p ancientcurse:staff_of_ra` - Gives you the Staff of Ra
- `/ra_staff` - Alternative command to get the Staff of Ra (requires op permission)

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