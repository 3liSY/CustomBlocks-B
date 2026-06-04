/**
 * Arabic letter and word block system.
 *   • ArabicLetterMap     — letter -> Unicode data + joining rules
 *   • ArabicBlockRegistry — maps (letter, color) -> block id, persisted to JSON
 *   • ArabicWordRenderer  — Java2D Arabic text rendering with bundled arabtype.ttf
 *
 * Auto-joining detects neighbouring letter blocks and swaps to the correct
 * initial/medial/final/isolated form.
 */
package com.customblocks.arabic;
