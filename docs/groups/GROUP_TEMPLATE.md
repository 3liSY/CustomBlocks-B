# Group XX - [Feature Name]

> [One clear sentence saying what this Group owns for the player.]

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Group_XX_Testing_Guide.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

[Two or three short paragraphs: the problem this Group solves, the player outcome, and why this work belongs together.]

## Ownership

| Owns | Does not own |
| --- | --- |
| [Commands, screens, systems, or behavior owned here.] | [Nearest related Group and what it owns instead.] |

Add rows until every boundary that could cause duplicate work is clear.

## Direction

[A short, plain-language description of the intended product behavior. This is not a testing verdict and must not include progress, test dates, or confirmation claims.]

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| YYYY-MM-DD | [Decision in clear language.] | [What future work must now do or avoid.] |

Keep only decisions that still control the design. Move replaced decisions to the folded history below.

## Feature Plan

### A. [Feature Area]

**Player outcome**

[What a player can do or understand when this area is complete.]

**Experience**

- [Visible behavior, wording, layout, interaction, or feedback.]
- [Important edge case or accessibility expectation.]

**Requirements**

- [Required command, screen, data behavior, or contract.]
- [Required validation, security, performance, or multiplayer behavior.]

**Boundary**

[What this area intentionally does not do, and where that work belongs instead.]

### B. [Feature Area]

Repeat the same four parts for each independently owned feature area. Use short tables only when they make a specification easier to compare.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| GXX | [Consumes, provides, routes to, or shares a boundary with this Group.] | [Exactly what must stay compatible.] |

Use `-` in each cell when no cross-group contract exists.

## Technical Contract

- [Enduring implementation constraint that future work must preserve.]
- [Data, packet, rendering, command, or performance rule with real design impact.]
- [Link to the code, ADR, or reference only when it explains a stable constraint.]

Do not put temporary build notes, test results, or source-only progress claims here.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| [Idea] | [Why it is not current work.] | [Group or future owner.] |

</details>

Use `*(none)*` inside the fold when there is no deferred scope.

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| YYYY-MM-DD | [Old direction.] | [Current decision or Group link.] |

</details>

Use `*(none)*` inside the fold when no historical decision needs preserving.

## References

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Group_XX_Testing_Guide.md) · [All Groups](README.md)

- [Relevant ADR or architectural reference.]
- [Relevant historical record, used only as dated background.]
- [Related Group document.]
