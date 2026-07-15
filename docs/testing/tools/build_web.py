import os
import re
import glob
import json
from datetime import datetime

DIR = r"c:\Users\66664\OneDrive\Desktop\Coding\CustomBlocks-B\docs\testing"
DASH_DIR = os.path.join(DIR, "dashboard")

def extract_data():
    files = glob.glob(os.path.join(DIR, "GROUP_*_TESTING_GUIDE.md"))
    data = {
        "lastUpdated": datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        "groups": [],
        "bugs": [],
        "nextTask": None
    }
    
    for f in sorted(files):
        name = os.path.basename(f).replace("_TESTING_GUIDE.md", "")
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
            
        verdict_m = re.search(r'\|\s*\*\*Verdict\*\*\s*\|\s*(.*?)\s*\|', content)
        progress_m = re.search(r'\|\s*\*\*Progress\*\*\s*\|\s*(.*?)\s*\|', content)
        last_tested_m = re.search(r'\|\s*\*\*Last tested\*\*\s*\|\s*(.*?)\s*\|', content)
        title_match = re.search(r'^#\s*(.*?)$', content, re.MULTILINE)
        
        display_name = title_match.group(1).strip() if title_match else name
        verdict = verdict_m.group(1) if verdict_m else "Unknown"
        progress_raw = progress_m.group(1) if progress_m else ""
        last_tested = last_tested_m.group(1) if last_tested_m else "Unknown"
        
        pct = 0
        passed = 0
        total = 0
        pct_m = re.search(r'(\d+)%\s*\((\d+)\/(\d+)', progress_raw)
        if pct_m:
            pct = int(pct_m.group(1))
            passed = int(pct_m.group(2))
            total = int(pct_m.group(3))
            
        group_data = {
            "id": name,
            "title": display_name,
            "filename": os.path.basename(f),
            "verdict": verdict,
            "percent": pct,
            "passed": passed,
            "total": total,
            "lastTested": last_tested
        }
        data["groups"].append(group_data)
        
        bug_match = re.search(r'# 🐛 Active Bugs.*?(\|-.*?-\|.*?)(?=\n---|)$', content, re.DOTALL)
        if bug_match:
            lines = bug_match.group(1).strip().split('\n')
            for line in lines[1:]: 
                parts = [p.strip() for p in line.split('|')[1:-1]]
                if len(parts) >= 4 and parts[0] != "":
                    data["bugs"].append({
                        "group": display_name,
                        "bugId": parts[0],
                        "testRow": parts[1],
                        "issue": parts[2],
                        "status": parts[3]
                    })
                    
        if data["nextTask"] is None:
            match = re.search(r'# 🎯 Test now\s*(.*?)(?=# 🗄️|# 🐛|---)', content, re.DOTALL)
            if match:
                test_now = match.group(1)
                row_match = re.search(r'\|\s*([A-Z0-9]+)\s*\|([^|]+)\|([^|]+)\|\s*(?:🟥|not tested)\s*\|', test_now)
                if row_match:
                    data["nextTask"] = {
                        "group": display_name,
                        "filename": os.path.basename(f),
                        "id": row_match.group(1).strip(),
                        "action": row_match.group(2).strip(),
                        "expect": row_match.group(3).strip()
                    }
                    
    return data

def build_web():
    os.makedirs(DASH_DIR, exist_ok=True)
    data = extract_data()
    js_content = f"const dashboardData = {json.dumps(data, indent=2)};"
    
    out_path = os.path.join(DASH_DIR, "data.js")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(js_content)
        
    print(f"Web data built at {out_path}")

if __name__ == "__main__":
    build_web()
