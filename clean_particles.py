from PIL import Image
import os

def clean_particle(input_path, output_path):
    img = Image.open(input_path).convert("RGBA")
    datas = img.getdata()

    newData = []
    for item in datas:
        # Check if it's black or very dark (near background)
        # Also check for the "fake" checkered background (usually gray/light gray)
        # A simple way for fire is: if max(r,g,b) < threshold, make transparent
        # and use brightness for alpha.
        
        r, g, b, a = item
        brightness = max(r, g, b)
        
        # If it's a "checkered" background, it usually has r == g == b and is gray
        # But for fire, we want the glow.
        
        if r == g == b and r < 150: # Likely the fake checkered background or black
            newData.append((0, 0, 0, 0))
        elif brightness < 20: # Black
            newData.append((0, 0, 0, 0))
        else:
            # Keep the color, maybe boost alpha
            newData.append((r, g, b, 255))

    img.putdata(newData)
    img.save(output_path, "PNG")
    print(f"Cleaned {input_path} -> {output_path}")

if __name__ == "__main__":
    base_dir = r"C:\Users\Joshua\.gemini\antigravity\brain\3d240579-3bea-4095-af53-8f9aa2058b3a"
    fire_in = os.path.join(base_dir, "orb_fire_particle_v2_1769474431909.png")
    fire_out = os.path.join(base_dir, "orb_fire_cleaned.png")
    
    flare_in = os.path.join(base_dir, "orb_flare_particle_v2_1769474444266.png")
    flare_out = os.path.join(base_dir, "orb_flare_cleaned.png")
    
    clean_particle(fire_in, fire_out)
    clean_particle(flare_in, flare_out)
