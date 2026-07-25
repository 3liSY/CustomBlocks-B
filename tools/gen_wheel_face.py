"""gen_wheel_face.py - Group 34 (Wheel of Fortune): bake the wheel face texture.

Writes  src/main/resources/assets/customblocks/textures/block/wheel_face.png
        (RGBA, TEX x TEX, transparent outside the disc)

WHY A TEXTURE AND NOT QUADS
---------------------------
The wheel face used to be ~850 TEXT_DISPLAY colour-quads, one strip stack per slice, each slice's
overhang meant to be painted over by the next slice's exact edge. That design is only correct if the
paint ORDER is guaranteed - which a Python mockup can do and Minecraft cannot: the strips are
translucent text-background quads separated by 0.002 blocks of depth, and that ordering does not hold
in game. The overhangs surfaced as stair-stepped seams (rejected in game three times, 2026-07-24).

A raster has no ordering problem at all. Every pixel belongs to exactly one slice by construction, so
there is nothing to overlap, nothing to z-fight, and the wedge boundary is a true straight ray.

HOW THE PIXELS ARE DECIDED
--------------------------
Colour is assigned to EVERY pixel of the square, inside the disc or not, purely from its angle:

    slice = floor(angle / SLICE_DEG + 0.5) mod SLICES

which is character-for-character the rule WheelRing.sliceAt uses to turn the arrow's landing angle
into a winner. So the colour under the arrow IS the slice the code awards, by construction.

Alpha is the only thing the radius decides: opaque inside OUTER_R, clear outside. Colouring the
outside too (instead of leaving it black) is what keeps the rim clean - when the supersamples are
averaged down, a half-covered rim texel blends the RIGHT colour at half alpha instead of fading to
black.

Anti-aliasing is SS x SS box supersampling. Every output texel is the mean of SS^2 samples, so a
wedge boundary lands as a one-texel colour ramp rather than a step. At TEX=1024 over a 20-block
wheel that ramp is ~2 cm wide in world units.

Run:  python tools/gen_wheel_face.py
"""
import os

import numpy as np
from PIL import Image

# ---------------------------------------------------------------- geometry (MUST match WheelRing.java)
SLICES = 50                       # WheelRing.SLICES
SLICE_DEG = 360.0 / SLICES        # WheelRing.SLICE_DEG

# ---------------------------------------------------------------- texture layout (MUST match WheelRing.FACE_*)
TEX = 1024                        # WheelRing.FACE_TEX_PX  - one texel is ~2 cm on the placed wheel
MARGIN = 8                        # WheelRing.FACE_MARGIN_PX - clear border so the rim's anti-aliased
                                  # edge is never clipped, and atlas mip bleed cannot reach the disc
SS = 8                            # supersamples per texel per axis (64 samples -> 65 blend levels)

RADIUS = TEX / 2.0 - MARGIN       # disc radius in texels; the Java side scales this back up to OUTER_R

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "..", "src", "main", "resources", "assets", "customblocks",
                   "textures", "block", "wheel_face.png")


def hsv_to_rgb(h):
    """S=1, V=1 pure saturated colour for hue h in degrees - the same branch table as
    WheelRing.hsvToRgb, kept identical so the baked face matches the documented palette."""
    c, x = 1.0, 1.0 - abs(((h / 60.0) % 2.0) - 1.0)
    if h < 60:    r, g, b = c, x, 0.0
    elif h < 120: r, g, b = x, c, 0.0
    elif h < 180: r, g, b = 0.0, c, x
    elif h < 240: r, g, b = 0.0, x, c
    elif h < 300: r, g, b = x, 0.0, c
    else:         r, g, b = c, 0.0, x
    return (round(r * 255), round(g * 255), round(b * 255))


# 50 unique fully-saturated colours. Hues step by the golden angle (~137.508 deg) so neighbouring
# slices are always far apart on the colour wheel - vivid contrast, never a muddy seam.
PALETTE = np.array([hsv_to_rgb((i * 137.508) % 360.0) for i in range(SLICES)], dtype=np.float32)


def bake():
    n = TEX * SS
    centre = n / 2.0
    radius_ss = RADIUS * SS
    out = np.empty((TEX, TEX, 4), dtype=np.uint8)

    # Sample x is the same for every row, so build it once.
    xs = (np.arange(n, dtype=np.float32) + 0.5) - centre

    # One block of output rows at a time: the full 8192^2 sample grid would be 200 MB+ per array.
    for row in range(TEX):
        ys = centre - ((np.arange(row * SS, (row + 1) * SS, dtype=np.float32)) + 0.5)
        yy = ys[:, None]                                     # (SS, 1)
        xx = xs[None, :]                                     # (1, n)

        deg = np.degrees(np.arctan2(yy, xx)) % 360.0
        # floor(x + 0.5) is Java's Math.round - NOT numpy's round(), which breaks ties to even.
        idx = np.floor(deg / SLICE_DEG + 0.5).astype(np.int32) % SLICES

        rgb = PALETTE[idx]                                   # (SS, n, 3) - coloured everywhere
        alpha = np.where(xx * xx + yy * yy <= radius_ss * radius_ss, 255.0, 0.0).astype(np.float32)

        # Box-average the SS x SS samples behind each output texel.
        rgb = rgb.reshape(SS, TEX, SS, 3).mean(axis=(0, 2))
        alpha = alpha.reshape(SS, TEX, SS).mean(axis=(0, 2))

        out[row, :, :3] = np.rint(rgb).astype(np.uint8)
        out[row, :, 3] = np.rint(alpha).astype(np.uint8)

    return out


def main():
    px = bake()
    path = os.path.normpath(OUT)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(px, "RGBA").save(path)

    opaque = int((px[:, :, 3] == 255).sum())
    edge = int(((px[:, :, 3] > 0) & (px[:, :, 3] < 255)).sum())
    print(f"wrote {path}  {TEX}x{TEX}  {SLICES} slices  disc r={RADIUS}px  {SS}x{SS} supersampled")
    print(f"  fully opaque texels {opaque}  anti-aliased rim texels {edge}  "
          f"clear {TEX * TEX - opaque - edge}")


if __name__ == "__main__":
    main()
