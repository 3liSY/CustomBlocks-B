CustomBlocks — white-ring fix tester
=====================================

WHAT THIS IS
  Bakes any image through the same block pipeline the mod uses, twice:
    OLD = the single-pass edge cleanup that is in-game now (leaves the thin white ring)
    NEW = the gradient anti-fringe peel (removes the ring, keeps the art)
  So you can eyeball the difference on YOUR OWN sample images before it ships.

HOW TO USE
  1. Drop test images into the  samples  folder  (.jpg .png .gif .bmp .webp).
  2. Double-click  run_test.bat .
  3. Type a tolerance (0-100) or just press Enter for 50.
  4. The  out  folder opens. For each image you get:
        NAME_OLD.png      - old result
        NAME_NEW.png      - new result (the fix)
        NAME_COMPARE.png  - the two side by side (LEFT = old, RIGHT = new)

ALREADY IN out  (the moon example):
    compare_full.png  - moon old vs new, full block
    removed_map.png   - red = exactly which pixels the fix removed (the halo ring only)
    zoom_old.png / zoom_new.png - top edge, zoomed 4x

NOTES
  - This is an OFFLINE preview only. It does NOT change the mod. The real fix lives in the
    mod source and still has to be built + tested in-game to be "done".
  - Run the same image at a few tolerances to see how each behaves.
  - GIFs: only the first frame is previewed here.
