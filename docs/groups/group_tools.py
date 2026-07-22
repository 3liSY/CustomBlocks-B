"""Validation helpers for current Group documents.

Groups are design and ownership documents. Testing Guides own status and results.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


GROUPS_DIR = Path(__file__).resolve().parent
TESTING_DIR = GROUPS_DIR.parent / "testing"
REQUIRED_HEADINGS = (
    "Purpose",
    "Ownership",
    "Direction",
    "Locked Decisions",
    "Feature Plan",
    "Cross-Group Contracts",
    "Technical Contract",
    "Deferred Scope",
    "Superseded Decisions",
    "References",
)
BANNED_PATTERNS = (
    (r"^## Status\s*$", "Status section belongs in the Testing Guide"),
    (r"\*\*Verdict\*\*", "Verdict belongs in the Testing Guide"),
    (r"\*\*Progress\*\*", "Progress belongs in the Testing Guide"),
    (r"\*\*Last tested\*\*", "Last tested belongs in the Testing Guide"),
    (r"^# Active Tests\s*$", "Active Tests belong in the Testing Guide"),
    (r"^## Current Truth\s*$", "Use Direction and Feature Plan instead of Current Truth"),
)
NAV_LINKS = (
    ("Dashboard", "../testing/Dashboard.md"),
    ("Testing Guide", None),
    ("All Groups", "README.md"),
)
NAV_ANCHORS = (
    "direction",
    "locked-decisions",
    "feature-plan",
    "cross-group-contracts",
    "superseded-decisions",
)


def group_files() -> list[Path]:
    return sorted(
        path
        for path in GROUPS_DIR.glob("GROUP_[0-9][0-9]_*.md")
        if path.name != "GROUP_TEMPLATE.md"
    )


def _heading_present(content: str, heading: str) -> bool:
    return bool(re.search(rf"^## {re.escape(heading)}\s*$", content, re.MULTILINE))


def _testing_link(content: str, path: Path) -> str | None:
    match = re.search(r"\[Testing Guide\]\(([^)]+)\)", content)
    if not match:
        return "missing Testing Guide link"
    target = (path.parent / match.group(1)).resolve()
    if not target.is_file() or TESTING_DIR not in target.parents:
        return f"Testing Guide link does not resolve: {match.group(1)}"
    return None


def check_group(path: Path) -> list[str]:
    content = path.read_text(encoding="utf-8")
    issues: list[str] = []

    if not re.search(r"^# Group \d{2} - .+", content, re.MULTILINE):
        issues.append("title must use '# Group XX - Feature Name'")

    for heading in REQUIRED_HEADINGS:
        if not _heading_present(content, heading):
            issues.append(f"missing required heading: {heading}")

    for label, target in NAV_LINKS:
        if target is None:
            continue
        if f"[{label}]({target})" not in content:
            issues.append(f"missing top navigation link: {label}")
    testing_link_issue = _testing_link(content, path)
    if testing_link_issue:
        issues.append(testing_link_issue)

    for anchor in NAV_ANCHORS:
        if f"](#{anchor})" not in content:
            issues.append(f"missing top navigation anchor: #{anchor}")

    for pattern, message in BANNED_PATTERNS:
        if re.search(pattern, content, re.MULTILINE):
            issues.append(message)

    return issues


def report(paths: list[Path], strict: bool) -> int:
    failures = 0
    for path in paths:
        issues = check_group(path)
        if not issues:
            print(f"PASS {path.name}")
            continue
        failures += 1
        print(f"MIGRATE {path.name}")
        for issue in issues:
            print(f"  - {issue}")

    if failures:
        mode = "FAILED" if strict else "needs migration"
        print(f"\nGroup document check: {mode} ({failures}/{len(paths)} documents).")
        return 1 if strict else 0

    print(f"\nGroup document check: passed ({len(paths)} documents).")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate current Group documents.")
    parser.add_argument("command", choices=("audit", "check", "health"))
    parser.add_argument("path", nargs="?", help="Group document path for check")
    args = parser.parse_args()

    if args.command == "check":
        if not args.path:
            parser.error("check requires a Group document path")
        path = Path(args.path).resolve()
        issues = check_group(path)
        if issues:
            print(f"FAILED {path.name}")
            for issue in issues:
                print(f"  - {issue}")
            return 1
        print(f"PASS {path.name}")
        return 0

    return report(group_files(), strict=args.command == "health")


if __name__ == "__main__":
    sys.exit(main())
