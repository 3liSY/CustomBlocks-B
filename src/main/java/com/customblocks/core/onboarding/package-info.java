/**
 * Group 23 — Player Experience: the screen-free onboarding + achievement engine.
 *
 * Everything in this package is server-side, self-contained logic/data with its own
 * JSON persistence under {@code config/customblocks/data/}. It deliberately does NOT
 * touch the create/retexture command handlers, the chest dashboard, or any client
 * screen — those wiring points are added separately so this layer can be built and
 * verified in isolation.
 *
 * Contents:
 *  - Achievement / Achievements / AchievementStore / AchievementManager — the milestone
 *    achievement engine (per-player tracking, persistence, unlock notification, progress).
 *  - TipPool       — rotating helpful tips for the dashboard Tip slot.
 *  - FirstUseHints — one-time contextual hints fired after specific commands.
 *
 * The tutorial screen and the achievements gallery screen are built in Group 27 (§G27.16);
 * this package only provides the data those screens read.
 */
package com.customblocks.core.onboarding;
