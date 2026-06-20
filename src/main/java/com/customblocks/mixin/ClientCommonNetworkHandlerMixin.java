/**
 * ClientCommonNetworkHandlerMixin.java
 *
 * Group 05 — silent resource-pack delivery. When OUR server has told this client to be
 * silent (SilentPackState), we intercept the point where vanilla builds the
 * "Would you like to download the resource pack?" confirm screen and instead take the
 * exact SILENT add path vanilla already uses when the client's server-pack policy is
 * "Enabled" — `client.getServerResourcePackProvider().addResourcePack(id, url, hash)`.
 *
 * This does NOT touch the resource-pack engine; it only chooses the accept branch. The
 * targeted method is only ever called on the prompt path, after the packet has been
 * marshalled onto the main thread, so the add runs on the right thread.
 *
 * Forced packs (required=true) are left alone — those are a documented Minecraft
 * limitation and still show the vanilla flow.
 *
 * Registered as a client mixin in customblocks.mixins.json.
 */
package com.customblocks.mixin;

import com.customblocks.client.SilentPackState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URL;
import java.util.UUID;

@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {

    /** The exact prompt label our server stamps on its pack (ResourcePackServer.sendToPlayer). */
    private static final String CUSTOMBLOCKS_PACK_LABEL = "CustomBlocks textures";

    @Inject(method = "createConfirmServerResourcePackScreen", at = @At("HEAD"), cancellable = true)
    private void customblocks$silentAccept(UUID id, URL url, String hash, boolean required,
                                           Text prompt, CallbackInfoReturnable<Screen> cir) {
        // OUR pack is recognised by its prompt label, so we silent-accept it the instant it
        // arrives — no dependence on SilentPackPayload having already set the flag. That flag is
        // pushed at join and can lose the race to a pack packet on a fresh world load (→ a stray
        // prompt or a dropped pack). Other servers' packs still honour the per-server flag, so the
        // auto-accept never bleeds onto them.
        boolean ours = prompt != null && CUSTOMBLOCKS_PACK_LABEL.equals(prompt.getString());
        if (required || (!ours && !SilentPackState.isSilent())) return; // forced or other-server not-silent → vanilla prompt
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        // Same call vanilla makes on the silent ("Enabled" policy) branch — no prompt screen.
        client.getServerResourcePackProvider().addResourcePack(id, url, hash);
        cir.setReturnValue(null); // null → no confirm screen is shown
    }
}
