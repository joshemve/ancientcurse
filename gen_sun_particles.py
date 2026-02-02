from PIL import Image, ImageDraw
import os

def create_glow_particle(size, inner_color, outer_color, output_path):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    center = size / 2
    
    for y in range(size):
        for x in range(size):
            dist = ((x - center)**2 + (y - center)**2)**0.5
            normalized_dist = dist / (size / 2)
            
            if normalized_dist <= 1.0:
                # Quadratic falloff for smoother edges
                alpha = int(255 * (1.0 - normalized_dist)**2)
                
                # Add starburst spikes
                dx = abs(x - center)
                dy = abs(y - center)
                # Cross shape boost
                if dx < 1.5 or dy < 1.5:
                    alpha = min(255, int(alpha * 1.5))
                
                # Linear interpolation for color
                r = int(inner_color[0] + (outer_color[0] - inner_color[0]) * normalized_dist)
                g = int(inner_color[1] + (outer_color[1] - inner_color[1]) * normalized_dist)
                b = int(inner_color[2] + (outer_color[2] - inner_color[2]) * normalized_dist)
                img.putpixel((x, y), (r, g, b, alpha))
                
    img.save(output_path)
    print(f"Generated {output_path}")

def create_flare_particle(width, height, output_path):
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    center_y = height / 2
    
    for x in range(width):
        for y in range(height):
            dist_y = abs(y - center_y) / (height / 2)
            # Tapered flare: fades towards the tip (right side)
            dist_x = x / width 
            
            if dist_y <= 1.0:
                # Sharp center, smooth vertical falloff, fades over length
                alpha = int(255 * (1.0 - dist_y)**3 * (1.0 - dist_x))
                r, g, b = 255, 255, 210
                img.putpixel((x, y), (r, g, b, alpha))
                
    img.save(output_path)
    print(f"Generated {output_path}")

if __name__ == "__main__":
    resource_dir = r"c:\Users\Joshua\Documents\ancient_curse\src\main\resources\assets\ancientcurse\textures\particle"
    os.makedirs(resource_dir, exist_ok=True)
    
    create_glow_particle(16, (255, 255, 128), (255, 100, 0), os.path.join(resource_dir, "orb_fire.png"))
    create_flare_particle(32, 8, os.path.join(resource_dir, "orb_flare.png"))
