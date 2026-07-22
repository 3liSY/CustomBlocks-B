# Master Bug Board

*All active bugs across all testing guides.*

| Guide | Bug ID | Test Row | Issue Description | Flag |
|---|---|---|---|---|
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N1 | UI/UX | Open `/cb create`. Left-click a face on the spinning preview cube. Verify it gets a highly visible glowing outline. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N2 | UI/UX | Drag the cube. Verify the block rotates but no faces are accidentally selected. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N3 | UI/UX | Right-click the cube. Verify spinning pauses/unpauses. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N4 | UI/UX | Multi-select: Click Top, then click North. Verify both have glowing outlines. Change texture. Verify both faces update. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N5 | UI/UX | Copy properties: Select a fully configured face, then click a blank face. Verify the blank face inherits all properties. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N6 | UI/UX | Complex shape: Create a Stair block. Verify you can click individual exposed polygon surfaces on the step. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N7 | Physics | Apply 6 distinct GIFs to 6 faces. Verify they play flawlessly and fully synchronized. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N8 | Physics | Set Top face to Glass (transparent). Verify you can see the inside of the block, and the inside walls mirror the outside textures. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N9 | Physics | Hollow Box: Set North face to "Passable". Verify you can walk straight through the North face into the block, but get blocked by the South face. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N10 | Physics | Directional Light: Set East face to Glow 15. Verify the floor to the East lights up. Break block, verify light goes away. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N11 | Sound | Set Top to Glass, Sides to Wood. Verify walking on top sounds like glass, touching sides sounds like wood. Break block, verify mixed sound plays. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N12 | UI/UX | Broken Link: Apply a 404 URL. Verify face flashes red in the Studio UI and auto-selects for fixing. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N13 | Menus | Inventory: Verify the held item displays a standard 3D isometric view with the customized faces. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N14 | Menus | Hub: Open `/cb hub`. Verify the block renders correctly as a 3D item and the menu does not lag. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | N15 | Cmds | Commands: Verify `/cb retexture` still only applies to the entire block as a fallback. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | ID | Group | Test Case | Status |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | --- | --- | --- | --- |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | O1 | UI | Open Studio. Verify 10 tabs are condensed into 4: Identity, Design, Shape, Behavior. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | O2 | UI | In the **Editing** tab, verify the two internal modes are exactly **Paint** and **Resize**; Animation remains a separate top-level tab. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | O3 | Resize | Resize Workspace: load a source, verify width/height fields, 64/128/256/512 presets, Custom size, and the linked-aspect default. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | O4 | Resize | Change the fit method to Keep shape, Crop, and Stretch. Verify the before/after preview changes to match each choice. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | O5 | Paint | Editing Paint Workspace: verify the camera stays on a movable flat 2D canvas with pan, zoom, and Fit to Screen. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | O6 | Paint | Glass Eraser: Paint a solid color. Select Glass Eraser and wipe. Verify a genuine transparent hole is punched. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | O7 | Paint | Synchronized Mirroring: Multi-select Top + North. Draw on Top. Verify brush strokes mirror instantly onto North. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | O8 | Keys | Shortcuts: Verify `[ ]` changes brush size, `Ctrl+Z` undoes, and holding `Spacebar` allows camera panning. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | O9 | Anim | Animation Workspace: Load a GIF. Open Film tab. Verify a massive horizontal timeline spans the bottom screen. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | O10 | Paint/Anim | Verify a still-image Paint reference underlay can be shown with adjustable opacity. GIF frame painting remains unavailable in this release. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | ID | Area | Test Case | Status |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | --- | --- | --- | --- |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E1 | Entry | Run `/cb create`, choose **Editing**, and verify one Editing tab opens with exactly **Paint** and **Resize** mode choices. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E2 | Entry | Run `/cb paint` and verify it opens the same Editing tab with Paint selected; no duplicate Paint screen appears. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E3 | Entry | Run `/cb resize` and verify it opens the same Editing tab with Resize selected; no duplicate Resize screen appears. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E4 | Create | In a new block flow, load a still image and verify Paint and Resize can be used before publishing. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E5 | Existing | Open an existing block in Edit Mode and verify its stored texture and dimensions are pre-filled without quality loss. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E6 | Navigation | Switch between Paint and Resize after making a working change. Verify the working result, mode state, and preview are retained. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E7 | Canvas | In Paint, verify the primary surface is a stable flat 2D viewport. Drag to pan, zoom in/out, and use Fit to Screen; texture data must not change from navigation. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E8 | Paint | Use pen, eraser, fill, eyedropper, line, rectangle, symmetry, and brush size. Verify each changes only the intended working texture area. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E9 | Paint | Paint transparency and erase pixels. Verify transparent pixels show on the checkerboard and remain transparent in the live preview. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E10 | GIF boundary | Load a GIF in Editing. Verify Paint clearly reports that GIF painting is unavailable, while Resize remains available. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E11 | Resize | Verify current width/height, editable numeric fields, 64/128/256/512 presets, Custom size, allowed maximum, and output-size estimate are visible. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E12 | Resize | Change width with proportions linked by default. Verify height follows; unlock proportions and verify width and height can then be edited independently. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E13 | Resize | Test Keep shape, Crop, and Stretch on the same still image. Verify the before/after preview shows fit-without-distortion, edge removal, and distortion respectively. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E14 | Resize | Resize a still image, click Apply, and verify the actual new or existing block uses the requested dimensions and fit method. The Editing tab stays open. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E15 | GIF Resize | Resize a GIF and verify every frame uses the same dimensions and fit method; no frame is skipped, duplicated, or left at the old size. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E16 | GIF preservation | Before and after GIF Resize, compare frame count, frame order, per-frame timing, and loop behavior. Verify all remain unchanged. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E17 | Preview | Use the before/after view for Paint, still Resize, and GIF Resize. Verify the original and current result are clearly distinguishable and update after each working change. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E18 | Original | Make Paint and Resize changes, then use **Restore Original** in the lower corner. Confirm the deliberate prompt and verify the original texture and dimensions return. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E19 | History | Use on-screen Undo/Redo after Paint and Resize. Then use `/cb undo` and `/cb redo`; verify both command paths operate on the same shared history. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E20 | Validation | Enter invalid, empty, oversized, and unsupported resize values. Verify no server change occurs and the screen gives a human explanation and a valid next action. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | E21 | Multiplayer | Apply a Paint or Resize change on a server, then have a second player view the block. Verify the applied result syncs without a rejoin and no unfinished working preview leaks to the other player. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | ID | Group | Test Case | Status |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | --- | --- | --- | --- |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P1 | UI | Run `/cb admin`. Verify UI blurs world background (Glassmorphism), scales to 80%, and plays dark red background animation. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P2 | T1 | Server Status: Verify it accurately identifies SP vs MP, and Capacity bar displays Used/Max slots. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P3 | T1 | Capacity Colors: Force Used slots > 86%. Verify Capacity progress bar dynamically turns Red. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P4 | T2 | General: Toggle Typo Correction to On. Open Advanced Dictionary, verify custom synonyms can be added/saved. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P5 | T3 | Backups: Open Backups tab. Verify a scrollable "Live Backup Manager" list exists with a red Restore button. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P6 | T3 | History: Click "Purge History Cache". Verify double-confirm prompt appears, and Anvil clank sound plays. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P7 | T4 | Visuals: Open Variant Colours. Verify a massive visual RGB Color Picker (photoshop-style) opens instead of a text field. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P8 | T5 | Vault: Click "Login to Vault". Verify an in-game OAuth-style browser window successfully opens and authenticates. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P9 | T5 | Webhooks: Paste a Discord URL and click "Test Webhook". Verify the customized test payload correctly sends. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P10 | T6 | Developer: Open Dev Tab (Admin only). Verify live RAM usage chart updates in real-time. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P11 | Cmds | Aliases: Verify `/cb config`, `/cb settings`, and `/cb admin` all open the exact same screen. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P12 | Core | Global Settings: Open any screen. Click the `⚙` gear. Verify "Accent Color" and "UI Volume Slider" apply universally. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P13 | Core | Search: Type in Search Bar. Verify UI jumps to the result and draws a glowing golden border around the field. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P14 | Core | Unsaved: Alter `maxSlots`. Verify Apply button turns orange. Press `ESC`. Verify red "Discard or Save?" popup triggers. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | P15 | Sec | True Privacy: De-op a player. Verify they only have access to Tab 1. Tabs 2-6 show Padlocks and actual values read `Hidden`. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | ID | Group | Test Case | Status |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | --- | --- | --- | --- |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | R1 | UI | Run `/cb undogui`. Verify a Screen interface opens showing the player's personal undo stack. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | R2 | UI | Run `/cb history`. Verify a Screen interface opens showing the mutation log. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | R3 | Cmds | In `/cb history`, verify advanced dropdowns can filter by Player, Date, and Block Type. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | R4 | Sec | As an OP, select a row in `/cb history`. Verify the [Rollback] button appears and successfully reverts that specific edit. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | ID | Group | Test Case | Status |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | --- | --- | --- | --- |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | S1 | UI | Trigger a success event. Verify a Modern Toast slides in from the top-right with a popping green checkmark micro-animation. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | S2 | UI | Toast Expiry: Wait for a toast to expire. Verify it physically detaches and falls off the bottom of the screen with gravity. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | S3 | Physics | Toast Swatting: Flick your mouse cursor at a falling toast. Verify you can physically 'swat' it off the screen edge. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | S4 | UI | Overload: Trigger 15 events in 1 millisecond. Verify a single "OVERLOAD" mega-toast appears, a 1-second subtle red vignette shader triggers on screen, and a bass-boosted sound plays instead of 15 overlapping sounds. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | S5 | UX | Wiki Tome: Run `/cb help`. Verify a hyper-realistic 3D book opens, and you can physically drag pages to turn them. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | S6 | UX | Embedded Playgrounds: Find a slider inside a Wiki text paragraph. Drag it. Verify the 3D block next to the text updates live. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | S7 | MP | Google Docs Editor: Two admins open the same Wiki page to edit. Verify both admins can see each other's live glowing cursors typing. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | S8 | UX | Admin Media: Drag a `.png` file from your actual Windows Desktop directly onto the Minecraft window. Verify the image embeds instantly into the Wiki editor. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | S9 | Video | First Join Welcome: Join as a new player. Verify a 10-second cinematic hype video aggressively force-opens and cannot be skipped. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | S10 | Combat | Welcome Invincibility: Get attacked by a zombie while the 10-second cinematic is playing. Verify you are 100% invincible and take no damage until it finishes. | ⏳ Planned |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | § | Feature | Status | Flags |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | --- | --- | --- | --- |
| [Group_27_Testing_Guide.md](Group_27_Testing_Guide.md) | A | Group 27 — Screens, Studio & HUD | Done ✅ | - |
