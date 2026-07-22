/**
 * Group 31 — BuzzerGame. A YouTube-style buzzer/stopwatch minigame built from CB-B blocks
 * (buzzer + timer stand) plus a session-owning wand. Ported + rewritten from the standalone reference
 * mod {@code Active_Projects/TimerChallenge} (package {@code com.timerchal}) into CB-B conventions.
 *
 * The round session is wand-owned (redesign 2026-07-18): {@link BuzzerSessionManager} holds one
 * in-memory {@link BuzzerSession} per host, created when the host uses the wand and destroyed on logout.
 * The old admin panel block + its Screen were scrapped. Remaining build items (stand resize/rotate/LED,
 * Duel/Party ranked reveal, sound/VFX, scoring) follow per docs/groups/GROUP_31_BUZZERGAME.md.
 */
package com.customblocks.buzzergame;
