import os
import re
import glob
import json
import sys
from datetime import datetime

DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DASH_DIR = os.path.join(os.path.dirname(__file__), "dashboard")
EXTRA_DIR = os.path.join(DIR, "extra")

if EXTRA_DIR not in sys.path:
    sys.path.insert(0, EXTRA_DIR)

from testing_tools import _guide_records

def extract_data():
    data = {
        "lastUpdated": datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        "groups": [],
        "bugs": [],
        "nextTask": None
    }
    
    # The Markdown dashboard and this supporting data must use the same guide.
    for record in _guide_records():
        f = os.path.join(DIR, record["filename"])
        base = os.path.basename(f)
        group_id = f"GROUP_{record['id']}"
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
            
        display_name = record["title"]
        verdict = record["verdict"]
        progress_raw = record["progress"]
        last_tested = record["tested"]
        
        pct = 0
        passed = 0
        total = 0
        pct_m = re.search(r'(100|[1-9]?\d)%', progress_raw)
        if pct_m:
            pct = int(pct_m.group(1))
            passed = progress_raw.count('\U0001F7E9')
            total = passed + progress_raw.count('\U0001F7E5')
            
        group_data = {
            "id": group_id,
            "title": display_name,
            "filename": base,
            "verdict": verdict,
            "percent": pct,
            "passed": passed,
            "total": total,
            "lastTested": last_tested
        }
        data["groups"].append(group_data)
        
        bug_match = re.search(r'(?ms)^#\s+Active Bugs\s*$\n(.*?)(?=^#\s|\Z)', content)
        if bug_match:
            lines = [line for line in bug_match.group(1).split('\n') if line.startswith('|')]
            for line in lines[2:]:
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
            match = re.search(r'(?ms)^#\s+Active Tests\s*$\n(.*?)(?=^#\s|\Z)', content)
            if match:
                row_match = None
                for candidate in re.finditer(r'\|\s*([A-Z0-9]+)\s*\|([^|]+)\|([^|]+)\|([^|]+)\|([^|]+)\|', match.group(1)):
                    if '\U0001F3AF' in candidate.group(4) or '\U0001F3AF' in candidate.group(5):
                        row_match = candidate
                        break
                if row_match is not None:
                    data["nextTask"] = {
                        "group": display_name,
                        "filename": base,
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

    stats_path = os.path.join(DIR, "Dashboard_Stats.json")
    with open(stats_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        
    print(f"Web data built at {out_path}")

if __name__ == "__main__":
    build_web()
