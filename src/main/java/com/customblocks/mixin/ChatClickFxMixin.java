/**
 * ChatClickFxMixin.java — GROUP 04 §G04-4 chat click-feedback trigger. CLIENT-SIDE ONLY.
 *
 * Vanilla routes EVERY clickable-text click (chat chips, book links, sign text) through
 * Screen.handleTextClick(Style). We inject at HEAD, read the style, and hand it to {@link ChatFx},
 * which seeds the click-feedback quartet ONLY when the style is a CustomBlocks chip and ignores
 * everything else. We NEVER cancel and NEVER change the return value — vanilla still runs the command
 * or copies the text exactly as before. This adds feedback next to chat; it does not touch chat itself
 * (the §G04-4 hard rule: never re-render vanilla chat).
 *
 * CLAUDE.md Mixin Checkmark: client mixin on Screen.handleTextClick, HEAD @Inject, no @Cancellable,
 * side-effect only (ChatFx). Listed in the "client" array of customblocks.mixins.json.
 *
 * Depends on: ChatFx. Called by: the Mixin framework on every clickable-text click.
 */
package com.customblocks.mixin;

import com.customblocks.client.hud.ChatFx;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Screen.class)
public class ChatClickFxMixin {

    // require = 0: this is a non-essential visual flourish. If a future mapping ever renames the target,
    // it must degrade to "no click feedback" — NEVER crash the client on load and block a whole test run.
    @Inject(method = "handleTextClick", at = @At("HEAD"), require = 0)
    private void customblocks$chatClickFx(Style style, CallbackInfoReturnable<Boolean> cir) {
        ChatFx.onTextClick(style);
    }
}
