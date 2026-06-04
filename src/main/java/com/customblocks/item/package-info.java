/**
 * The seven custom tool items that manipulate custom blocks on interaction:
 * Lumina Brush (glow), Amethyst Chisel (hardness/shape), Deleter (safe delete +
 * trash), Color Square (recolor), Color Triangle (precision recolor / enclosed
 * holes), Golden Hexagon (admin), Rectangle Tool (rectangular texturing).
 *
 * Design rule: never early-return on world.isClient in useOnBlock — gate on
 * ServerPlayerEntity instead, to avoid the client-side skip delay (§9.6).
 */
package com.customblocks.item;
