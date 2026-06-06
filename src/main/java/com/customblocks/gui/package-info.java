/**
 * In-game GUI system. GuiEngine provides shared render primitives and widgets;
 * GuiMode enumerates every screen context; GuiState is a serializable snapshot
 * driving an ArrayDeque back-stack; AnvilPromptManager handles text input;
 * ColorLibrary / ColorPickerHelper provide HSV/RGB selection. Concrete screens
 * live in the screens sub-package.
 *
 * Design rule: NO GUI monolith. The old GuiManager grew to ~9,400 lines and must
 * not be repeated (see §9.3). GUI state is always serialized before navigation.
 */
package com.customblocks.gui;
