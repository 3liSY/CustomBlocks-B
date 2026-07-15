/**
 * ConfigField.java
 *
 * Responsibility: An immutable descriptor for ONE server setting — its human key + label, a
 * plain-English explanation, its type, default, which Settings-Book page/drawer it lives in, and
 * string-based get/set so every consumer (GUI, editors, search, presets, reset, ✦ modified-marker)
 * reads one shape regardless of the underlying field's Java type (Group 21, §2 of the spec).
 *
 * Values are exchanged as canonical Strings (booleans "true"/"false", ints as digits, enums/hex as
 * their stored token) so save / preset / default-compare are uniform. Type + enumValues/min/max/step
 * tell the editors how to present and change the value.
 *
 * Depends on: nothing (plain data + java.util.function). Built by {@link ConfigRegistry}.
 * Called by:  ConfigRegistry (construction); the config GUI + editors (later phases).
 */
package com.customblocks.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigField {

    /** How a field is presented + edited. ACTION = a button (e.g. Edit HUD); GROUP = opens a
     *  sub-chest; COMING_SOON = visible but inert (greyed, no value). */
    public enum Type { BOOL, ENUM, INT, STRING, HEX, ACTION, GROUP, COMING_SOON }

    public final String key;          // human, stable id used in config.json (e.g. "silent_pack")
    public final String name;         // display label (e.g. "Silent pack")
    public final String help;         // one-line plain-English explanation (always shown)
    public final String category;     // drawer id (see ConfigRegistry.Cat)
    public final int page;            // 1 or 2
    public final Type type;
    public final boolean restart;     // change needs a server restart → show the warning
    public final boolean confirm;     // risky change → Yes/No first (confirm-guard)
    public final String defaultRaw;   // shipped default (drives ✦ marker + shift-click reset)

    public final String[] enumValues; // for ENUM (in cycle order); null otherwise
    public final int min, max, step;  // for INT; ignored otherwise

    private final Supplier<String> getter; // current value as canonical String (null for non-value types)
    private final Consumer<String> setter; // apply a canonical String (null = not directly editable)

    private ConfigField(String key, String name, String help, String category, int page, Type type,
                        boolean restart, boolean confirm, String defaultRaw, String[] enumValues,
                        int min, int max, int step, Supplier<String> getter, Consumer<String> setter) {
        this.key = key; this.name = name; this.help = help; this.category = category; this.page = page;
        this.type = type; this.restart = restart; this.confirm = confirm; this.defaultRaw = defaultRaw;
        this.enumValues = enumValues; this.min = min; this.max = max; this.step = step;
        this.getter = getter; this.setter = setter;
    }

    // ── value access (uniform String) ─────────────────────────────────────────
    /** Current value as a canonical String ("" for ACTION/GROUP/COMING_SOON). */
    public String get() { return getter == null ? "" : getter.get(); }

    /** Apply a canonical String value (no-op when the field isn't directly editable). */
    public void set(String raw) { if (setter != null && raw != null) setter.accept(raw); }

    /** True when the field stores a value the GUI can read/write (vs a button/group/placeholder). */
    public boolean hasValue() { return getter != null; }

    /** True when the value can be changed in-place by the GUI (toggle/cycle/stepper/anvil). */
    public boolean editable() { return setter != null; }

    /** True when the current value equals its shipped default (drives the ✦ modified-marker). */
    public boolean isDefault() { return defaultRaw != null && defaultRaw.equals(get()); }

    /** Reset this field to its shipped default. */
    public void resetToDefault() { if (defaultRaw != null) set(defaultRaw); }

    public boolean asBool() { return Boolean.parseBoolean(get()); }
    public int asInt() { try { return Integer.parseInt(get().trim()); } catch (Exception e) { return 0; } }

    // ── factories ─────────────────────────────────────────────────────────────

    public static ConfigField bool(String key, String name, String help, String cat, int page,
                                    Supplier<Boolean> g, Consumer<Boolean> s, boolean def,
                                    boolean restart, boolean confirm) {
        return new ConfigField(key, name, help, cat, page, Type.BOOL, restart, confirm,
                String.valueOf(def), null, 0, 0, 0,
                () -> String.valueOf(g.get()), v -> s.accept(Boolean.parseBoolean(v)));
    }

    public static ConfigField intField(String key, String name, String help, String cat, int page,
                                        Supplier<Integer> g, Consumer<Integer> s, int def,
                                        int min, int max, int step, boolean restart, boolean confirm) {
        return new ConfigField(key, name, help, cat, page, Type.INT, restart, confirm,
                String.valueOf(def), null, min, max, step,
                () -> String.valueOf(g.get()),
                v -> { try { s.accept(Math.max(min, Math.min(max, Integer.parseInt(v.trim())))); } catch (Exception ignored) {} });
    }

    public static ConfigField enumField(String key, String name, String help, String cat, int page,
                                         Supplier<String> g, Consumer<String> s, String def,
                                         String[] values, boolean restart, boolean confirm) {
        return new ConfigField(key, name, help, cat, page, Type.ENUM, restart, confirm,
                def, values, 0, 0, 0, g, s);
    }

    public static ConfigField string(String key, String name, String help, String cat, int page,
                                      Supplier<String> g, Consumer<String> s, String def, boolean confirm) {
        return new ConfigField(key, name, help, cat, page, Type.STRING, false, confirm,
                def, null, 0, 0, 0, g, s);
    }

    public static ConfigField hex(String key, String name, String help, String cat, int page,
                                  Supplier<String> g, Consumer<String> s, String def) {
        return new ConfigField(key, name, help, cat, page, Type.HEX, false, false,
                def, null, 0, 0, 0, g, s);
    }

    /** A clickable button that performs an action rather than holding a value (e.g. Edit HUD). */
    public static ConfigField action(String key, String name, String help, String cat, int page) {
        return new ConfigField(key, name, help, cat, page, Type.ACTION, false, false,
                null, null, 0, 0, 0, null, null);
    }

    /** A slot that opens its own sub-chest (e.g. Effects, Variant Colours). */
    public static ConfigField group(String key, String name, String help, String cat, int page) {
        return new ConfigField(key, name, help, cat, page, Type.GROUP, false, false,
                null, null, 0, 0, 0, null, null);
    }

    /** A visible but inert placeholder for a feature that isn't built yet (D6/D9). */
    public static ConfigField comingSoon(String key, String name, String help, String cat, int page) {
        return new ConfigField(key, name, help, cat, page, Type.COMING_SOON, false, false,
                null, null, 0, 0, 0, null, null);
    }
}
