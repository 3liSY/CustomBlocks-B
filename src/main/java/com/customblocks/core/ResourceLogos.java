/**
 * ResourceLogos.java — GROUP_14 §S TEMPORARY (remove when the logo re-source job is done).
 *
 * The assistant-maintained list of sourced HD brand logos: block id → a direct, hotlinkable PNG URL of that
 * brand's CURRENT official logo. /cb tempretexture all walks this list and bakes each URL onto the matching
 * base block in one pass; the owner reviews in-game and /cb tempretexture undo <id> reverts any miss.
 *
 * RULES for entries (the WhatsApp-stale-logo lesson):
 *   • Direct raster image only (PNG/JPG). SVG won't decode — use Wikimedia's /thumb/.../NNNpx-*.png render.
 *   • Must be the brand's CURRENT official logo, and the URL must actually resolve (verified 200 + image/*).
 *   • id = the block's customId (the floating tag on /cb sourcewall), lower-case.
 *   • Only LOGOS belong here — non-brand subjects (food, objects, scenery) have no canonical source.
 *
 * Depends on: nothing. Called by: TempRetextureCommands.
 */
package com.customblocks.core;

import java.util.List;

public final class ResourceLogos {

    private ResourceLogos() {} // static-only

    /** One sourced logo: the target block id and the verified current-logo image URL. */
    public record Logo(String id, String url) {}

    /**
     * The sourced logos. Grows batch-by-batch as the assistant verifies more; the owner already did
     * whatsapp / mitsubishi / tesla by hand, so those are intentionally NOT here.
     */
    public static final List<Logo> LIST = List.of(
        // ── Batch 1 (2026-06-28) — 23 logos, each verified 200 + image/png via Wikimedia Special:FilePath.
        //    FilePath renders the SVG to a 512px PNG and the mod follows the (https→https) redirect. ──
        // Social / tech
        new Logo("youtube", "https://commons.wikimedia.org/wiki/Special:FilePath/YouTube%20full-color%20icon%20%282017%29.svg?width=512"), // play-button icon
        new Logo("netflix", "https://commons.wikimedia.org/wiki/Special:FilePath/Netflix%202015%20logo.svg?width=512"), // red wordmark
        new Logo("facebook", "https://commons.wikimedia.org/wiki/Special:FilePath/2021%20Facebook%20icon.svg?width=512"), // blue f
        new Logo("instagram", "https://commons.wikimedia.org/wiki/Special:FilePath/Instagram%20logo%202022.svg?width=512"), // gradient camera
        new Logo("twitch", "https://commons.wikimedia.org/wiki/Special:FilePath/Twitch%20Glitch%20Logo%20Purple.svg?width=512"), // purple glitch
        new Logo("pinterest", "https://commons.wikimedia.org/wiki/Special:FilePath/Pinterest-logo.png?width=512"), // red P
        new Logo("discord", "https://commons.wikimedia.org/wiki/Special:FilePath/Discord%20Color%20Text%20Logo%20No%20Padding.svg?width=512"), // blurple wordmark (Commons has no Clyde mascot)
        new Logo("snapchat", "https://commons.wikimedia.org/wiki/Special:FilePath/Snapchat-logo-svgrepo-com.svg?width=512"), // yellow ghost
        new Logo("reddit", "https://commons.wikimedia.org/wiki/Special:FilePath/Reddit%20logo.svg?width=512"), // orange alien wordmark
        new Logo("twitter", "https://commons.wikimedia.org/wiki/Special:FilePath/Logo%20of%20Twitter.svg?width=512"), // BLUE BIRD, not the X rebrand — undo if you want X
        new Logo("skype", "https://commons.wikimedia.org/wiki/Special:FilePath/Skype%20logo%202017.svg?width=512"), // blue cloud
        // Games
        new Logo("minecraft", "https://commons.wikimedia.org/wiki/Special:FilePath/Minecraft%20Logo-en.svg?width=512"), // game wordmark
        new Logo("mario", "https://commons.wikimedia.org/wiki/Special:FilePath/Mario%20Series%20Logo.svg?width=512"), // Super Mario wordmark
        new Logo("ps1", "https://commons.wikimedia.org/wiki/Special:FilePath/PlayStation%20logo.svg?width=512"), // PlayStation brand logo
        new Logo("ps2", "https://commons.wikimedia.org/wiki/Special:FilePath/PlayStation%202%20logo.svg?width=512"), // PS2 wordmark
        new Logo("ps3", "https://commons.wikimedia.org/wiki/Special:FilePath/PlayStation%203%20logo.svg?width=512"), // PS3 wordmark
        // Cars
        new Logo("bmw", "https://commons.wikimedia.org/wiki/Special:FilePath/BMW.svg?width=512"), // roundel
        new Logo("mercedes", "https://commons.wikimedia.org/wiki/Special:FilePath/Mercedes-Logo.svg?width=512"), // three-point star
        new Logo("toyota", "https://commons.wikimedia.org/wiki/Special:FilePath/Toyota.svg?width=512"), // emblem
        new Logo("mazda", "https://commons.wikimedia.org/wiki/Special:FilePath/Mazda%20logo%20with%20emblem%2C%20new.svg?width=512"), // emblem + wordmark
        new Logo("jeep", "https://commons.wikimedia.org/wiki/Special:FilePath/Jeep%201987%20logo.svg?width=512"), // classic wordmark
        new Logo("infiniti", "https://commons.wikimedia.org/wiki/Special:FilePath/Infiniti%20logo%202023.svg?width=512"), // current emblem
        // Sport
        new Logo("bayern", "https://commons.wikimedia.org/wiki/Special:FilePath/FC%20Bayern%20M%C3%BCnchen%20logo%20%282017%29.svg?width=512"), // crest

        // ── Batch 2 (2026-06-28) — 15 more, each verified 200 + image/png via en.wikipedia Special:FilePath
        //    (en.wikipedia hosts the fair-use logos Commons won't — game logos + club crests). ──
        // Games
        new Logo("tiktok", "https://en.wikipedia.org/wiki/Special:FilePath/Tiktok%20icon.svg?width=512"), // multicolor music note
        new Logo("fortnite", "https://en.wikipedia.org/wiki/Special:FilePath/Fortnite.png?width=512"), // logo
        new Logo("gta", "https://en.wikipedia.org/wiki/Special:FilePath/Grand%20Theft%20Auto%20logo%20series.svg?width=512"), // spiky letters
        new Logo("fnaf", "https://en.wikipedia.org/wiki/Special:FilePath/Five%20Nights%20at%20Freddy%27s%20Logo.png?width=512"), // logo
        new Logo("amongus", "https://en.wikipedia.org/wiki/Special:FilePath/Among%20Us%20Logo.svg?width=512"), // logo
        new Logo("overwatch", "https://en.wikipedia.org/wiki/Special:FilePath/Overwatch%202%20logo.svg?width=512"), // OW2 logo
        new Logo("hollowknight", "https://en.wikipedia.org/wiki/Special:FilePath/Hollow%20Knight%20Textlogo.png?width=512"), // wordmark
        // Brands
        new Logo("huawei", "https://en.wikipedia.org/wiki/Special:FilePath/Huawei%20wordmark.svg?width=512"), // red wordmark
        new Logo("porsche", "https://en.wikipedia.org/wiki/Special:FilePath/Porsche-911-logo.png?width=512"), // crest
        // Football crests
        new Logo("arsenal", "https://en.wikipedia.org/wiki/Special:FilePath/Arsenal%20FC.svg?width=512"), // cannon crest
        new Logo("chelsea", "https://en.wikipedia.org/wiki/Special:FilePath/ChelseaFC.png?width=512"), // lion crest
        new Logo("city", "https://en.wikipedia.org/wiki/Special:FilePath/Manchester%20City%20FC%20badge.svg?width=512"), // Man City badge
        new Logo("united", "https://en.wikipedia.org/wiki/Special:FilePath/Manchester%20United%20FC%20crest.svg?width=512"), // Man Utd red devil crest
        new Logo("alahli", "https://en.wikipedia.org/wiki/Special:FilePath/Al%20Ahli%20Saudi%20FC%20logo.svg?width=512"), // crest
        new Logo("aljazira", "https://en.wikipedia.org/wiki/Special:FilePath/Al%20Jazira%20Club%20logo%20en%20%282021%29.svg?width=512") // crest
    );
}
