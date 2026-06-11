/**
 * The custom tool items that manipulate custom blocks on interaction:
 * Omni-Tool (multi-mode: glow, hardness, area, …), Lumina Brush + Amethyst Chisel
 * (legacy, merged into the Omni-Tool), Deleter (definition delete, undoable),
 * Colour Triangles (create a colour variant of a block), Colour Squares (swap a
 * placed block to an existing colour variant), Rainbow Rectangle (area).
 *
 * Design rule: never early-return on world.isClient in useOnBlock — gate on
 * ServerPlayerEntity instead, to avoid the client-side skip delay (§9.6).
 */
package com.customblocks.item;
