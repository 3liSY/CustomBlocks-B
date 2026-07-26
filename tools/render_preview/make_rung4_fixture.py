"""make_rung4_fixture.py — builds the picture that exercises cascade rung 4 (G10 §H).

Rungs 1-3 are more certain and take every baseline picture, so the estimator ensemble never runs on
real content and its mask would go unseen. This generates the case that reaches it, and the reason it
reaches it is the point of the fixture:

  * no alpha channel               -> rung 1 has nothing to read
  * grainy background              -> no side is a majority of ONE flat tone, so rung 2 cannot key it
  * grain breaks colour adjacency  -> the background shatters into many small regions, none of which
                                      wraps the frame, so rung 3 finds nothing to separate
  * distance-from-border histogram -> still cleanly two humps, which is precisely what rung 4 measures
    stays strongly bimodal

That combination is not artificial: a scanned or heavily re-compressed photograph behaves the same way.

Committed as a script rather than as a loose PNG so the fixture has provenance and can be rebuilt.

Usage: python tools/render_preview/make_rung4_fixture.py
Writes: tools/render_preview/bg_rung4/grainy_subject.png
"""

import os
import random

from PIL import Image, ImageDraw

W = H = 400
BG = (past := 96, 120, 168)          # a mid blue background
SUBJECT = (232, 196, 64)             # a warm yellow subject, far from BG in colour
GRAIN = 26                           # +/- per-channel noise amplitude, enough to break JND adjacency

random.seed(20260726)                # fixed seed: the fixture must be reproducible


def main() -> None:
    img = Image.new("RGB", (W, H), BG)
    px = img.load()

    # Grainy background.
    for y in range(H):
        for x in range(W):
            r, g, b = BG
            px[x, y] = (
                max(0, min(255, r + random.randint(-GRAIN, GRAIN))),
                max(0, min(255, g + random.randint(-GRAIN, GRAIN))),
                max(0, min(255, b + random.randint(-GRAIN, GRAIN))),
            )

    # A solid subject with a hole in it, so pocket absorption is exercised too.
    d = ImageDraw.Draw(img)
    d.ellipse((90, 70, 310, 330), fill=SUBJECT)
    d.ellipse((160, 150, 240, 250), fill=BG)      # enclosed pocket, clean (not grainy)

    out_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "bg_rung4")
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.join(out_dir, "grainy_subject.png")
    img.save(out)
    print(f"wrote {out} ({W}x{H})")


if __name__ == "__main__":
    main()
