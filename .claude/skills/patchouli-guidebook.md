# Patchouli Guidebook Skill

This skill provides expert knowledge for creating and updating Patchouli guidebooks in the Ancient Curse mod.

## Quick Reference

### File Locations
```
data/ancientcurse/patchouli_books/ancient_tome/
└── book.json                    # Book configuration

assets/ancientcurse/patchouli_books/ancient_tome/en_us/
├── categories/                  # Category definitions
│   ├── getting_started.json
│   ├── blocks.json
│   ├── items.json
│   ├── entities.json
│   └── world_generation.json
└── entries/                     # Entry pages
    ├── getting_started/
    ├── blocks/
    ├── items/
    └── entities/
```

### Give Command
```
/give @s patchouli:guide_book{"patchouli:book":"ancientcurse:ancient_tome"}
```

---

## TEXT FORMATTING RULES (Avoid UI Issues)

### Character Limits Per Page
- **Text pages**: ~380-420 characters max (including formatting codes)
- **Spotlight pages**: ~200-250 characters for description
- **With title**: Reduce by ~50 characters

### Line Break Rules
- `$(br)` = Single line break
- `$(br2)` = Paragraph break (use between topics)
- NEVER use more than 3 `$(li)` bullet points per page
- Nested bullets (`$(li2)`, `$(li3)`) take more horizontal space

### Formatting Codes
| Code | Effect | Usage |
|------|--------|-------|
| `$()` | Clear formatting | End colored/styled text |
| `$(br)` | Line break | Within paragraph |
| `$(br2)` | Paragraph break | Between sections |
| `$(li)` | Bullet point | Max 3-4 per page |
| `$(l)` | **Bold** | Headers, warnings |
| `$(o)` | *Italic* | Flavor text, notes |
| `$(item)` | Item highlight | Item names |
| `$(thing)` | Concept highlight | Game concepts |
| `$(#RRGGBB)` | Hex color | Custom colors |

### Custom Macros (defined in book.json)
- `$(egyptian)` = Sandy gold color (#C19A6B)
- `$(gold)` = Bright gold (#FFD700)
- `$(curse)` = Dark red italic (#8B0000)
- `$(deity)` = Blue bold (#4169E1)
- `$(warning)` = Red bold (#FF0000)
- `$(tip)` = Green italic (#00AA00)
- `$(nile)` = Nile blue (#1E90FF)

### Internal Links
```
$(l:category_id)Link to Category$(/l)
$(l:category_id/entry_id)Link to Entry$(/l)
$(l:category_id/entry_id#anchor)Link to Anchor$(/l)
```

---

## PAGE TYPE TEMPLATES

### Text Page
```json
{
  "type": "patchouli:text",
  "title": "Optional Title",
  "text": "Keep under 400 chars. Use $(br2) for paragraphs.$(br2)$(li)Bullet one$(li)Bullet two$(li)Bullet three"
}
```

### Spotlight Page (Item Showcase)
```json
{
  "type": "patchouli:spotlight",
  "item": "ancientcurse:item_id",
  "title": "Item Name",
  "text": "Keep under 250 chars for description.",
  "link_recipe": true
}
```

### Crafting Recipe Page
```json
{
  "type": "patchouli:crafting",
  "recipe": "ancientcurse:recipe_id",
  "recipe2": "ancientcurse:optional_second_recipe",
  "title": "Crafting Title",
  "text": "Brief description under 150 chars."
}
```

### Entity Page
```json
{
  "type": "patchouli:entity",
  "entity": "ancientcurse:entity_id",
  "scale": 0.5,
  "offset": 0.0,
  "rotate": true,
  "default_rotation": -45,
  "name": "Entity Name",
  "text": "Keep under 200 chars."
}
```

### Image Page
```json
{
  "type": "patchouli:image",
  "images": ["ancientcurse:textures/gui/book/image_name.png"],
  "title": "Image Title",
  "border": true,
  "text": "Caption under 150 chars."
}
```

### Multiblock Structure Page
```json
{
  "type": "patchouli:multiblock",
  "name": "Structure Name",
  "multiblock": {
    "pattern": [
      ["   ", " 0 ", "   "],
      ["SSS", "SGS", "SSS"]
    ],
    "mapping": {
      "S": "minecraft:stone_bricks",
      "G": "minecraft:gold_block",
      "0": "ancientcurse:block_id"
    }
  },
  "text": "Brief description."
}
```

### Relations Page (See Also)
```json
{
  "type": "patchouli:relations",
  "entries": [
    "ancientcurse:category/entry1",
    "ancientcurse:category/entry2"
  ],
  "title": "Related Topics",
  "text": "Brief intro to related content."
}
```

---

## CATEGORY TEMPLATE

Location: `assets/ancientcurse/patchouli_books/ancient_tome/en_us/categories/[name].json`

```json
{
  "name": "Category Name",
  "description": "Description shown on category page. Keep under 300 chars.",
  "icon": "ancientcurse:icon_item",
  "sortnum": 0
}
```

**sortnum values (current structure):**
- 0 = Getting Started
- 1 = World Generation
- 2 = Blocks
- 3 = Items & Tools
- 4 = Entities & Bosses

---

## ENTRY TEMPLATE

Location: `assets/ancientcurse/patchouli_books/ancient_tome/en_us/entries/[category]/[name].json`

```json
{
  "name": "Entry Name",
  "icon": "ancientcurse:item_icon",
  "category": "ancientcurse:category_id",
  "sortnum": 0,
  "pages": [
    {
      "type": "patchouli:text",
      "title": "Introduction",
      "text": "First page should always be text introducing the topic."
    },
    {
      "type": "patchouli:spotlight",
      "item": "ancientcurse:featured_item",
      "text": "Showcase the main item."
    }
  ]
}
```

---

## ADDING NEW CONTENT

### To Add a New Entry:
1. Create JSON file in `entries/[category]/[entry_name].json`
2. Set `"category": "ancientcurse:[category_id]"`
3. Set appropriate `sortnum` for ordering
4. Keep text within character limits

### To Add a New Category:
1. Create JSON file in `categories/[category_name].json`
2. Set unique `sortnum` for position
3. Create matching folder in `entries/`

### Common Issues & Fixes:
| Issue | Cause | Fix |
|-------|-------|-----|
| Text cut off | Too many characters | Split into multiple pages |
| Overlapping text | Long words/no breaks | Add `$(br)` breaks |
| Bullets overflow | Too many `$(li)` | Max 3-4 bullets per page |
| Missing entry | Wrong category ID | Check `"category"` matches exactly |
| Recipe not showing | Recipe ID wrong | Verify recipe exists in data/ |

---

## CURRENT BOOK STRUCTURE

```
data/ancientcurse/patchouli_books/ancient_tome/
└── book.json

assets/ancientcurse/patchouli_books/ancient_tome/en_us/
├── categories/
│   ├── getting_started.json    (sortnum: 0)
│   ├── world_generation.json   (sortnum: 1)
│   ├── blocks.json             (sortnum: 2)
│   ├── items.json              (sortnum: 3)
│   └── entities.json           (sortnum: 4)
└── entries/
    ├── getting_started/
    │   ├── welcome.json        (sortnum: 0)
    │   ├── khamsin_curse.json  (sortnum: 1)
    │   ├── ankh_system.json    (sortnum: 2)
    │   └── purification.json   (sortnum: 3)
    ├── world_generation/
    │   ├── biomes.json         (sortnum: 0)
    │   ├── nile_river.json     (sortnum: 1)
    │   └── deshret_desert.json (sortnum: 2)
    ├── blocks/
    │   ├── building_materials.json (sortnum: 0)
    │   ├── necrostone.json     (sortnum: 1)
    │   ├── cursed_blocks.json  (sortnum: 2)
    │   └── solar_spire.json    (sortnum: 3)
    ├── items/
    │   ├── staff_of_ra.json    (sortnum: 0)
    │   ├── bronze_tools.json   (sortnum: 1)
    │   ├── magical_weapons.json (sortnum: 2)
    │   ├── armor.json          (sortnum: 3)
    │   ├── artifacts.json      (sortnum: 4)
    │   └── consumables.json    (sortnum: 5)
    └── entities/
        ├── anubis.json         (sortnum: 0)
        ├── thoth.json          (sortnum: 1)
        ├── hostile_creatures.json (sortnum: 2)
        └── bosses.json         (sortnum: 3)
```

### Crafting Recipe
Location: `data/ancientcurse/recipes/ancient_tome.json`
- Book + Papyrus Paper = Ancient Tome

---

## MOD-SPECIFIC CONTENT INDEX

### Key Items to Document:
- Staff of Ra (flagship item, animated)
- Bronze Tools Set
- Ceremonial Armor Set
- Ankh items (restoration mechanics)
- Curse removal items
- Magical artifacts (Eye of Apophis, Canopic Heart Jar, etc.)

### Key Entities:
- Anubis (neutral, guardian)
- Thoth (neutral, magical)
- Djeserhath (boss)
- Withered Pharaoh (boss)
- Scarab Beetle (hostile)
- Locus/Baby Locus (hostile)
- Sun Golem (utility)

### Key Blocks:
- Deshret Blocks (red desert theme)
- Necrostone Blocks (tomb theme)
- Pillar Blocks (decorative)
- Cursed Plant Blocks
- Egyptian Plant Blocks
- Construction Blocks

### Key Biomes:
- Ancient Desert
- Deshret Desert
- Nile River

---

## TESTING CHECKLIST

Before committing changes:
- [ ] Text fits on pages (no overflow)
- [ ] All item IDs are valid
- [ ] All recipe IDs exist
- [ ] All entity IDs are valid
- [ ] Category references are correct
- [ ] Links work (`$(l:...)$(/l)`)
- [ ] Book opens without errors
- [ ] Images display correctly (if used)

Test command: `/give @s patchouli:guide_book{"patchouli:book":"ancientcurse:ancient_tome"}`
