import json
import sys

def fix_geo(filepath):
    with open(filepath, 'r') as f:
        data = json.load(f)
    
    modified = False
    for geo in data.get('minecraft:geometry', []):
        for bone in geo.get('bones', []):
            for cube in bone.get('cubes', []):
                size = cube.get('size', [])
                for i in range(len(size)):
                    if size[i] == 0:
                        size[i] = 0.1
                        modified = True
                        
    if modified:
        with open(filepath, 'w') as f:
            json.dump(data, f, indent='\t')
        print(f"Fixed zero sizes in {filepath}")
    else:
        print(f"No zero sizes found in {filepath}")

if __name__ == "__main__":
    for path in sys.argv[1:]:
        fix_geo(path)
