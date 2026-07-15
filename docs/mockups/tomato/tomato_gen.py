"""Photoreal tomato item texture generator.

Renders a lit sphere with the things that actually make a tomato read as a tomato in a photo:
  - subsurface scatter (light bleeding through thin skin at the terminator)
  - a waxy, tight specular highlight + a broad soft sheen
  - vertical lobing (tomatoes are not billiard balls)
  - freckling / micro colour variation in the skin
  - a green calyx with distinct leaves and a stem
  - rim darkening so it doesn't look like plastic

Outputs 128x128 (item) and 256x256 (entity) with a clean alpha cut-out.
"""
import math, random
from PIL import Image, ImageFilter

def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))

def clamp(v, lo=0.0, hi=1.0):
    return max(lo, min(hi, v))

def smooth(e0, e1, x):
    t = clamp((x - e0) / (e1 - e0))
    return t * t * (3 - 2 * t)

GRADES = {
    # deep shadow          mid body            lit face             hot rim / subsurface
    "A": dict(shadow=(78, 12, 14),  body=(186, 26, 24),  lit=(238, 74, 44),  sss=(255, 128, 66),
              leaf=(86, 124, 46),   name="Beefsteak",  lobes=1.0,  gloss=1.0,  freckle=0.14,
              squash=0.86, leaves=6),
    "B": dict(shadow=(60, 8, 12),   body=(158, 18, 20),  lit=(210, 52, 34),  sss=(236, 100, 54),
              leaf=(66, 100, 38),   name="Overripe",   lobes=1.25, gloss=0.78, freckle=0.24,
              squash=0.82, leaves=6),
    "C": dict(shadow=(92, 16, 18),  body=(204, 30, 26),  lit=(252, 92, 52),  sss=(255, 150, 88),
              leaf=(102, 146, 56),  name="Vine-ripe",  lobes=0.7,  gloss=1.25, freckle=0.09,
              squash=0.90, leaves=5),
}

def render(size, g, seed=7):
    rnd = random.Random(seed)
    ss = 4                      # supersample, then box-down — gives clean edges + real antialiasing
    S = size * ss
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = img.load()

    # Fill the slot: the fruit runs nearly edge-to-edge, leaving only enough margin for the calyx
    # to sit inside the frame. A tomato that floats in the middle of its own icon looks like a berry.
    cx, cy = S * 0.5, S * 0.585
    R = S * 0.50
    SQ = g["squash"]            # a tomato is WIDER than tall — this is what stops it reading as an apple
    # light from upper-left, slightly toward the viewer
    L = (-0.52, -0.62, 0.59)
    Ln = math.sqrt(sum(c * c for c in L))
    L = tuple(c / Ln for c in L)

    # freckle field: cheap value noise, sampled per pixel
    def freckle(x, y):
        n = math.sin(x * 0.31 + y * 0.17) * 43758.5453
        n += math.sin(x * 0.11 - y * 0.43) * 12345.6789
        return (n - math.floor(n))

    for y in range(S):
        for x in range(S):
            dx = (x - cx) / R
            dy = (y - cy) / (R * SQ)

            # lobing: squeeze the radius periodically around the vertical axis
            ang = math.atan2(dy, dx)
            lobe = 1.0 + 0.034 * g["lobes"] * math.cos(ang * 5.0)
            # shoulder dimple — the top caves in slightly where the calyx sits
            dimple = 1.0 - 0.10 * smooth(0.55, 1.0, -dy) * smooth(0.60, 0.0, abs(dx))
            d = math.sqrt(dx * dx + dy * dy) / (lobe * dimple)
            if d > 1.0:
                continue

            nz = math.sqrt(max(0.0, 1.0 - d * d))
            nx, ny = dx, dy

            lam = clamp(nx * L[0] + ny * L[1] + nz * L[2])

            # base ramp: shadow -> body -> lit
            if lam < 0.45:
                col = lerp(g["shadow"], g["body"], smooth(0.0, 0.45, lam))
            else:
                col = lerp(g["body"], g["lit"], smooth(0.45, 1.0, lam))

            # subsurface: skin glows where light grazes through it (the terminator + lower rim)
            sss = smooth(0.55, 0.0, lam) * smooth(0.55, 1.0, d) * 0.55
            col = lerp(col, g["sss"], sss)

            # freckling — tiny skin variation, strongest in the mid-tones
            f = (freckle(x / ss, y / ss) - 0.5) * 2.0
            amt = g["freckle"] * (1.0 - abs(lam - 0.55) * 1.2)
            col = tuple(clamp(c + f * 16 * amt, 0, 255) for c in col)

            # tight waxy specular
            h = ((L[0] + 0) * 0.5, (L[1] + 0) * 0.5, (L[2] + 1) * 0.5)
            hn = math.sqrt(sum(c * c for c in h))
            h = tuple(c / hn for c in h)
            spec = clamp(nx * h[0] + ny * h[1] + nz * h[2])
            hot = (spec ** 42) * 1.15 * g["gloss"]
            sheen = (spec ** 6) * 0.10 * g["gloss"]
            col = tuple(clamp(c + (255 - c) * clamp(hot + sheen), 0, 255) for c in col)

            # rim darkening — the edge of a real tomato falls off hard
            rim = smooth(0.80, 1.0, d)
            col = tuple(c * (1.0 - 0.42 * rim) for c in col)

            px[x, y] = (int(col[0]), int(col[1]), int(col[2]), 255)

    # ── calyx: a WIDE flat star lying over the shoulder, seen at a shallow angle ──
    # The leaves radiate outward from the stem and hug the curve of the fruit — they do NOT stand up
    # like an apple's. Foreshortened vertically (yc) because we're looking at the top from the side.
    leaf = g["leaf"]
    stem = tuple(int(c * 0.55) for c in leaf)
    top = (cx, cy - R * SQ * 0.78)
    N = g["leaves"]

    for i in range(N):
        a = (i / N) * math.tau + rnd.uniform(-0.10, 0.10)
        ln = R * rnd.uniform(0.52, 0.68)
        for t in range(int(ln)):
            tt = t / ln
            w = (1.0 - tt) ** 0.75 * R * 0.10        # tapers to a point
            lx = top[0] + math.cos(a) * t
            # vertical foreshortening + droop: leaves fall away over the shoulder
            ly = top[1] + math.sin(a) * t * 0.42 + tt * tt * R * 0.22
            # a leaf pointing away from the light is darker; one pointing into it catches highlight
            face = clamp(0.5 + 0.5 * (math.cos(a) * L[0] + math.sin(a) * 0.4 * L[1]))
            shade = 0.62 + 0.55 * face * (1.0 - tt * 0.45)
            for oy in range(int(-w) - 1, int(w) + 2):
                for ox in range(int(-w) - 1, int(w) + 2):
                    if ox * ox + oy * oy > w * w:
                        continue
                    X, Y = int(lx + ox), int(ly + oy)
                    if 0 <= X < S and 0 <= Y < S and px[X, Y][3] > 0:
                        c = tuple(int(clamp(v * shade, 0, 255)) for v in leaf)
                        px[X, Y] = (c[0], c[1], c[2], 255)

    # stem: a SHORT thick nub, not a stalk
    sr = R * 0.085
    for t in range(int(R * 0.16)):
        X0, Y0 = top[0], top[1] - t
        r = sr * (1.0 + 0.25 * (t / max(1, R * 0.16)))   # flares slightly at the cut end
        for oy in range(int(-r) - 1, int(r) + 2):
            for ox in range(int(-r) - 1, int(r) + 2):
                if ox * ox + oy * oy > r * r:
                    continue
                X, Y = int(X0 + ox), int(Y0 + oy)
                if 0 <= X < S and 0 <= Y < S:
                    lit = 1.0 + 0.35 * clamp(-ox / max(1.0, r))   # lit on the light-facing side
                    c = tuple(int(clamp(v * lit, 0, 255)) for v in stem)
                    px[X, Y] = (c[0], c[1], c[2], 255)

    return img.resize((size, size), Image.LANCZOS)


if __name__ == "__main__":
    import base64, io, json, sys
    out = {}
    for key, g in GRADES.items():
        for size in (128, 256):
            im = render(size, g, seed=11 + ord(key))
            path = f"tomato_{key}_{size}.png"
            im.save(path)
            if size == 128:
                buf = io.BytesIO()
                im.save(buf, "PNG")
                out[key] = dict(name=g["name"],
                                b64=base64.b64encode(buf.getvalue()).decode())
        print(f"{key} {g['name']}: 128 + 256 written")
    with open("tomato_preview.json", "w") as f:
        json.dump(out, f)
    print("preview json written")
