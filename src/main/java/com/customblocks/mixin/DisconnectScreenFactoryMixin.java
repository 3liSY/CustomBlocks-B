/**
 * DisconnectScreenFactoryMixin.java — GROUP 04 §G04-5 kick unification (PRIMARY swap). CLIENT-SIDE ONLY.
 *
 * Why this exists (2026-07-19 fix for TG4 §E "real kick shows the old vanilla screen"):
 *   The real registry/max_blocks kick fires in the CONFIGURATION phase — {@link
 *   com.customblocks.mixin.RegistrySyncHealMixin} arms a {@link CbKickInfo} then throws, and Minecraft
 *   disconnects. On MC 1.21.1 that disconnect is built by {@code ClientCommonNetworkHandler.onDisconnected}
 *   → {@code createDisconnectedScreen(info)} → {@code MinecraftClient.disconnect(screen)}. The OLD approach
 *   ({@link DisconnectedScreenMixin}) let vanilla build its {@code DisconnectedScreen} and then tried to
 *   swap it out from the screen's own {@code init} TAIL — a re-entrant {@code setScreen} that also carried
 *   {@code require = 0}, so it degraded to a silent no-op in production and the swap never landed (while
 *   the {@code /cb debug kick} preview kept working because it calls {@code setScreen} directly, no mixin).
 *
 *   This mixin fixes that at the source: it intercepts the disconnect SCREEN FACTORY. When a CustomBlocks
 *   kick is armed, {@code createDisconnectedScreen} returns {@link CbKickScreen} directly, so the disconnect
 *   flow shows our screen from birth — no re-entrancy, no deferred {@code execute}, no dependence on the
 *   vanilla screen's {@code init} being hooked. This is the same class (and injection style) as the
 *   confirmed-working Group 05 pack mixin ({@link ClientCommonNetworkHandlerMixin}), so it applies reliably
 *   in the remapped production jar.
 *
 * A non-CustomBlocks disconnect arms nothing, so {@link CbKick#consume} returns null and vanilla's screen
 * is returned untouched. {@link DisconnectedScreenMixin} is kept as a harmless secondary fallback (the
 * one-shot consume means only one of the two ever fires).
 *
 * CLAUDE.md Mixin Checkmark: client mixin on ClientCommonNetworkHandler.createDisconnectedScreen, HEAD
 * @Inject, cancellable (returns CbKickScreen only when a kick is armed). Listed in the "client" array of
 * customblocks.mixins.json.
 *
 * Depends on: CbKick, CbKickInfo, CbKickScreen. Called by: the Mixin framework when the disconnect screen
 * is built for any client common (login-config / play) disconnect.
 */
package com.customblocks.mixin;

import com.customblocks.client.kick.CbKick;
import com.customblocks.client.kick.CbKickInfo;
import com.customblocks.client.kick.CbKickScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.DisconnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ClientCommonNetworkHandler.class)
public class DisconnectScreenFactoryMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("CustomBlocks");

    @Inject(method = "createDisconnectedScreen", at = @At("HEAD"), cancellable = true)
    private void customblocks$kickScreen(DisconnectionInfo info, CallbackInfoReturnable<Screen> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        CbKickInfo kick = CbKick.consume();
        if (kick == null) return; // not a CustomBlocks kick → leave vanilla's disconnect screen untouched
        // Fires ONLY on a real armed CustomBlocks kick (rare) — never on ordinary disconnects, so this is
        // not INFO spam. Lets the owner's end-test confirm the real path swapped in CbKickScreen.
        LOGGER.info("[CustomBlocks] Real kick → showing CbKickScreen ({})", kick.errorCode());
        cir.setReturnValue(new CbKickScreen(kick, () -> mc.setScreen(new TitleScreen())));
    }
}
