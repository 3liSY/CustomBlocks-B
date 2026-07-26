/**
 * Standalone image processing pipeline (the old project had one 94KB monolith).
 *   • ImageDownloader   — URL fetch with browser-like headers, WebP proxy, timeouts
 *   • ImageProcessor    — coordinator: download -> decode -> resize (bicubic) -> PNG
 *   • BackgroundRemover — auto (BgCascade decides) or none; no strength anywhere
 *   • ColorReplacer     — per-pixel color replacement for the hex change wizard
 */
package com.customblocks.image;
