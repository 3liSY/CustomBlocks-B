/**
 * TomatoDamageSource.java — Group 32 (Explosive Tomato) Phase E ("Combat"), server side.
 *
 * A DamageSource that knows how to name a SELF-kill (owner-locked 2026-07-17, G32-SELFKILL-MSG). Vanilla's
 * death-message system, given a normal explosion source, names the thrower via the {@code .player} variant —
 * but when the tomato kills its OWN thrower (attacker == victim, e.g. standing in your own blast, or riding
 * into the 20s airburst — the rider is always the owner), that reads as the generic uncredited line with no
 * self-deprecating flavour. Here we intercept only that case, per-victim, and pull from a separate 30-line
 * self-kill pool ({@code death.attack.customblocks_tomato_self_0..29}, self-deprecating tone), keyed off the
 * same random {@code variant} the blast already chose (so the pick stays uniform). Every other victim — a
 * normal kill, or a dispenser tomato with no owner — falls straight through to vanilla's behaviour via super.
 *
 * The self-kill lang lines take a single arg (the victim), so the identical-attacker name is simply not shown.
 *
 * Depends on: CustomBlocksMod (MOD_ID), the resolved tomato_N DamageType entry
 * Called by:  TomatoCombat.explosionDamageSource (built per blast), the death-message pipeline (getDeathMessage)
 */
package com.customblocks.tomato;

import com.customblocks.CustomBlocksMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

public class TomatoDamageSource extends DamageSource {

    /** Which of the 30 message variants this blast rolled — shared by the thrower-credited and self-kill pools. */
    private final int variant;

    public TomatoDamageSource(RegistryEntry<DamageType> type, Entity source, Entity attacker, int variant) {
        super(type, source, attacker);
        this.variant = variant;
    }

    @Override
    public Text getDeathMessage(LivingEntity killed) {
        Entity attacker = getAttacker();
        if (attacker != null && attacker == killed) { // self-kill (thrower == victim, riding included)
            return Text.translatable("death.attack." + CustomBlocksMod.MOD_ID + "_tomato_self_" + variant,
                    killed.getDisplayName());
        }
        return super.getDeathMessage(killed);
    }
}
