import json
import sys

def fix_hierarchy(filepath):
    with open(filepath, 'r') as f:
        data = json.load(f)
    
    modified = False
    for geo in data.get('minecraft:geometry', []):
        for bone in geo.get('bones', []):
            name = bone.get('name')
            if name in ['bottom', 'middle', 'middle2', 'top']:
                if bone.get('parent') != 'sekhem_cactus':
                    bone['parent'] = 'sekhem_cactus'
                    modified = True
                    print(f"Reparented {name} to sekhem_cactus in {filepath}")
    
    if modified:
        with open(filepath, 'w') as f:
            json.dump(data, f, indent='\t')

if __name__ == "__main__":
    for path in sys.argv[1:]:
        fix_hierarchy(path)
