/**
 * TimerPart.java — Group 31 (BuzzerGame) item F (per-part resize).
 *
 * The independently-resizable pieces of the timer stand. BASE / LEG / SCREEN are the three stacked model
 * parts (each its own ITEM_DISPLAY, growing a lower one lifts the parts above); WHOLE scales all of them
 * together (also driven by {@code /cb buzzergame size}). The wand's Resize mode picks BASE/LEG/SCREEN by the
 * band the crosshair lands in; WHOLE is reached through the size command.
 *
 * Called by: BuzzerGameWand (selection + stepping), TimerDisplayBlockEntity (per-part scale storage).
 */
package com.customblocks.buzzergame;

public enum TimerPart {
    BASE("base"),
    LEG("leg"),
    SCREEN("screen"),
    WHOLE("whole");

    private final String label;

    TimerPart(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
