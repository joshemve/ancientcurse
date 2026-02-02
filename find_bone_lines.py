import json
import sys

def find_bone_lines(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    print(f"Bones in {filepath}:")
    for i, line in enumerate(lines):
        if '"name":' in line:
            print(f"Line {i+1}: {line.strip()}")

if __name__ == "__main__":
    for path in sys.argv[1:]:
        find_bone_lines(path)
