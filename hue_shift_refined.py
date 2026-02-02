import os
from PIL import Image
import colorsys

def improved_hue_shift(input_path, output_path, target_hue, saturation_boost=1.1, lightness_boost=1.0):
    if not os.path.exists(input_path):
        print(f"Error: {input_path} not found")
        return

    img = Image.open(input_path).convert("RGBA")
    pixels = img.load()

    for y in range(img.size[1]):
        for x in range(img.size[0]):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            
            # Convert RGB to HLS
            h, l, s = colorsys.rgb_to_hls(r/255.0, g/255.0, b/255.0)
            
            # Shift hue to soul fire
            if s > 0.05:
                h = target_hue
                s = min(1.0, s * saturation_boost)
                # Boost lightness more for already light pixels to bring out "whites"
                if l > 0.6:
                    l = min(1.0, l * 1.2 * lightness_boost)
                else:
                    l = min(1.0, l * lightness_boost)
            
            # Convert back to RGB
            nr, ng, nb = colorsys.hls_to_rgb(h, l, s)
            pixels[x, y] = (int(nr*255), int(ng*255), int(nb*255), a)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path)
    print(f"Refined texture saved to {output_path}")

# Adjusting based on feedback: "slightly brighter blue and the whites brought out"
TARGET_HUE = 0.51 # A bit more towards blue than the 0.47 cyan
SAT_BOOST = 1.1
LIGHT_BOOST = 1.15

BASE_DIR = r"c:\Users\Joshua\Documents\ancient_curse"
REF_DIR = os.path.join(BASE_DIR, "1.20.1_minecraft_reference_files", "assets", "minecraft", "textures")
ASSET_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "assets", "ancientcurse", "textures")

improved_hue_shift(
    os.path.join(REF_DIR, "block", "lava_still.png"),
    os.path.join(ASSET_DIR, "block", "soul_lava_still.png"),
    TARGET_HUE, SAT_BOOST, LIGHT_BOOST
)

improved_hue_shift(
    os.path.join(REF_DIR, "block", "lava_flow.png"),
    os.path.join(ASSET_DIR, "block", "soul_lava_flow.png"),
    TARGET_HUE, SAT_BOOST, LIGHT_BOOST
)

improved_hue_shift(
    os.path.join(REF_DIR, "item", "lava_bucket.png"),
    os.path.join(ASSET_DIR, "item", "soul_lava_bucket.png"),
    TARGET_HUE, SAT_BOOST, LIGHT_BOOST
)
