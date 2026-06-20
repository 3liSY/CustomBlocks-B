/**
 * StudioAiPanel.java — Group 15 (Block Creation Studio, "AI" tab). CLIENT-SIDE ONLY.
 *
 * Turns the prompt typed in the AI tab into a live texture on the preview cube, keyless, via
 * Pollinations.ai (see {@link AiTextureGenerator}). Same pattern as StudioAnimPanel/StudioCategoryPanel:
 * the screen owns the prompt text field + Regenerate button; this panel draws the tab's labels/status and
 * drives generation. The prompt field is NOT read here directly — its changed-listener keeps
 * {@link StudioState#aiPrompt} live, and this panel reads that each frame from {@link #render}.
 *
 * Live behaviour ("smooth, not spammy"):
 *   • Debounce — fire ~0.8s after typing stops, only for prompts ≥ 3 chars.
 *   • Deterministic seed — derives from the prompt text, so pausing on the same words doesn't re-roll.
 *   • Stale-guard — each generation carries a request id; a slow earlier response can't overwrite a newer one.
 *   • No flicker — the cube keeps the current texture until the new one fully decodes, then swaps.
 *   • Preview fetches small (PREVIEW_SIZE); {@link StudioState#url} stores the CREATE_SIZE url so
 *     "Create & Publish" re-fetches the SAME seed at full quality.
 *
 * Depends on: DrawContext / TextRenderer, StudioState, AiTextureGenerator, StudioTextureLoader, AnimData.
 * Called by: StudioSections (AI case) + BlockCreationStudioScreen (Regenerate button).
 */
package com.customblocks.client.gui;

import com.customblocks.ai.AiTextureGenerator;
import com.customblocks.client.gui.studio.StudioState;
import com.customblocks.core.AnimData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class StudioAiPanel {

    private static final long DEBOUNCE_MS = 800; // fire this long after typing stops
    private static final int  MIN_LEN = 3;       // ignore prompts shorter than this

    // Debounce + de-dup state (lives on the panel, which persists across tab rebuilds).
    private String lastTyped = "";   // last prompt text seen (to detect "still typing")
    private long   lastTypeAt;       // when the prompt last changed
    private String lastGen = "";     // last prompt actually generated (skip re-firing the same words)
    private int    seedSalt;         // bumped by Regenerate to roll a different look for the same prompt

    private volatile int requestSeq; // newest generation id; a worker only applies if it's still newest

    /** Draw the AI tab's labels + status, and run the debounced generation. Reads st.aiPrompt (kept live
     *  by the screen's prompt-field listener). */
    public void render(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, int mx, int my) {
        ctx.drawTextWithShadow(tr, Text.literal("§7Describe the block §8(AI · keyless)"), x, y, 0xFFFFFFFF);

        String prompt = st.aiPrompt == null ? "" : st.aiPrompt.trim();
        maybeGenerate(st, prompt);

        // Status line (mirrors the cube badge so the player sees it near the controls too).
        String status = switch (st.loadState) {
            case "aigen"  -> "§b✦ generating… §8(can take ~30s)";
            case "aifail" -> "§ccouldn't generate — try rephrasing";
            case "ok"     -> st.textureLoaded ? "§a✔ texture ready" : "";
            default       -> "";
        };
        if (!status.isEmpty())
            ctx.drawTextWithShadow(tr, Text.literal(status), x, y + 54, 0xFFFFFFFF);

        if (prompt.length() < MIN_LEN)
            ctx.drawTextWithShadow(tr, Text.literal("§8Type 3+ letters — e.g. glowing red crystal"), x, y + 66, 0xFFFFFFFF);
        else
            ctx.drawTextWithShadow(tr, Text.literal("§8Edits regenerate live · ↻ rolls a new look"), x, y + 66, 0xFFFFFFFF);
    }

    /** Roll a different look for the same prompt (Regenerate ↻). Bumps the seed and fires immediately. */
    public void regenerate(StudioState st) {
        seedSalt++;
        String prompt = st.aiPrompt == null ? "" : st.aiPrompt.trim();
        if (prompt.length() >= MIN_LEN) fire(st, prompt);
    }

    /** Debounce: fire ~0.8s after the prompt stops changing, once per distinct prompt. */
    private void maybeGenerate(StudioState st, String prompt) {
        if (!prompt.equals(lastTyped)) { lastTyped = prompt; lastTypeAt = System.currentTimeMillis(); }
        if (prompt.length() >= MIN_LEN && !prompt.equals(lastGen)
                && System.currentTimeMillis() - lastTypeAt >= DEBOUNCE_MS) {
            fire(st, prompt);
        }
    }

    /** Build the seeded URLs, mark generating, and fetch the preview off-thread (stale-guarded). */
    private void fire(StudioState st, String prompt) {
        lastGen = prompt;
        long seed = seed(prompt);
        // st.url is what "Create & Publish" sends — full size, same seed → the published block matches.
        st.url = AiTextureGenerator.buildUrl(prompt, AiTextureGenerator.CREATE_SIZE, seed);
        String previewUrl = AiTextureGenerator.buildUrl(prompt, AiTextureGenerator.PREVIEW_SIZE, seed);
        st.loadState = "aigen";
        final int mySeq = ++requestSeq;
        Thread t = new Thread(() -> {
            StudioTextureLoader.Result r =
                    StudioTextureLoader.load(previewUrl, AiTextureGenerator.FETCH_TIMEOUT_SECONDS);
            if (mySeq != requestSeq) return; // a newer request superseded this one — drop the stale result
            if (r != null) {
                st.grid = r.frames()[0];
                st.frames = null;        // AI textures are static
                st.anim = AnimData.NONE;
                st.textureLoaded = true;
                st.loadState = "ok";
            } else {
                st.loadState = "aifail"; // keep the last good grid (no flicker)
            }
        }, "CustomBlocks-AiGen");
        t.setDaemon(true);
        t.start();
    }

    /** Deterministic non-negative seed from the prompt + the Regenerate salt. */
    private long seed(String prompt) {
        return ((long) prompt.hashCode() + (long) seedSalt * 1_000_003L) & 0x7fff_ffffL;
    }
}
