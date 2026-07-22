/**
 * DisconnectedScreenMixin.java — GROUP 04 §G04-5 kick unification (SECONDARY fallback). CLIENT-SIDE ONLY.
 *
 * PRIMARY swap now lives in {@link DisconnectScreenFactoryMixin}, which returns {@link CbKickScreen}
 * straight from {@code createDisconnectedScreen} so the disconnect flow shows our screen from birth. This
 * mixin remains only as a belt-and-suspenders fallback for any path that builds a {@code DisconnectedScreen}
 * WITHOUT going through that factory (none today; kept in case a future kick cause does). The one-shot
 * {@link CbKick#consume} means at most one of the two fires: on the real config-phase kick the factory
 * consumes first and no {@code DisconnectedScreen} is ever created, so this mixin's TAIL never runs.
 *
 * Historical note (2026-07-19): this after-the-fact swap was the cause of TG4 §E's regression — it carried
 * {@code require = 0} and did a re-entrant {@code setScreen} from the screen's own init, which degraded to
 * a silent no-op in the remapped production jar so the swap never landed (while the {@code /cb debug kick}
 * preview kept working because it calls {@code setScreen} directly, no mixin). The factory interception
 * above replaces that fragile path; this fallback must not be relied on as the sole mechanism.
 *
 * When it does fire: vanilla's raw DisconnectedScreen is swapped for {@link CbKickScreen}. The swap is
 * deferred through {@code client.execute} so it happens cleanly rather than re-entrantly inside the vanilla
 * screen's own init.
 *
 * CLAUDE.md Mixin Checkmark: client mixin on DisconnectedScreen.init, TAIL @Inject, no @Cancellable,
 * require = 0 (a mapping change must degrade to the vanilla screen, never crash the client). Listed in
 * the "client" array of customblocks.mixins.json.
 *
 * Depends on: CbKick, CbKickScreen. Called by: the Mixin framework when a disconnect screen opens.
 */
package com.customblocks.mixin;

import com.customblocks.client.kick.CbKick;
import com.customblocks.client.kick.CbKickInfo;
import com.customblocks.client.kick.CbKickScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin {

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void customblocks$maybeKickScreen(CallbackInfo ci) {
        CbKickInfo info = CbKick.consume();
        if (info == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.execute(() -> mc.setScreen(new CbKickScreen(info, () -> mc.setScreen(new TitleScreen()))));
    }
}
