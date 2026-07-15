/**
 * UpdateScreen.java
 *
 * Responsibility: The client screen shown when a joining player is on an older CustomBlocks version
 * than the server and auto-update is on (GROUP_20 §CS3, AU5). On open it kicks {@link JarUpdater}
 * and polls it every frame to draw a progress bar. On success it offers [Restart Now]
 * (MinecraftClient.scheduleStop() → clean quit; the player relaunches and Fabric loads the new jar)
 * and [Later]. On failure it shows the error + [Close], leaving the old jar untouched (AU3).
 *
 * No changelog — AU8 is parked (2026-07-11). CLIENT-SIDE ONLY.
 *
 * Depends on: Screen/ButtonWidget/DrawContext, GuiEngine, JarUpdater
 * Called by: UpdateController (older client + autoUpdateEnabled + a download URL)
 */
package com.customblocks.client.update;

import com.customblocks.gui.GuiEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class UpdateScreen extends Screen {

    private static final int BTN_W = 150;
    private static final int BTN_H = 20;
    private static final int BAR_W = 240;
    private static final int BAR_H = 12;

    private final String serverVersion;
    private final String clientVersion;
    private final String downloadUrl;
    private final String sha256;

    private ButtonWidget restartBtn;
    private ButtonWidget laterBtn;
    private ButtonWidget closeBtn;
    private JarUpdater.Phase lastSeen = null;

    public UpdateScreen(String serverVersion, String clientVersion, String downloadUrl, String sha256) {
        super(Text.literal("CustomBlocks Update"));
        this.serverVersion = serverVersion;
        this.clientVersion = clientVersion;
        this.downloadUrl = downloadUrl;
        this.sha256 = sha256;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int by = height / 2 + 40;

        restartBtn = ButtonWidget.builder(Text.literal("Restart Now"),
                b -> { if (client != null) client.scheduleStop(); })
                .dimensions(cx - BTN_W - 4, by, BTN_W, BTN_H).build();
        laterBtn = ButtonWidget.builder(Text.literal("Later"), b -> close())
                .dimensions(cx + 4, by, BTN_W, BTN_H).build();
        closeBtn = ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(cx - BTN_W / 2, by, BTN_W, BTN_H).build();

        addDrawableChild(restartBtn);
        addDrawableChild(laterBtn);
        addDrawableChild(closeBtn);

        // Start the download once (idempotent — a re-init won't relaunch a live/finished run).
        JarUpdater.start(downloadUrl, sha256, serverVersion);
        applyPhase();
    }

    /** Show only the buttons that make sense for the current phase. */
    private void applyPhase() {
        JarUpdater.Phase p = JarUpdater.phase();
        lastSeen = p;
        boolean done = p == JarUpdater.Phase.DONE;
        boolean failed = p == JarUpdater.Phase.FAILED;
        restartBtn.visible = restartBtn.active = done;
        laterBtn.visible = laterBtn.active = done;
        closeBtn.visible = closeBtn.active = failed;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (JarUpdater.phase() != lastSeen) applyPhase(); // react to background thread transitions
        GuiEngine.drawBackground(ctx, width, height);
        GuiEngine.drawHeader(ctx, width);
        GuiEngine.drawTitle(ctx, textRenderer, title, width, 10);
        GuiEngine.drawSeparator(ctx, 0, width, 30);

        int cx = width / 2;
        int y = height / 2 - 40;
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Server runs CustomBlocks v" + serverVersion), cx, y, GuiEngine.COL_BODY);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7You have v" + clientVersion), cx, y + 12, GuiEngine.COL_DIM);

        String status = statusLine();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(status), cx, y + 34, GuiEngine.COL_BODY);

        drawProgressBar(ctx, cx, y + 52);

        super.render(ctx, mx, my, delta);
    }

    private String statusLine() {
        return switch (JarUpdater.phase()) {
            case DOWNLOADING -> "§eDownloading update…";
            case VERIFYING   -> "§eVerifying…";
            case SWAPPING    -> "§eInstalling…";
            case DONE        -> "§a✔ Updated to v" + serverVersion + " — restart to apply.";
            case FAILED      -> "§c✖ " + (JarUpdater.error() == null ? "Update failed." : JarUpdater.error());
            default          -> "§7Preparing…";
        };
    }

    private void drawProgressBar(DrawContext ctx, int cx, int y) {
        if (JarUpdater.isFailed()) return;
        int x = cx - BAR_W / 2;
        ctx.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xFF000000); // opaque border/backing
        ctx.fill(x, y, x + BAR_W, y + BAR_H, 0xFF2A2A2A);                  // track
        float p = JarUpdater.isDone() ? 1f : JarUpdater.progress();
        int fillW;
        if (p < 0f) {
            // indeterminate: a sweeping block while total size is unknown
            long t = (System.currentTimeMillis() / 4) % (BAR_W + 60);
            int bx = (int) (x - 30 + t);
            ctx.fill(Math.max(x, bx), y, Math.min(x + BAR_W, bx + 30), y + BAR_H, GuiEngine.COL_TITLE);
            return;
        }
        fillW = (int) (BAR_W * p);
        ctx.fill(x, y, x + fillW, y + BAR_H, GuiEngine.COL_TITLE);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public boolean shouldCloseOnEsc() {
        // Don't let Esc kill the screen mid-download — only once it's done or failed.
        return JarUpdater.isDone() || JarUpdater.isFailed() || JarUpdater.phase() == JarUpdater.Phase.IDLE;
    }
}
