/**
 * StarterBook.java
 *
 * Responsibility: Build the "CustomBlocks Starter Guide" written book and give it to a player.
 * Called once, from the first-join flow (OnboardingManager), so a new player gets a short
 * 5-page quick-start in their hand. Pure item construction — no persistence of its own; the
 * "give only once" gate is the caller's first-join check.
 *
 * Depends on: Minecraft item + written-book component API.
 * Called by:  OnboardingManager.sendWelcome (first join only).
 */
package com.customblocks.core.onboarding;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

import java.util.List;

public final class StarterBook {

    private StarterBook() {} // static-only

    private static final String TITLE  = "CustomBlocks Starter Guide";
    private static final String AUTHOR = "CustomBlocks";

    /** The five pages, in order. Kept short so each fits one book page. */
    private static final List<String> PAGES = List.of(
            "§l§nCustomBlocks§r\n\n§0Turn any image into a real, working block.\n\n§0Flip the page to start. >>",

            "§l§n1. Create§r\n\n§0/cb create <id> <name>\n\n§0Makes a blank block you can texture next.",

            "§l§n2. Texture§r\n\n§0/cb retexture <id> <url>\n\n§0Use any image or GIF link. The block updates live.",

            "§l§n3. Get it§r\n\n§0/cb give <id>\n\n§0Puts the block in your hand. Or open the Custom Blocks creative tab.",

            "§l§n4. More§r\n\n§0/cb  opens the menu.\n§0/cb help  lists everything.\n\n§0Have fun building!");

    /** Give the starter guide book to the player (drops it if the inventory is full). */
    public static void give(ServerPlayerEntity player) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<RawFilteredPair<Text>> pages = PAGES.stream()
                .map(s -> RawFilteredPair.of((Text) Text.literal(s)))
                .toList();
        WrittenBookContentComponent content = new WrittenBookContentComponent(
                RawFilteredPair.of(TITLE), AUTHOR, 0, pages, true);
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content);
        player.giveItemStack(book);
    }
}
