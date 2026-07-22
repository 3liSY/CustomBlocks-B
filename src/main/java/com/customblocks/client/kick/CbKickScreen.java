/**
 * CbKickScreen.java — GROUP 04 §G04-5 unified kick screen. CLIENT-SIDE ONLY.
 *
 * The ONE screen every CustomBlocks kick shows. It replaces vanilla's raw DisconnectedScreen (via
 * DisconnectedScreenMixin) whenever a CustomBlocks kick is armed, and is also opened directly by the
 * hidden /cb debug kick <cause> dev command so each cause can be tested without a real crash.
 *
 * Layout (owner-locked 2026-07-15): a red title, then What happened (white) / Why (yellow) / How to
 * fix it (green). A Technical Details control (light blue) expands the raw error + deeper diagnostic
 * (gray) WITHOUT replacing the human explanation. A real Copy Report button (light blue) — never
 * "Copy AI Report", and the report mentions no AI — copies the full pasteable report and works even
 * though the player cannot join. A Back button leaves the screen.
 *
 * Depends on: CbKickInfo (all content + colours), vanilla Screen. Opened by: DisconnectedScreenMixin,
 * CustomBlocksClient (dev preview).
 */
package com.customblocks.client.kick;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.List;

@Environment(EnvType.CLIENT)
public class CbKickScreen extends Screen {

    private final CbKickInfo info;
    private final Runnable onBack;
    private boolean detailsOpen = false;
    private boolean copied = false;
    private ButtonWidget detailsButton;
    private ButtonWidget copyButton;

    public CbKickScreen(CbKickInfo info, Runnable onBack) {
        super(Text.literal(info.title()));
        this.info = info;
        this.onBack = onBack;
    }

    private static Text tinted(String s, int rgb) {
        return Text.literal(s).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0x00FFFFFF)));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int by = this.height - 34;
        int bw = 150;

        detailsButton = ButtonWidget.builder(detailsLabel(), b -> {
            detailsOpen = !detailsOpen;
            b.setMessage(detailsLabel());
        }).dimensions(cx - bw - 82, by, bw, 20).build();

        copyButton = ButtonWidget.builder(copyLabel(), b -> {
            if (this.client != null && this.client.keyboard != null) {
                this.client.keyboard.setClipboard(info.report());
            }
            copied = true;
            b.setMessage(copyLabel());
        }).dimensions(cx - bw / 2, by, bw, 20).build();

        ButtonWidget back = ButtonWidget.builder(tinted("Back", CbKickInfo.WHAT), b -> {
            if (onBack != null) onBack.run();
        }).dimensions(cx + 82, by, bw, 20).build();

        addDrawableChild(detailsButton);
        addDrawableChild(copyButton);
        addDrawableChild(back);
    }

    private Text detailsLabel() {
        return tinted((detailsOpen ? "▾ Technical Details" : "▸ Technical Details"), CbKickInfo.CTRL);
    }

    private Text copyLabel() {
        return tinted(copied ? "Copied ✔" : "Copy Report", CbKickInfo.CTRL);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // background + buttons

        int cx = this.width / 2;
        int colW = Math.min(this.width - 60, 460);
        int x = cx - colW / 2;
        int y = Math.max(20, this.height / 8);

        ctx.drawCenteredTextWithShadow(this.textRenderer, tinted(info.title(), CbKickInfo.TITLE), cx, y, 0xFFFFFFFF);
        y += 22;

        y = section(ctx, x, y, colW, "What happened", info.whatHappened(), CbKickInfo.WHAT);
        y = section(ctx, x, y, colW, "Why", info.why(), CbKickInfo.WHY);
        y = section(ctx, x, y, colW, "How to fix it", info.howToFix(), CbKickInfo.FIX);

        if (!info.errorCode().isEmpty()) {
            y += 2;
            ctx.drawTextWithShadow(this.textRenderer, tinted("Error code: " + info.errorCode(), CbKickInfo.RAW), x, y, 0xFFFFFFFF);
            y += 12;
        }

        if (detailsOpen) {
            y += 6;
            y = section(ctx, x, y, colW, "Technical details", info.technicalDetails(), CbKickInfo.RAW);
            y = section(ctx, x, y, colW, "Raw error", info.rawError(), CbKickInfo.RAW);
        }
    }

    /** Draw a bold coloured label + its wrapped body; return the y below it. */
    private int section(DrawContext ctx, int x, int y, int colW, String label, String body, int color) {
        ctx.drawTextWithShadow(this.textRenderer, tinted(label + ":", color).copy().styled(s -> s.withBold(true)), x, y, 0xFFFFFFFF);
        y += 11;
        List<OrderedText> lines = this.textRenderer.wrapLines(tinted(body, color), colW);
        for (OrderedText line : lines) {
            ctx.drawTextWithShadow(this.textRenderer, line, x, y, 0xFFFFFFFF);
            y += 10;
        }
        return y + 6;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // a kick screen is not a menu — leave only via Back, like vanilla's disconnect screen
    }
}
