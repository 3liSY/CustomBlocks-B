/**
 * FaceEditorMenu.java — per-face texture editor for one block (Group 08, G08.11). One slot per
 * face (down/up/north/south/west/east): LEFT-click pre-fills "/cb paintface <id> <face> " in chat
 * so the player pastes a URL (the proven long-URL input path, same as Retexture — URLs don't fit
 * an anvil); RIGHT-click clears that face via the tested "/cb clearface <id> <face>". A reset
 * button clears every face. Painted faces carry an enchant glint. No texture logic lives here.
 *
 * Depends on: ChestMenu/Icons/GuiRouter/Nav, SlotData/SlotManager, TextureStore (face state).
 * Called by:  GuiRouter (Dest.FACE_EDITOR), ChestGuiCommands (/cb facechangegui <id>).
 */
package com.customblocks.gui.chest;

import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FaceEditorMenu {

    private FaceEditorMenu() {} // static-only

    /** The six faces, paired with a friendly label + an item that hints at the face's direction. */
    private static final String[] FACE   = {"up", "down", "north", "south", "west", "east"};
    private static final String[] LABEL  = {"Top", "Bottom", "North", "South", "West", "East"};
    private static final Item[]    ICON   = {
            Items.LIME_STAINED_GLASS, Items.GRAY_STAINED_GLASS, Items.RED_STAINED_GLASS,
            Items.BLUE_STAINED_GLASS, Items.YELLOW_STAINED_GLASS, Items.ORANGE_STAINED_GLASS};
    /** One centred row of six. */
    private static final int[]     SLOTS  = {20, 21, 22, 23, 24, 25};

    public static ChestMenu build(ServerPlayerEntity player, String id) {
        SlotData d = SlotManager.getById(id);
        ChestMenu m = new ChestMenu("CustomBlocks Faces - ID: " + id, 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 46; i < 53; i++) m.set(i, Icons.accent());

        if (d == null) {
            m.set(22, Icons.of(Items.BARRIER, "§cNo block '" + id + "'"));
            m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
            return m;
        }

        int idx = d.index();
        int painted = 0;
        Item preview = SlotManager.itemAt(idx);
        for (int i = 0; i < FACE.length; i++) {
            String face = FACE[i];
            boolean has = TextureStore.hasFace(idx, face);
            if (has) painted++;
            String title = "§e§l" + LABEL[i] + " face";
            String state = has ? "§astatus: painted" : "§7status: default texture";
            var stack = has
                    ? Icons.glint(ICON[i], title, state, "§eLeft-click §7paste a new URL",
                            "§eRight-click §7clear this face")
                    : Icons.of(ICON[i], title, state, "§eLeft-click §7paint from a URL");
            m.set(SLOTS[i], stack, (p, b, a) -> {
                if (b == 1) { // right-click → clear just this face
                    GuiRouter.runAndReopen(p, "clearface " + id + " " + face, MenuKey.of(Dest.FACE_EDITOR, id));
                } else {      // left-click → chat-prefill the paint command for a URL paste
                    GuiRouter.promptCommand(p, "/cb paintface " + id + " " + face + " ", "paintface " + id + " " + face);
                }
            });
        }

        m.set(4, Icons.of(preview != null ? preview : Items.STONE, "§f§l" + d.displayName(),
                "§7id: §f" + d.customId(),
                "§7painted faces: §f" + painted + "§7/6",
                "§8Faces with no override use the base texture."));

        m.set(40, Icons.of(Items.BARRIER, "§cClear all faces",
                        "§7Same as /cb clearallfaces " + id),
                (p, b, a) -> GuiRouter.runAndReopen(p, "clearallfaces " + id, MenuKey.of(Dest.FACE_EDITOR, id)));

        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
