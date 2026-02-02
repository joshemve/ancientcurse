import json
import sys

def list_bones(filepath):
    with open(filepath, 'r') as f:
        data = json.load(f)
    print(f"Bones in {filepath}:")
    for geo in data.get('minecraft:geometry', []):
        for bone in geo.get('bones', []):
            name = bone.get('name')
            parent = bone.get('parent')
            print(f" - {name} (parent: {parent})")

if __name__ == "__main__":
    for path in sys.argv[1:]:
        list_bones(path)
