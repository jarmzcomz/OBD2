import sys, json
from graphify.detect import detect
from pathlib import Path
result = detect(Path('.'))
# Write with explicit UTF-8, no BOM
with open('graphify-out/.graphify_detect.json', 'w', encoding='utf-8-sig') as f:
    json.dump(result, f, ensure_ascii=False, indent=2)
print("Detection complete")
