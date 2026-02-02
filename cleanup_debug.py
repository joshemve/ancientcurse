import os
import re

files_to_clean = [
    r"c:\Users\Joshua\Documents\ancient_curse\src\main\java\com\ancientcurse\network\CurseZonePackets.java",
    r"c:\Users\Joshua\Documents\ancient_curse\src\main\java\com\ancientcurse\mixin\client\GameRendererScreenShakeMixin.java",
    r"c:\Users\Joshua\Documents\ancient_curse\src\main\java\com\ancientcurse\entity\RaEntity.java",
    r"c:\Users\Joshua\Documents\ancient_curse\src\main\java\com\ancientcurse\entity\ai\RaShardAttackGoal.java",
    r"c:\Users\Joshua\Documents\ancient_curse\src\main\java\com\ancientcurse\entity\ai\RaSunBeamAttackGoal.java",
    r"c:\Users\Joshua\Documents\ancient_curse\src\main\java\com\ancientcurse\client\ScreenShakeManager.java"
]

def clean_java_logs(filepath):
    if not os.path.exists(filepath):
        print(f"File not found: {filepath}")
        return
    
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    new_lines = []
    skip_next = False
    
    # Simple line-by-line removal for System.out.println
    # Patterns to match: 
    # 1. System.out.println(...);
    # 2. if (...) { System.out.println(...); }
    # 3. if (...) { \n System.out.println(...); \n }
    
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Check for framed logs (if blocks)
        if "if (System.currentTimeMillis() %" in line or "if (entity.age % 20 == 0)" in line:
            # Skip the if block (usually 2-3 lines)
            if "{" in line:
                # Find matching brace (simple implementation)
                while i < len(lines) and "}" not in lines[i]:
                    i += 1
                i += 1 # Skip the closing brace line
                continue
        
        if "System.out.println" in line:
            # Check if it's a multi-line println
            if ";" not in line:
                while i < len(lines) and ";" not in lines[i]:
                    i += 1
            i += 1
            continue
        
        new_lines.append(line)
        i += 1
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
    print(f"Cleaned {filepath}")

def compact_animation_vectors(filepath):
    if not os.path.exists(filepath):
        print(f"File not found: {filepath}")
        return
        
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Replace:
    # "vector": [
    #     -0.5,
    #     0,
    #     0
    # ]
    # with:
    # "vector": [-0.5, 0, 0]
    
    # Regex: find "vector": [ followed by whitespace and numbers with commas
    pattern = r'"vector":\s*\[\s*(-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)\s*\]'
    # Wait, the expanded version has newlines.
    
    # Better approach: Collapse all [ ... ] that contain only numbers and whitespace/commas
    def collapse_array(match):
        arr_content = match.group(1)
        # Remove newlines and extra spaces, but keep commas
        collapsed = re.sub(r'\s+', ' ', arr_content).strip()
        # Ensure commas have a space after them
        collapsed = re.sub(r',\s*', ', ', collapsed)
        return f'[{collapsed}]'

    # Match [ surrounded by newlines and numbers
    content = re.sub(r'\[([^\[\]]+?)\]', collapse_array, content, flags=re.DOTALL)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Compacted {filepath}")

# Clean Java files
for f in files_to_clean:
    clean_java_logs(f)

# Compact animation
compact_animation_vectors(r"c:\Users\Joshua\Documents\ancient_curse\src\main\resources\assets\ancientcurse\animations\ra.animation.json")
