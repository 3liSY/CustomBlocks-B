"""
gen_tool_textures.py — Group 06 tool textures.

- Shape tools (8): the old project's flat coloured square/triangle style. The
  green/yellow/black PNGs are copied verbatim from the old run capture; the two RED
  textures are reproduced with the OLD project's exact makeSquarePng/makeTrianglePng
  algorithm (no red PNG was captured), so all 8 match the original set.
- New tools (3): rainbow_rectangle, deleter (trash), omni_tool (a visual merge of the
  brush + chisel + rainbow-rectangle concepts) — drawn fresh.

Writes 16x16 RGBA PNGs into the new project's assets/customblocks/textures/item/.
"""
import os
import shutil
from PIL import Image

OLD = r"C:\Users\66664\OneDrive\Desktop\Coding\CustomBlocks\run\resourcepacks\customblocks_generated\assets\customblocks\textures\item"
DST = r"C:\Users\66664\OneDrive\Desktop\Coding\CustomBlocks-B\src\main\resources\assets\customblocks\textures\item"
os.makedirs(DST, exist_ok=True)

TRANSPARENT = (0, 0, 0, 0)


def lerp(a, b, t):
    return int(a + (b - a) * t)


def blank():
    return Image.new("RGBA", (16, 16), TRANSPARENT)


def setpx(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c)


# ── Old algorithm: flat coloured square (row=y, col=x) ────────────────────────
def make_square(r, g, b):
    img = blank()
    dark = (r // 4, g // 4, b // 4, 255)
    main = (r, g, b, 255)
    light = (lerp(r, 255, 0.55), lerp(g, 255, 0.55), lerp(b, 255, 0.55), 255)
    shade = (int(r * 0.65), int(g * 0.65), int(b * 0.65), 255)
    shine = (255, 255, 255, 255)
    for row in range(2, 14):
        for col in range(2, 14):
            setpx(img, col, row, main)
    for i in range(1, 15):
        setpx(img, i, 1, dark); setpx(img, i, 14, dark)
        setpx(img, 1, i, dark); setpx(img, 14, i, dark)
    for row in range(2, 5):
        for col in range(2, 13):
            setpx(img, col, row, light)
    for row in range(12, 14):
        for col in range(4, 14):
            setpx(img, col, row, shade)
    for row in range(4, 14):
        for col in range(12, 14):
            setpx(img, col, row, shade)
    setpx(img, 2, 2, shine); setpx(img, 3, 2, shine); setpx(img, 2, 3, shine)
    return img


# ── Old algorithm: outlined upward triangle ───────────────────────────────────
def make_triangle(r, g, b):
    img = blank()
    dark = (r // 4, g // 4, b // 4, 255)
    main = (r, g, b, 255)
    light = (lerp(r, 255, 0.55), lerp(g, 255, 0.55), lerp(b, 255, 0.55), 255)
    shine = (255, 255, 255, 255)
    apex_cx, apex_row, base_row, base_hw = 7.5, 1, 14, 6.5

    def hw(row):
        t = (row - apex_row) / (base_row - apex_row)
        return t * base_hw

    for row in range(apex_row, base_row + 1):
        left = round(apex_cx - hw(row)); right = round(apex_cx + hw(row))
        for col in range(left, right + 1):
            setpx(img, col, row, main)
    for row in range(apex_row, base_row + 1):
        left = round(apex_cx - hw(row)); right = round(apex_cx + hw(row))
        setpx(img, left, row, dark); setpx(img, right, row, dark)
    setpx(img, 7, apex_row, dark); setpx(img, 8, apex_row, dark)
    for col in range(1, 15):
        if img.getpixel((col, base_row)) != TRANSPARENT:
            setpx(img, col, base_row, dark)
    for row in range(apex_row + 1, apex_row + 5):
        left = round(apex_cx - hw(row)) + 1; right = round(apex_cx + hw(row)) - 1
        for col in range(left, right + 1):
            if img.getpixel((col, row)) == main:
                setpx(img, col, row, light)
    if img.getpixel((7, 3)) == light:
        setpx(img, 7, 3, shine)
    if img.getpixel((8, 3)) == light:
        setpx(img, 8, 3, shine)
    return img


# ── New: rainbow rectangle (horizontal rainbow bands in a bordered box) ───────
def make_rainbow_rectangle():
    img = blank()
    bands = [
        (228, 52, 52), (236, 140, 40), (242, 214, 48),
        (66, 186, 66), (54, 132, 226), (110, 78, 206),
    ]
    x0, x1, y0, y1 = 2, 13, 3, 12          # box bounds (inclusive)
    border = (28, 28, 34, 255)
    band_h = (y1 - 1 - (y0 + 1) + 1) / len(bands)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (y0, y1)
            if edge:
                setpx(img, x, y, border)
            else:
                idx = min(len(bands) - 1, int((y - (y0 + 1)) / band_h))
                r, g, b = bands[idx]
                setpx(img, x, y, (r, g, b, 255))
    # white shine on the top band
    setpx(img, x0 + 1, y0 + 1, (255, 255, 255, 230))
    setpx(img, x0 + 2, y0 + 1, (255, 255, 255, 160))
    return img


# ── New: deleter (trash bin) ──────────────────────────────────────────────────
def make_deleter():
    img = blank()
    body = (120, 126, 132, 255)
    bodyd = (78, 84, 90, 255)
    lid = (150, 156, 162, 255)
    outline = (38, 40, 44, 255)
    # handle on the lid
    for x in range(6, 10):
        setpx(img, x, 2, outline)
    setpx(img, 6, 3, outline); setpx(img, 9, 3, outline)
    # lid (rows 3-4, cols 4-11)
    for x in range(4, 12):
        setpx(img, x, 3, lid); setpx(img, x, 4, lid)
    for x in range(4, 12):
        setpx(img, x, 3, outline if x in (4, 11) else lid)
    setpx(img, 3, 4, outline); setpx(img, 12, 4, outline)
    # bin body (rows 5-14, slightly tapered)
    for y in range(5, 15):
        left = 5 + (1 if y >= 12 else 0)
        right = 10 - (1 if y >= 12 else 0)
        for x in range(left, right + 1):
            setpx(img, x, y, body)
        setpx(img, left, y, outline); setpx(img, right, y, outline)
    for x in range(5, 11):
        setpx(img, x, 14, outline)
    # vertical ridge lines (garbage-can grooves)
    for y in range(6, 13):
        setpx(img, 6, y, bodyd); setpx(img, 8, y, bodyd); setpx(img, 9, y, bodyd)
    return img


# ── New: omni-tool (chisel blade + brush bristles + rainbow base) ─────────────
def make_omni():
    img = blank()
    outline = (32, 32, 38, 255)
    steel = (176, 182, 190, 255)
    steeld = (120, 126, 134, 255)
    wood = (150, 104, 56, 255)
    woodd = (110, 74, 38, 255)
    bristle = (236, 206, 120, 255)      # brush tip
    rainbow = [(228, 52, 52), (242, 214, 48), (66, 186, 66), (54, 132, 226), (140, 80, 210)]

    # Chisel blade — steel diamond at the top
    for y in range(1, 5):
        for x in range(6, 10):
            setpx(img, x, y, steel)
    for x in range(6, 10):
        setpx(img, x, 1, outline)
    setpx(img, 6, 2, steeld); setpx(img, 6, 3, steeld); setpx(img, 6, 4, steeld)
    setpx(img, 5, 4, outline); setpx(img, 10, 4, outline)

    # Wooden shaft down the middle
    for y in range(5, 11):
        setpx(img, 7, y, wood); setpx(img, 8, y, wood)
        setpx(img, 7, y, woodd if y % 2 else wood)
        setpx(img, 6, y, outline); setpx(img, 9, y, outline)

    # Brush bristles flaring out mid-shaft (right side)
    for y in range(6, 10):
        setpx(img, 10, y, bristle); setpx(img, 11, y, bristle)
    setpx(img, 12, 7, bristle); setpx(img, 12, 8, bristle)
    setpx(img, 11, 6, outline); setpx(img, 12, 6, outline)
    setpx(img, 12, 9, outline); setpx(img, 13, 8, outline)

    # Rainbow rectangle base (bottom)
    for i, c in enumerate(rainbow):
        y = 11 + (i // 3)
        x = 5 + (i % 3) * 2
        setpx(img, x, y, (*c, 255)); setpx(img, x + 1, y, (*c, 255))
    for x in range(4, 12):
        setpx(img, x, 14, outline)
    setpx(img, 4, 13, outline); setpx(img, 11, 13, outline)
    return img


def save(img, name):
    img.save(os.path.join(DST, name + ".png"))
    print("wrote", name + ".png")


# ── Shape tools ───────────────────────────────────────────────────────────────
COLORS = {"black": (10, 10, 10), "yellow": (240, 200, 20),
          "green": (30, 140, 30), "red": (238, 51, 51)}

for color in ("green", "yellow", "black"):
    for shape in ("square", "triangle"):
        name = f"{color}_{shape}.png"
        src = os.path.join(OLD, name)
        if os.path.exists(src):
            shutil.copyfile(src, os.path.join(DST, name))
            print("copied", name, "(verbatim from old project)")
        else:
            print("MISSING old", name)

r, g, b = COLORS["red"]
save(make_square(r, g, b), "red_square")
save(make_triangle(r, g, b), "red_triangle")

# ── New tools ─────────────────────────────────────────────────────────────────
save(make_rainbow_rectangle(), "rainbow_rectangle")
save(make_deleter(), "deleter")
save(make_omni(), "omni_tool")

print("done")
