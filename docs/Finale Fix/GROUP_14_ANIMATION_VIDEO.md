# Group 14 — Animation System & Video Studio

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 13 (AI + Video + Macros) build-verified.
>
> **Objective:** Restore the animated block editor (GIF and pre-sliced PNG support) with an improved implementation free of the old bugs. Wire the existing `jcodec` video extractor into a Video Studio chest GUI.
>
> **Source issues:** P1 (AnimBlockScreen — Issue 17.16), Q6 (Video Studio — `/cb video`)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores / adds

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| AnimBlockScreen | Old system existed but had bugs (playback issues, frame sizing) | `AnimBlockScreen` class missing — `client/gui/` empty | Rebuilt from scratch with bug fixes |
| GIF import | Upload a GIF URL → animated block | Missing | Restored |
| Pre-sliced PNG | Upload a sprite sheet PNG → animated block | Missing | Restored |
| Frame editing | Adjust frame delay, loop mode, frame count | Missing | Restored |
| Video Studio | `/cb video list` and `/cb video extract` existed as stub commands | Commands present but no GUI | Full Video Studio chest GUI |
| `/cb video` command | Stubs only | Stubs only | Fully wired to jcodec + chest GUI |

---

## What this group covers

| Feature | Commands |
|---|---|
| Animated block create | `/cb create <id> <name> <gif-url>` (GIF auto-detected) |
| Anim block editor | `/cb anim <id>` |
| Video file list | `/cb video list` |
| Video extract frame | `/cb video extract <file> <id> <frame>` |
| Video Studio GUI | `/cb video` → opens Video Studio chest GUI |
| Video batch frames | From Video Studio GUI: "Create Animated Block from Video" |

---

## Implementation Requirements

### P1 — Animation System (Rebuilt)

**Supported inputs:**
1. **GIF URL** — `/cb create <id> <name> <url>` where the URL ends in `.gif` or Content-Type is `image/gif`. Each GIF frame extracted to individual PNGs. Packed into the resource pack as a Minecraft animated texture using `mcmeta` metadata.
2. **Pre-sliced PNG sprite sheet** — frames stacked vertically. `/cb anim <id>` allows configuring the frame count and selecting sprite-sheet mode.

**Known bugs fixed (from old version):**
- Frame sizes were not normalized — all frames must be squared to `textureSize` before packing.
- Playback speed was not serialized per-block — now stored in `SlotData.frameDelay`.
- Frame boundaries were sometimes misdetected — now strictly height/frameCount based.

**`/cb anim <id>` chest GUI:**
| Slot | Function |
|---|---|
| Frame count | Set number of frames (for sprite sheets) |
| Frame delay | Set ticks per frame (1 = fastest, 20 = 1 second per frame) |
| Loop mode | Loop / Bounce / Once |
| Preview | Shows animation playing on a map item |
| Source mode | GIF URL / Sprite sheet |
| Re-upload | Triggers re-download and re-processing |
| Apply | Commits changes and triggers pack rebuild |

### Q6 — Video Studio Chest GUI

`/cb video` opens the Video Studio chest GUI:
- **Left column:** List of all `.mp4` files in `config/customblocks/videos/`. Click a file → selected.
- **Right column:** Actions for the selected video:
  - "Extract frame N" — enter frame number, creates a static block from that frame.
  - "Create animated block" — extracts all frames (or a range) and creates an animated block.
  - "Preview frame" — spawns a temp hologram showing that frame.
- **Bottom row:** Status slot showing progress (e.g., "Extracting frame 42 of 240…").

Video processing runs on a daemon thread — does not freeze the server. Pack rebuilds automatically after extraction.

---

## Setup

Place an `.mp4` file in `config/customblocks/videos/` before testing the video features. Any short MP4 (a few seconds) will work.

---

## Test G14.1 — GIF animated block create

```
/cb create g14anim AnimTest https://media.giphy.com/media/example.gif
```
(Use a real GIF URL — any small animated GIF from the internet.)

**Expected:** Block created. GIF frames extracted and packed into resource pack. Block in world shows animated texture cycling through frames.

**Pass:** Block animates correctly in world.
**Fail:** Error, static texture, or black/missing texture.

---

## Test G14.2 — Anim editor opens

```
/cb anim g14anim
```

**Expected:** Chest GUI opens with frame count, frame delay, loop mode, preview, and apply slots.

**Pass:** GUI opens with all slots.
**Fail:** Command missing, or screen-based GUI.

---

## Test G14.3 — Frame delay change

In the anim editor, set frame delay to 5 (fast).

Click Apply.

**Expected:** Animation speeds up visibly in world.

**Pass:** Speed change applies.
**Fail:** No change after apply.

---

## Test G14.4 — Animation persists after restart

Restart the server. Look at `g14anim` in the world.

**Expected:** Animation still plays with the last set frame delay.

**Pass:** Animation preserved.
**Fail:** Block reverts to static or frame delay resets.

---

## Test G14.5 — Video Studio chest GUI opens

```
/cb video
```

**Expected:** Video Studio chest GUI opens. Left column shows `.mp4` files from `config/customblocks/videos/`. Click a file to select it.

**Pass:** GUI opens, video files listed.
**Fail:** Command opens old text-based stub, or GUI missing.

---

## Test G14.6 — Video frame extract (single frame)

In the Video Studio, select a video file. Click "Extract frame N". Enter frame number 1.

**Expected:** A new block is created from frame 1 of the video. Chat: `Block "video_frame_1" created from frame 1 of <filename>.mp4.`

**Pass:** Block created with video frame as texture.
**Fail:** Error, or no block created.

---

## Test G14.7 — Video animated block

In the Video Studio, select a video file. Click "Create animated block". Enter frame range 1–24 (or whatever is available).

**Expected:** An animated block is created with frames extracted from the video. Animation plays in world.

**Pass:** Animated block created from video frames.
**Fail:** Error, static block, or pack rebuild fails.

---

## Test G14.8 — Video processing is non-blocking

During video frame extraction (G14.7), try another command:
```
/cb create g14test BlockDuringVideo
```

**Expected:** Command executes immediately. Server is not frozen during video processing.

**Pass:** Commands work during video extraction.
**Fail:** Server freezes until extraction completes.

---

## Group 14 Verdict

| Test | Description | Result |
|---|---|---|
| G14.1 | GIF animated block created from URL | ⬜ |
| G14.2 | Anim editor GUI opens | ⬜ |
| G14.3 | Frame delay change applies | ⬜ |
| G14.4 | Animation persists after restart | ⬜ |
| G14.5 | Video Studio GUI opens | ⬜ |
| G14.6 | Single frame extracted from video | ⬜ |
| G14.7 | Animated block from video frames | ⬜ |
| G14.8 | Video processing is non-blocking | ⬜ |

**Group 14 passes when animated blocks and video extraction both work in-game.**

If anything shows ❌ — paste:
1. The GIF URL or video file used
2. What the block looked like vs what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g14anim
/cb delete g14test
```
(Also delete any auto-named video frame blocks.)
