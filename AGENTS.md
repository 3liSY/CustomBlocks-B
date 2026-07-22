# CustomBlocks-B Agent Rules

Read [CLAUDE.md](CLAUDE.md) at the start of every task. It is the workspace workflow contract.

For documentation work, read [the canonical glossary](docs/testing/extra/Glossary.md) before making a decision. Do not copy its rules into individual documents and do not invent a competing status, emoji, archive, or filename system.

Before editing a feature, read its Group document and its one canonical Testing Guide. After editing a Testing Guide, run:

```powershell
python docs/testing/extra/testing_tools.py health
```

Treat warnings as unfinished work. The dashboard is navigation only; the Group document and Testing Guide own current truth.

Group documents follow [GROUP_TEMPLATE.md](docs/groups/GROUP_TEMPLATE.md). Keep testing verdicts, progress, test dates, rows, and owner confirmation in the Testing Guide only.

After changing a Group document, run `python docs/groups/group_tools.py check <group-document>`. Preserve every still-relevant decision and specification during migration; never make a second Group plan or silently discard detail.
