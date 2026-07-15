import os
import re
import glob
import sys
from datetime import datetime

# Force UTF-8 output for emojis in Windows console
if sys.stdout.encoding.lower() != 'utf-8':
    try: sys.stdout.reconfigure(encoding='utf-8')
    except: pass

try:
    import msvcrt
except ImportError:
    msvcrt = None
    import msvcrt
except ImportError:
    msvcrt = None

DIR = r"c:\Users\66664\OneDrive\Desktop\Coding\CustomBlocks-B\docs\testing"

def generate_dashboard():
    files = glob.glob(os.path.join(DIR, "Group_[0-9][0-9]_Testing_Guide*.md"))
    dashboard = ["# Master Testing Dashboard\n"]
    dashboard.append(f"*Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*\n")
    dashboard.append("| Group | Verdict | Progress | Last Tested |")
    dashboard.append("|---|---|---|---|")
    
    for f in sorted(files):
        name = os.path.basename(f).replace("_TESTING_GUIDE.md", "")
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
            
        verdict = re.search(r'\|\s*\*\*Verdict\*\*\s*\|\s*(.*?)\s*\|', content)
        progress = re.search(r'\|\s*\*\*Progress\*\*\s*\|\s*(.*?)\s*\|', content)
        last_tested = re.search(r'\|\s*\*\*Last tested\*\*\s*\|\s*(.*?)\s*\|', content)
        
        v_str = verdict.group(1) if verdict else "Unknown"
        p_str = progress.group(1) if progress else "Unknown"
        lt_str = last_tested.group(1) if last_tested else "Unknown"
        
        title_match = re.search(r'^#\s*(.*?)$', content, re.MULTILINE)
        display_name = title_match.group(1).strip() if title_match else name
        
        link = f"[{display_name}]({os.path.basename(f)})"
        dashboard.append(f"| {link} | {v_str} | {p_str} | {lt_str} |")

    dash_path = os.path.join(DIR, "00_DASHBOARD.md")
    with open(dash_path, 'w', encoding='utf-8') as file:
        file.write("\n".join(dashboard) + "\n")
    print(f"✅ Generated {dash_path}")

def give_me_work():
    files = glob.glob(os.path.join(DIR, "Group_[0-9][0-9]_Testing_Guide*.md"))
    for f in sorted(files):
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
            
        match = re.search(r'# 🎯 Test now\s*(.*?)(?=# 🗄️|# 🐛|---)', content, re.DOTALL)
        if match:
            test_now = match.group(1)
            row_match = re.search(r'\|\s*([A-Z0-9]+)\s*\|([^|]+)\|([^|]+)\|\s*(?:🟥|not tested)\s*\|', test_now)
            if row_match:
                print(f"\n🚀 YOUR NEXT TASK:")
                print(f"File:   {os.path.basename(f)}")
                print(f"ID:     {row_match.group(1).strip()}")
                print(f"Action: {row_match.group(2).strip()}")
                print(f"Expect: {row_match.group(3).strip()}\n")
                return
    print("\n🎉 No active tasks found. You are 100% complete or everything is parked!\n")

def archive_completed():
    files = glob.glob(os.path.join(DIR, "Group_[0-9][0-9]_Testing_Guide*.md"))
    archive_dir = os.path.join(DIR, "completed")
    os.makedirs(archive_dir, exist_ok=True)
    
    moved = 0
    for f in sorted(files):
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
        
        progress = re.search(r'\|\s*\*\*Progress\*\*\s*\|\s*(.*?)\s*\|', content)
        if progress and "100%" in progress.group(1):
            base = os.path.basename(f)
            new_path = os.path.join(archive_dir, base)
            os.rename(f, new_path)
            print(f"📦 Archived {base} -> 100% complete!")
            moved += 1
            
    if moved == 0:
        print("\nℹ️ No guides are at 100% yet.\n")
    else:
        print(f"\n✅ Archived {moved} guide(s).\n")

def master_bug_board():
    files = glob.glob(os.path.join(DIR, "Group_[0-9][0-9]_Testing_Guide*.md"))
    bugs = ["# Master Bug Board\n", "*All active bugs across all testing guides.*\n", "| Guide | Bug ID | Test Row | Issue Description | Status |", "|---|---|---|---|---|"]
    
    count = 0
    for f in sorted(files):
        name = os.path.basename(f)
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
            
        bug_match = re.search(r'# 🐛 Active Bugs.*?(\|-.*?-\|.*?)(?=\n---|)$', content, re.DOTALL)
        if bug_match:
            lines = bug_match.group(1).strip().split('\n')
            for line in lines[1:]: # skip the separator |---|---|
                parts = [p.strip() for p in line.split('|')[1:-1]]
                if len(parts) >= 4 and parts[0] != "":
                    # Has a bug ID
                    link = f"[{name}]({name})"
                    bugs.append(f"| {link} | {parts[0]} | {parts[1]} | {parts[2]} | {parts[3]} |")
                    count += 1
                    
    bugs_path = os.path.join(DIR, "00_BUGS.md")
    with open(bugs_path, 'w', encoding='utf-8') as file:
        file.write("\n".join(bugs) + "\n")
    print(f"\n✅ Compiled {count} bugs into {bugs_path}\n")

def table_formatter():
    files = glob.glob(os.path.join(DIR, "*.md"))
    formatted_count = 0
    
    for f in files:
        with open(f, 'r', encoding='utf-8') as file:
            lines = file.readlines()
            
        in_table = False
        table_lines = []
        new_lines = []
        
        def process_table():
            nonlocal table_lines, new_lines
            if not table_lines: return
            
            # Find max col widths
            parsed = []
            for t_line in table_lines:
                cols = [c.strip() for c in t_line.split('|')]
                if len(cols) > 2: # valid table row
                    parsed.append(cols)
                else:
                    parsed.append(None) # Not a valid row, keep raw
                    
            valid_rows = [p for p in parsed if p is not None]
            if not valid_rows:
                new_lines.extend(table_lines)
                table_lines = []
                return
                
            col_count = max(len(p) for p in valid_rows)
            col_widths = [0] * col_count
            
            for row in valid_rows:
                for i, col in enumerate(row):
                    if i < col_count and not re.match(r'^:?-+:?$', col):
                        col_widths[i] = max(col_widths[i], len(col))
            
            # Format
            for row, orig_line in zip(parsed, table_lines):
                if row is None:
                    new_lines.append(orig_line)
                    continue
                
                formatted_cols = []
                for i, col in enumerate(row):
                    if i == 0 or i == len(row) - 1: # Empty edges
                        formatted_cols.append("")
                    else:
                        if re.match(r'^:?-+:?$', col): # Separator row
                            formatted_cols.append("-" * max(3, col_widths[i]))
                        else:
                            formatted_cols.append(col.ljust(col_widths[i]))
                
                new_lines.append("| " + " | ".join(formatted_cols[1:-1]) + " |\n")
            table_lines = []
        
        changed = False
        for line in lines:
            if line.strip().startswith('|') and line.strip().endswith('|'):
                in_table = True
                table_lines.append(line)
            else:
                if in_table:
                    process_table()
                    in_table = False
                new_lines.append(line)
                
        if in_table:
            process_table()
            
        result = "".join(new_lines)
        if "".join(lines) != result:
            with open(f, 'w', encoding='utf-8') as file:
                file.write(result)
            formatted_count += 1
            
    print(f"\n✨ Formatted tables in {formatted_count} files.\n")

SECTION_PRIORITY = {
    '🎯': 0,
    '💔': 1, '❔': 1, '❗': 1,
    '🟡': 2, '⚠️': 2,
    '✅': 3,
    '🛠️': 4,
    '🧊': 5,
    '⏳': 6, '🟥': 6,
}

def _state_priority(state_text):
    matches = [SECTION_PRIORITY[sym] for sym in SECTION_PRIORITY if sym in state_text]
    return min(matches) if matches else 99

# Terms the glossary (extra/00_GLOSSARY.md) explicitly says to avoid
BANNED_TERMS = ['build green', 'build-green']

def _check_section_fraction_drift(name, content):
    """A section header like '## A · Foo · 🎯 2/7' claims 2/7 passed.
    Verify that against the actual SP/MP table directly below it."""
    warnings = 0
    for m in re.finditer(r'^##\s+([A-Z0-9]+)\s*·.*?(\d+)/(\d+)', content, re.MULTILINE):
        claimed_pass, claimed_total = int(m.group(2)), int(m.group(3))
        table_start = content.find('\n|', m.end())
        if table_start == -1:
            continue
        table_end = content.find('\n\n', table_start)
        table = content[table_start:table_end if table_end != -1 else None]
        rows = [r for r in table.split('\n') if r.strip().startswith('|')][2:]  # skip header+sep
        if not rows:
            continue
        actual_total = len(rows)
        actual_pass = 0
        for row in rows:
            cells = [c.strip() for c in row.split('|')]
            if len(cells) >= 3 and cells[-2].startswith('✅') and cells[-3].startswith('✅'):
                actual_pass += 1
        if (actual_pass, actual_total) != (claimed_pass, claimed_total):
            print(f"  [WARN] {name} §{m.group(1)} -> header claims {claimed_pass}/{claimed_total}, table shows {actual_pass}/{actual_total}")
            warnings += 1
    return warnings

def _check_banned_terms(name, content):
    warnings = 0
    lowered = content.lower()
    for term in BANNED_TERMS:
        if term in lowered:
            print(f"  [WARN] {name} -> banned term '{term}' found (00_GLOSSARY.md says avoid it)")
            warnings += 1
    return warnings

def health_check():
    files = glob.glob(os.path.join(DIR, "Group_[0-9][0-9]_Testing_Guide*.md"))
    groups_dir = os.path.normpath(os.path.join(DIR, "..", "groups"))
    errors = 0
    print("\n🩺 Running Health Check...")

    for f in sorted(files):
        name = os.path.basename(f)
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
            lines = content.split('\n')

        in_test_now = False
        for i, line in enumerate(lines):
            if "# 🎯 Test now" in line: in_test_now = True
            if "# 🗄️ Archive" in line: in_test_now = False

            # Check unformatted checkboxes in passed rows
            if in_test_now and line.strip().startswith('|'):
                if "✅" in line and "[ ]" in line:
                    print(f"  [WARN] {name}:{i+1} -> Row marked ✅ but has unchecked [ ]")
                    errors += 1

                # Check for uneven table pipes
                if line.strip().endswith('|') and line.count('|') < 3:
                    print(f"  [WARN] {name}:{i+1} -> Broken table row")
                    errors += 1

        # Related Doc link must resolve to a real file in docs/groups/
        link_match = re.search(r'Related Doc:\*\*\s*\[[^\]]+\]\(([^)]+)\)', content)
        if link_match:
            link = link_match.group(1)
            resolved = os.path.normpath(os.path.join(DIR, link))
            if not os.path.isfile(resolved):
                print(f"  [WARN] {name} -> Related Doc link broken: {link}")
                errors += 1
        else:
            print(f"  [WARN] {name} -> No Related Doc link found")
            errors += 1

        # Sections table must be priority-sorted, not alphabetical
        sec_match = re.search(r'## 🗺️ Sections\n\n(?:>.*\n\n)?((?:\|.*\n)+)', content)
        if sec_match:
            rows = sec_match.group(1).rstrip('\n').split('\n')[2:]
            priorities = []
            for row in rows:
                cells = [c.strip() for c in row.split('|')]
                state_cell = cells[-2] if len(cells) >= 2 else ''
                priorities.append(_state_priority(state_cell))
            if priorities != sorted(priorities):
                print(f"  [WARN] {name} -> Sections table not priority-sorted (must be 🎯→💔→🟡→✅→🛠️→🧊→⏳)")
                errors += 1

        # No root-cause/implementation leaks (💬 callout marker)
        if '💬' in content:
            print(f"  [WARN] {name} -> 💬 root-cause/implementation callout found (belongs in PROGRESS_LOG.md)")
            errors += 1

        # Banner (everything before first '## ') must be <= 3 non-empty lines
        banner = content.split('## ', 1)[0]
        banner_lines = [l for l in banner.split('\n') if l.strip()]
        if len(banner_lines) > 3:
            print(f"  [WARN] {name} -> Banner is {len(banner_lines)} lines (max 3)")
            errors += 1

        # Test-row tables must use SP/MP columns, not a single Status column
        if re.search(r'^\|\s*#\s*\|.*\|\s*Status\s*\|\s*$', content, re.MULTILINE):
            print(f"  [WARN] {name} -> test-row table uses single 'Status' column, needs SP/MP")
            errors += 1

        # Section header fraction must match its own table's actual pass count
        errors += _check_section_fraction_drift(name, content)

        # Fully-passed sections must be folded into Archive, not left in Test now
        test_now_match = re.search(r'# 🎯 Test now(.*?)(?=\n# 🗄️ Archive|\Z)', content, re.S)
        if test_now_match:
            for h in re.finditer(r'^##\s+\S\s*·[^\n]*$', test_now_match.group(1), re.MULTILINE):
                header = h.group(0)
                m2 = re.search(r'(\d+)/(\d+)', header)
                if '✅' in header and (not m2 or m2.group(1) == m2.group(2)):
                    print(f"  [WARN] {name} -> section fully passed but still under 'Test now': {header.strip()} (fold into Archive)")
                    errors += 1

        # Banned terminology (per 00_GLOSSARY.md)
        errors += _check_banned_terms(name, content)

    if errors == 0:
        print("✅ Health Check Passed! All guides look pristine.\n")
    else:
        print(f"❌ Found {errors} issues.\n")

def daily_snapshot():
    date_str = datetime.now().strftime('%Y-%m-%d')
    report_dir = os.path.join(DIR, "reports")
    os.makedirs(report_dir, exist_ok=True)
    report_path = os.path.join(report_dir, f"TEST_RUN_{date_str}.md")
    
    files = glob.glob(os.path.join(DIR, "Group_[0-9][0-9]_Testing_Guide*.md"))
    report = [f"# Test Snapshot: {date_str}\n"]
    
    passed_count = 0
    total_count = 0
    
    for f in sorted(files):
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
        
        progress = re.search(r'\|\s*\*\*Progress\*\*\s*\|\s*.*?\(\s*(\d+)\s*/\s*(\d+)\s*passed\)\s*\|', content)
        if progress:
            passed_count += int(progress.group(1))
            total_count += int(progress.group(2))
            
    pct = int((passed_count / total_count) * 100) if total_count > 0 else 0
    report.append(f"### Overall Project Progress: {pct}% ({passed_count}/{total_count} tests passed)\n")
    
    # We could add more details, but keeping it simple for now.
    with open(report_path, 'w', encoding='utf-8') as file:
        file.write("\n".join(report) + "\n")
    print(f"\n📸 Snapshot generated at docs/testing/reports/TEST_RUN_{date_str}.md\n")

def interactive_menu():
    if not msvcrt:
        print("Interactive menu requires msvcrt (Windows). Please use command line args.")
        return
        
    options = [
        ("View/Generate Dashboard", generate_dashboard),
        ("Get Next Task (Give Me Work)", give_me_work),
        ("Format Markdown Tables", table_formatter),
        ("Run Health Check", health_check),
        ("Compile Master Bug Board", master_bug_board),
        ("Generate Daily Snapshot", daily_snapshot),
        ("Archive 100% Completed Guides", archive_completed),
        ("Exit", sys.exit)
    ]
    
    selected = 0
    while True:
        os.system('cls' if os.name == 'nt' else 'clear')
        print("=== CustomBlocks Testing Tools ===\n")
        print("Use UP/DOWN arrows to select, ENTER to run.\n")
        
        for i, (text, _) in enumerate(options):
            if i == selected:
                print(f"  > [ {text} ]")
            else:
                print(f"    {text}")
                
        key = ord(msvcrt.getch())
        if key == 224: # Arrow keys
            key = ord(msvcrt.getch())
            if key == 72: # Up
                selected = (selected - 1) % len(options)
            elif key == 80: # Down
                selected = (selected + 1) % len(options)
        elif key == 13: # Enter
            os.system('cls' if os.name == 'nt' else 'clear')
            options[selected][1]()
            print("Press any key to return to menu...")
            msvcrt.getch()

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "dashboard": generate_dashboard()
        elif sys.argv[1] == "work": give_me_work()
        elif sys.argv[1] == "archive": archive_completed()
        elif sys.argv[1] == "format": table_formatter()
        elif sys.argv[1] == "health": health_check()
        elif sys.argv[1] == "bugs": master_bug_board()
        elif sys.argv[1] == "snapshot": daily_snapshot()
    else:
        interactive_menu()
