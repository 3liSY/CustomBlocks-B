/**
 * Heart of the system. Contains SlotManager (the single source of truth for all
 * 1028 slot assignments), the immutable SlotData model and its .update() builder,
 * SlotDataStore (the ONLY class that reads/writes slot data to disk), plus the
 * undo/redo, snapshot, backup, trash, category, color-ecosystem, search, template,
 * and onboarding managers.
 *
 * Design rule: SlotData is always immutable; SlotManager is the only thing that
 * mutates assignments; all slot disk I/O goes through SlotDataStore.
 */
package com.customblocks.core;
