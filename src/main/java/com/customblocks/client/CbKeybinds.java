/**
 * CbKeybinds.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: register the three CustomBlocks key bindings (real vanilla KeyBindings, so
 * they appear under a "CustomBlocks" category in Options → Controls and rebind there for free)
 * and poll them each client tick:
 *   - Toggle HUD on/off  (default H)         → flips HudConfig.visible + saves
 *   - Open CustomBlocks Menu (default RShift)→ runs /cb (server opens the menu GUI)
 *   - Open HUD Editor (default RCtrl)        → opens HudEditorScreen
 *
 * All three keys are unbound in vanilla by default, so the chosen defaults don't clash.
 * Minecraft key bindings are always client-local — there is no "server keybind".
 *
 * Depends on: HudConfig, HudEditorScreen, Fabric KeyBindingHelper + ClientTickEvents.
 * Called by: CustomBlocksClient.onInitializeClient() (register()).
 */
package com.customblocks.client;

import com.customblocks.client.gui.HudEditorScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class CbKeybinds {

    private CbKeybinds() {}

    public static final String CATEGORY = "key.categories.customblocks";

    public static KeyBinding TOGGLE_HUD;
    public static KeyBinding OPEN_MENU;
    public static KeyBinding OPEN_EDITOR;

    public static void register() {
        TOGGLE_HUD = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.customblocks.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
        OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.customblocks.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        OPEN_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.customblocks.open_editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(CbKeybinds::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client == null) return;

        while (TOGGLE_HUD.wasPressed()) {
            HudConfig.visible = !HudConfig.visible;
            HudConfig.save();
            if (client.player != null)
                client.player.sendMessage(net.minecraft.text.Text.literal(
                        "§7CustomBlocks HUD " + (HudConfig.visible ? "§aon" : "§coff")), true);
        }
        while (OPEN_MENU.wasPressed()) {
            if (client.player != null && client.player.networkHandler != null)
                client.player.networkHandler.sendChatCommand("cb");
        }
        while (OPEN_EDITOR.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new HudEditorScreen());
        }
    }
}
