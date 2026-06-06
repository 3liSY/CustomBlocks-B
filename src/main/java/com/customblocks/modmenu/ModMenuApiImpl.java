/**
 * ModMenuApiImpl.java
 *
 * Responsibility: Register CustomBlocks with Mod Menu so the "Config" button in
 * Mod Menu's mod list opens the in-game ConfigScreen.
 * Only loaded when Mod Menu is installed (listed as a suggested dep, not required).
 * CLIENT-SIDE ONLY.
 *
 * Depends on: modmenu API (compile-only), ConfigScreen
 * Called by: Fabric loader via the "modmenu" entrypoint in fabric.mod.json
 */
package com.customblocks.modmenu;

import com.customblocks.gui.screens.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModMenuApiImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigScreen();
    }
}
