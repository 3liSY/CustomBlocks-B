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

DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
GLOSSARY_PATH = os.path.join(DIR, "extra", "Glossary.md")

def _guide_rank(path):
    """Prefer one unsuffixed guide; legacy suffix copies are fallbacks only."""
    base = os.path.basename(path)
    if re.fullmatch(r'Testing_Guide_\d{2}\.md', base):
        return 1
    if re.search(r'_Done\.md$', base, re.IGNORECASE):
        return 2
    if re.search(r'_Scrapped\.md$', base, re.IGNORECASE):
        return 3
    return 4


def _guide_status(content, filename):
    """Return the dashboard label currently declared by the guide."""
    lower_name = filename.lower()
    if '_scrapped.md' in lower_name:
        return 'Scrapped 👎', 'scrapped'
    if '_done.md' in lower_name:
        progress = re.search(r'\|\s*\*\*Progress\*\*\s*\|\s*(.*?)\s*\|', content)
        if progress and '100%' not in progress.group(1):
            return '-', 'unknown'
        return 'Done ✅', 'done'

    statuses = re.findall(r'\|\s*[^|]+\s*\|\s*[^|]+\s*\|\s*(Built 🎯|Designed ⏳|Planned 📜|Done ✅)\s*\|', content)
    if not statuses:
        return '-', 'unknown'
    if 'Built 🎯' in statuses:
        return 'Built 🎯', 'built'
    if 'Designed ⏳' in statuses:
        return 'Designed ⏳', 'designed'
    if 'Planned 📜' in statuses:
        return 'Planned 📜', 'planned'
    return 'Done ✅', 'done'


def _guide_records():
    """Read one canonical guide per group and ignore suffix duplicates."""
    patterns = [
        os.path.join(DIR, 'Testing_Guide_[0-9][0-9]*.md'),
    ]
    candidates = {}
    for path in sorted(set(file for pattern in patterns for file in glob.glob(pattern))):
        match = re.search(r'Testing_Guide_(\d{2})', os.path.basename(path))
        if not match:
            continue
        group_id = match.group(1)
        previous = candidates.get(group_id)
        if previous is None or _guide_rank(path) < _guide_rank(previous):
            candidates[group_id] = path

    records = []
    for group_id, path in sorted(candidates.items()):
        with open(path, 'r', encoding='utf-8') as file:
            content = file.read()

        title_match = re.search(r'^#\s*(.*?)\s*$', content, re.MULTILINE)
        title = title_match.group(1).strip() if title_match else f'Group {group_id}'
        title = re.sub(r'^Group\s+\d{2}\s*[-—]\s*', '', title, flags=re.IGNORECASE)
        verdict_match = re.search(r'\|\s*\*\*Verdict\*\*\s*\|\s*(.*?)\s*\|', content)
        progress_match = re.search(r'\|\s*\*\*Progress\*\*\s*\|\s*(.*?)\s*\|', content)
        tested_match = re.search(r'\|\s*\*\*Last tested\*\*\s*\|\s*(.*?)\s*\|', content)
        source_match = re.search(r'\*\*Original Group:\*\*\s*\[[^\]]+\]\(([^)]+)\)', content)
        status, bucket = _guide_status(content, os.path.basename(path))

        records.append({
            'id': group_id,
            'title': title,
            'filename': os.path.basename(path),
            'source': source_match.group(1) if source_match else None,
            'verdict': verdict_match.group(1) if verdict_match else '-',
            'progress': progress_match.group(1) if progress_match else '-',
            'tested': tested_match.group(1) if tested_match else '-',
            'status': status,
            'bucket': bucket,
        })
    return records


def _dashboard_row(record, show_verdict=False):
    guide = f"[TG]({record['filename']})"
    source = f"[Group]({record['source']})" if record['source'] else '-'
    docs = f'{guide} · {source}'
    if show_verdict:
        return f"| G{record['id']} - {record['title']} | {record['verdict']} | {record['progress']} | {record['tested']} | {docs} |"
    return f"| G{record['id']} - {record['title']} | {record['progress']} | {record['tested']} | {docs} |"


def generate_dashboard():
    records = _guide_records()
    buckets = {
        'built': [record for record in records if record['bucket'] == 'built'],
        'designed': [record for record in records if record['bucket'] in {'designed', 'planned'}],
        'done': [record for record in records if record['bucket'] == 'done'],
        'scrapped': [record for record in records if record['bucket'] == 'scrapped'],
        'unknown': [record for record in records if record['bucket'] == 'unknown'],
    }
    continue_with = buckets['built'][0] if buckets['built'] else (buckets['designed'][0] if buckets['designed'] else None)

    nav_items = []
    if buckets['built']:
        nav_items.append('[Built](#built)')
    if buckets['designed']:
        nav_items.append('[Designed & Planned](#designed--planned)')
    if buckets['done']:
        nav_items.append('[Done](#done)')
    if buckets['scrapped']:
        nav_items.append('[Scrapped](#scrapped)')
    if buckets['unknown']:
        nav_items.append('[Needs review](#needs-review)')

    dashboard = [
        '# CustomBlocks-B',
        '',
        '## Documentation Dashboard',
        '',
        f'*Refreshed: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}*',
        '',
        f'**{len(buckets["built"])}** Built 🎯 · **{len(buckets["designed"])}** Designed ⏳ / Planned 📜 · **{len(buckets["done"])}** Done ✅ · **{len(buckets["scrapped"])}** Scrapped 👎',
        '',
        ' · '.join(nav_items),
        '',
    ]

    if continue_with:
        dashboard.extend([
            '## Continue',
            '',
            '| Group | Verdict | Progress | Last tested | Open |',
            '| --- | --- | --- | --- | --- |',
            _dashboard_row(continue_with, show_verdict=True),
            '',
        ])

    sections = [
        ('Built', buckets['built']),
        ('Designed & Planned', buckets['designed']),
        ('Done', buckets['done']),
        ('Scrapped', buckets['scrapped']),
        ('Needs review', buckets['unknown']),
    ]
    for heading, items in sections:
        if not items:
            continue
        dashboard.extend([f'## {heading}', '', f'<details><summary>{len(items)} group(s)</summary>', ''])
        dashboard.extend([
            '| Group | Progress | Last tested | Open |',
            '| --- | --- | --- | --- |',
        ])
        dashboard.extend(_dashboard_row(record) for record in items)
        dashboard.extend(['', '</details>', ''])

    dash_path = os.path.join(DIR, '00_DASHBOARD.md')
    with open(dash_path, 'w', encoding='utf-8') as file:
        file.write('\n'.join(dashboard).rstrip() + '\n')
    print(f'✅ Generated {dash_path}')

def give_me_work():
    for record in _guide_records():
        path = os.path.join(DIR, record["filename"])
        with open(path, 'r', encoding='utf-8') as file:
            content = file.read()

        active_tests = re.search(r'(?ms)^#\s+Active Tests\s*$\n(.*?)(?=^#\s|\Z)', content)
        if not active_tests:
            continue

        for row in re.finditer(r'\|\s*([A-Z0-9]+)\s*\|([^|]+)\|([^|]+)\|([^|]+)\|([^|]+)\|', active_tests.group(1)):
            if '\U0001F3AF' not in row.group(4) and '\U0001F3AF' not in row.group(5):
                continue
            print("\nNext test:")
            print(f"Guide:  {record['filename']}")
            print(f"ID:     {row.group(1).strip()}")
            print(f"Action: {row.group(2).strip()}")
            print(f"Expect: {row.group(3).strip()}\n")
            return

    print("\nNo active test rows were found. Everything is confirmed, parked, or awaiting design.\n")

def archive_completed():
    """Keep completed guides in place and verify their filename lifecycle."""
    print("\nCompleted guides stay in docs/testing with the _Done filename suffix.")
    print("No files were moved. Checking the lifecycle instead...\n")
    health_check()

def master_bug_board():
    print("\nThe master bug board is retired.")
    print("Keep each finding in its own Testing Guide so it stays with its test evidence.\n")

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

def _section_sort_priority(status, flags):
    if '👎' in flags:
        return 7
    if '💤' in flags:
        return 6
    if '💔' in flags or '‼️' in flags:
        return 1
    if '✏️' in flags or '🎨' in flags:
        return 2
    if status == 'Built 🎯':
        return 0
    if status == 'Designed ⏳':
        return 3
    if status == 'Planned 📜':
        return 4
    if status == 'Done ✅':
        return 5
    return 99

# Terms the glossary (extra/Glossary.md) explicitly says to avoid as status language.
BANNED_TERMS = [
    'build green',
    'build-green',
    'test-now',
    'passed tests history',
    'main status',
    'built, needs testing',
    'designed, unbuilt',
    'done, confirmed in-game',
    'scrapped/removed',
]

STATUS_VALUES = {'Planned 📜', 'Designed ⏳', 'Built 🎯', 'Done ✅'}
STATUS_ONLY_VALUES = STATUS_VALUES | {'📜', '⏳', '🎯', '✅'}
PROGRESS_BAR_RE = re.compile(r'^(?P<bar>(?:🟩|🟥){10})\s+(?P<pct>100|[1-9]?\d)%$')
LAST_TESTED_RE = re.compile(r'^\d{4}-\d{2}-\d{2}$')
JAR_RE = re.compile(r'^`[^`]+\.jar`$')
SECTION_HEADER = ['§', 'Feature', 'Status', 'Flags']
ARCHIVE_FOLD_HEADERS = [
    '✅ <b>Confirmed</b>',
    '💔 <b>Regression</b>',
    '📜 <b>Planned</b>',
    '💤 <b>Parked</b>',
    '👎 <b>Scrapped</b>',
]
CONFIRMED_ARCHIVE_RE = re.compile(r'^-\s+§[^—\n]+—\s+✅\s+`\d{4}-\d{2}-\d{2}`\s*$')

def _check_section_fraction_drift(name, content):
    """A section header like '## A · Foo · 🎯 2/7' claims 2/7 confirmed.
    Verify that against all SP/MP test tables inside that section."""
    warnings = 0
    for m in re.finditer(r'^##\s+([A-Z0-9]+)\s*(?:·|-).*?(\d{1,3})/(\d{1,3})(?!\d)', content, re.MULTILINE):
        claimed_pass, claimed_total = int(m.group(2)), int(m.group(3))
        section_end = content.find('\n## ', m.end())
        section_body = content[m.end():section_end if section_end != -1 else None]
        tables = re.findall(r'((?:^\|.*\|\s*\n)+)', section_body, re.MULTILINE)
        rows = []
        for table in tables:
            table_lines = [r for r in table.split('\n') if r.strip().startswith('|')]
            if not table_lines:
                continue
            header = [c.strip().lower() for c in table_lines[0].strip('|').split('|')]
            if not {'sp', 'mp'}.issubset(set(header)):
                continue
            rows.extend(table_lines[2:])  # skip header+separator
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
            print(f"  [WARN] {name} -> banned term '{term}' found (Glossary.md says avoid it)")
            warnings += 1
    return warnings

def _check_glossary_boundary(name, content):
    """Testing guides must obey the glossary without copying it into the guide."""
    warnings = 0
    if "Glossary.md" in content or "Confused by symbols or terms?" in content:
        print(f"  [WARN] {name} -> glossary reminder or glossary link found; keep glossary rules centralized")
        warnings += 1
    if "\U0001F9F0" in content:
        print(f"  [WARN] {name} -> old toolbox callout found; use 💡 for setup/check lines")
        warnings += 1
    return warnings

def _check_empty_table_cells(name, content):
    """Real table data cells must use '-' instead of looking blank."""
    warnings = 0
    separator_re = re.compile(r'^:?-{3,}:?$')
    lines = content.splitlines()
    table = []
    start_line = 0

    def flush_table():
        nonlocal warnings, table, start_line
        seen_separator = False
        for offset, line in enumerate(table):
            cells = [cell.strip() for cell in line.strip().strip('|').split('|')]
            is_separator = bool(cells) and all(separator_re.match(cell or '') for cell in cells)
            if is_separator:
                seen_separator = True
                continue
            if not seen_separator:
                continue
            if cells and any(cells) and any(cell == '' for cell in cells):
                print(f"  [WARN] {name}:{start_line + offset} -> empty table data cells must use '-'")
                warnings += 1
        table = []

    for line_number, line in enumerate(lines, start=1):
        stripped = line.strip()
        if stripped.startswith('|') and stripped.endswith('|'):
            if not table:
                start_line = line_number
            table.append(line)
        else:
            if table:
                flush_table()
    if table:
        flush_table()

    return warnings

def _check_tg_bloat_sections(name, content):
    warnings = 0
    for heading in ['# Test Notes', '# Active Bugs']:
        if re.search(r'^' + re.escape(heading) + r'\s*$', content, re.MULTILINE):
            print(f"  [WARN] {name} -> remove {heading}; TGs should keep active tests and archive only")
            warnings += 1
    return warnings

def _check_inactive_bloat(name, content):
    """Finished TGs should omit placeholder sections instead of saying nothing is active."""
    warnings = 0
    active_match = re.search(r'^# Active Tests\s*\n(?P<body>.*?)(?=\n# |\Z)', content, re.MULTILINE | re.DOTALL)
    if active_match:
        body = active_match.group('body')
        if re.search(r'Nothing currently active|folded into the Archive|no active tests', body, re.IGNORECASE):
            print(f"  [WARN] {name} -> omit the Active Tests section when nothing is active")
            warnings += 1
    return warnings

def _check_status_block(name, content):
    """TG status blocks must stay short: Verdict, Progress bar, Last tested date, Jar."""
    warnings = 0

    status_match = re.search(r'^##[^\n]*Status\s*\n\n((?:\|.*\n)+)', content, re.MULTILINE)
    if not status_match:
        print(f"  [WARN] {name} -> missing Status block")
        return 1

    rows = [row.strip() for row in status_match.group(1).splitlines() if row.strip().startswith('|')]
    data_rows = []
    for row in rows:
        cells = [cell.strip() for cell in row.strip('|').split('|')]
        if len(cells) >= 2 and cells[0].startswith('**') and cells[0].endswith('**'):
            data_rows.append((cells[0].strip('*'), cells[1]))

    labels = [label for label, _ in data_rows]
    if labels != ['Verdict', 'Progress', 'Last tested', 'Jar']:
        print(f"  [WARN] {name} -> Status block rows must be exactly Verdict, Progress, Last tested, Jar")
        warnings += 1
        return warnings

    values = dict(data_rows)
    verdict = values['Verdict'].strip()
    if not verdict:
        print(f"  [WARN] {name} -> Verdict must be one useful sentence")
        warnings += 1
    elif verdict in STATUS_ONLY_VALUES:
        print(f"  [WARN] {name} -> Verdict must be descriptive, not just a one-word status")
        warnings += 1
    elif len(verdict) > 180:
        print(f"  [WARN] {name} -> Verdict is too long; keep it to one clear line")
        warnings += 1
    progress_match = PROGRESS_BAR_RE.match(values['Progress'])
    if not progress_match:
        print(f"  [WARN] {name} -> Progress must be exactly 10 bar emojis plus percentage, e.g. 🟩🟩🟩🟥🟥🟥🟥🟥🟥🟥 30%")
        warnings += 1
    else:
        pct = int(progress_match.group('pct'))
        expected_green = 10 if pct == 100 else pct // 10
        actual_green = progress_match.group('bar').count('🟩')
        if actual_green != expected_green:
            print(f"  [WARN] {name} -> Progress bar has {actual_green} green blocks but {pct}% requires {expected_green}")
            warnings += 1
    if not LAST_TESTED_RE.match(values['Last tested']):
        print(f"  [WARN] {name} -> Last tested must be a date only: YYYY-MM-DD")
        warnings += 1
    if not JAR_RE.match(values['Jar']):
        print(f"  [WARN] {name} -> Jar must be only a jar file name in backticks")
        warnings += 1
    return warnings

def _check_sections_table(name, content):
    warnings = 0
    sec_match = re.search(r'^##[^\n]*Sections\s*\n\n((?:\|.*\n)+)', content, re.MULTILINE)
    if not sec_match:
        print(f"  [WARN] {name} -> missing Sections table")
        return 1

    table_lines = [line.strip() for line in sec_match.group(1).splitlines() if line.strip().startswith('|')]
    if len(table_lines) < 2:
        print(f"  [WARN] {name} -> Sections table is incomplete")
        return 1

    header = [cell.strip() for cell in table_lines[0].strip('|').split('|')]
    if header != SECTION_HEADER:
        print(f"  [WARN] {name} -> Sections table columns must be exactly: § | Feature | Status | Flags")
        warnings += 1
        return warnings

    priorities = []
    for row in table_lines[2:]:
        cells = [cell.strip() for cell in row.strip('|').split('|')]
        if len(cells) != 4:
            print(f"  [WARN] {name} -> Sections table row must have exactly 4 cells: {row}")
            warnings += 1
            continue
        section_id, feature, status, flags = cells
        if flags == "":
            print(f"  [WARN] {name} §{section_id} -> empty Flags cells must use '-'")
            warnings += 1
        if status not in STATUS_VALUES:
            print(f"  [WARN] {name} §{section_id} -> section Status must be one of: Planned 📜, Designed ⏳, Built 🎯, Done ✅")
            warnings += 1
        if re.search(r'✅\s+`\d{4}-\d{2}-\d{2}`|\bPassed\b|confirmed in-game|Done ✅ on', feature + ' ' + status + ' ' + flags, re.IGNORECASE):
            print(f"  [WARN] {name} §{section_id} -> Sections table must not contain archive dates or passed prose")
            warnings += 1
        if any(sym in status for sym in ['💔', '‼️', '💤', '✏️', '🎨', '👎']):
            print(f"  [WARN] {name} §{section_id} -> flags belong in the Flags column, not Status")
            warnings += 1
        priorities.append(_section_sort_priority(status, flags))

    if priorities and priorities != sorted(priorities):
        print(f"  [WARN] {name} -> Sections table not priority-sorted (must be Built→flagged→Designed→Planned→Done→Parked→Scrapped)")
        warnings += 1
    return warnings

def _check_archive_folds(name, content):
    warnings = 0
    archive_match = re.search(
        r'^#\s+.*Archive\s*\n(?P<body>.*?)(?=\n---\s*\n\n<details><summary>🧨 <b>Cleanup</b></summary>|\n#\s+.*Active Bugs|\Z)',
        content,
        re.MULTILINE | re.DOTALL,
    )
    if not archive_match:
        print(f"  [WARN] {name} -> missing Archive section")
        return 1

    body = archive_match.group('body')
    headers = re.findall(r'<details><summary>(.*?)</summary>', body)
    if headers != ARCHIVE_FOLD_HEADERS:
        print(f"  [WARN] {name} -> Archive folds must be exactly: ✅ Confirmed, 💔 Regression, 📜 Planned, 💤 Parked, 👎 Scrapped")
        warnings += 1

    confirmed_match = re.search(
        r'<details><summary>✅ <b>Confirmed</b></summary>\s*(?P<body>.*?)(?=</details>)',
        body,
        re.DOTALL,
    )
    if confirmed_match:
        confirmed_body = confirmed_match.group('body')
        for line in confirmed_body.splitlines():
            stripped = line.strip()
            if not stripped or stripped == '*(none)*':
                continue
            if stripped.startswith('- ') and not CONFIRMED_ARCHIVE_RE.match(stripped):
                print(f"  [WARN] {name} -> Confirmed archive bullet must be: - §A Feature name — ✅ `YYYY-MM-DD`")
                warnings += 1
            elif re.search(r'\b(Passed|Done ✅ on|confirmed in-game)\b', stripped, re.IGNORECASE):
                print(f"  [WARN] {name} -> Confirmed archive uses old passed wording")
                warnings += 1
    return warnings

def _guide_paths_by_group():
    patterns = [
        os.path.join(DIR, 'Testing_Guide_[0-9][0-9]*.md'),
    ]
    grouped = {}
    for path in sorted(set(file for pattern in patterns for file in glob.glob(pattern))):
        match = re.search(r'Testing_Guide_(\d{2})', os.path.basename(path))
        if match:
            grouped.setdefault(match.group(1), []).append(path)
    return grouped


def _check_filename_lifecycle(name, content):
    warnings = 0
    lower_name = name.lower()
    progress = re.search(r'\|\s*\*\*Progress\*\*\s*\|\s*(.*?)\s*\|', content)
    section_rows = re.findall(r'\|\s*[^|]+\s*\|\s*[^|]+\s*\|\s*(Built 🎯|Designed ⏳|Planned 📜|Done ✅)\s*\|\s*([^|]+)\s*\|', content)
    active_match = re.search(r'^# Active Tests\s*\n(?P<body>.*?)(?=\n# |\Z)', content, re.MULTILINE | re.DOTALL)
    has_active_section = bool(active_match and re.search(r'^##\s+[A-Z0-9]+\s*[-·]', active_match.group('body'), re.MULTILINE))

    if lower_name.endswith('_done.md'):
        if not progress or '100%' not in progress.group(1):
            print(f"  [WARN] {name} -> _Done filename requires 100% progress")
            warnings += 1
        active_sections = [status for status, flags in section_rows if 'Parked 💤' not in flags and 'Scrapped 👎' not in flags]
        if active_sections and any(status != 'Done ✅' for status in active_sections):
            print(f"  [WARN] {name} -> _Done filename requires every in-scope section to be Done ✅")
            warnings += 1
        if has_active_section:
            print(f"  [WARN] {name} -> _Done filename cannot retain Active Tests")
            warnings += 1

    if lower_name.endswith('_scrapped.md') and has_active_section:
        print(f"  [WARN] {name} -> _Scrapped filename cannot retain Active Tests")
        warnings += 1
    return warnings


def health_check():
    grouped_paths = _guide_paths_by_group()
    files = []
    duplicate_errors = 0
    for group_id, candidates in sorted(grouped_paths.items()):
        if len(candidates) > 1:
            names = ', '.join(os.path.basename(path) for path in candidates)
            print(f"  [WARN] Group {group_id} -> duplicate Testing Guides: {names}")
            duplicate_errors += 1
        files.append(min(candidates, key=_guide_rank))
    groups_dir = os.path.normpath(os.path.join(DIR, "..", "groups"))
    errors = duplicate_errors
    print("\n🩺 Running Health Check...")

    if not os.path.isfile(GLOSSARY_PATH):
        print(f"  [WARN] Missing canonical glossary: {GLOSSARY_PATH}")
        errors += 1

    for f in sorted(glob.glob(os.path.join(DIR, "*.md"))):
        name = os.path.basename(f)
        if name.startswith("Testing_Guide_"):
            continue
        with open(f, 'r', encoding='utf-8') as file:
            errors += _check_empty_table_cells(name, file.read())

    for f in sorted(files):
        name = os.path.basename(f)
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
            lines = content.split('\n')

        errors += _check_filename_lifecycle(name, content)

        in_test_now = False
        for i, line in enumerate(lines):
            if "# 🎯 Test now" in line or line.strip() == "# Active Tests": in_test_now = True
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

        # Original Group link must resolve to a real file in docs/groups/
        link_match = re.search(r'Original Group:\*\*\s*\[[^\]]+\]\(([^)]+)\)', content)
        if link_match:
            link = link_match.group(1)
            resolved = os.path.normpath(os.path.join(DIR, link))
            if not os.path.isfile(resolved):
                print(f"  [WARN] {name} -> Original Group link broken: {link}")
                errors += 1
        else:
            print(f"  [WARN] {name} -> No Original Group link found")
            errors += 1

        # Status block must use the fixed Verdict / Progress / Last tested / Jar template.
        errors += _check_empty_table_cells(name, content)
        errors += _check_tg_bloat_sections(name, content)
        errors += _check_inactive_bloat(name, content)
        errors += _check_status_block(name, content)

        # Sections table must use the fixed § / Feature / Status / Flags template.
        errors += _check_sections_table(name, content)

        # Archive folds must use the fixed folding system.
        errors += _check_archive_folds(name, content)

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

        # Fully confirmed sections must be folded into Archive, not left in Active Tests.
        test_now_match = re.search(r'# (?:🎯 Test now|Active Tests)(.*?)(?=\n# 🗄️ Archive|\Z)', content, re.S)
        if test_now_match:
            for h in re.finditer(r'^##\s+\S\s*·[^\n]*$', test_now_match.group(1), re.MULTILINE):
                header = h.group(0)
                m2 = re.search(r'(\d+)/(\d+)', header)
                if '✅' in header and (not m2 or m2.group(1) == m2.group(2)):
                    print(f"  [WARN] {name} -> section fully confirmed but still under Active Tests: {header.strip()} (fold into Archive)")
                    errors += 1

        # Banned terminology (per Glossary.md)
        errors += _check_glossary_boundary(name, content)
        errors += _check_banned_terms(name, content)

    if errors == 0:
        print("✅ Health Check Passed! All guides look pristine.\n")
    else:
        print(f"❌ Found {errors} issues.\n")

def daily_snapshot():
    print("\nDaily snapshot files are retired to prevent duplicate status documents.")
    generate_dashboard()

def interactive_menu():
    if not msvcrt:
        print("Interactive menu requires msvcrt (Windows). Please use command line args.")
        return
        
    options = [
        ("Refresh Markdown Dashboard", generate_dashboard),
        ("Get Next Task (Give Me Work)", give_me_work),
        ("Run Health Check", health_check),
        ("Validate Completed Guide Lifecycle", archive_completed),
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
