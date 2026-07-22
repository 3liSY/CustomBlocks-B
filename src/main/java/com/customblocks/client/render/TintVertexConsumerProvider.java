/**
 * TintVertexConsumerProvider.java — Group 32 (Explosive Tomato) Phase E, E6/E7 shield tint. CLIENT-ONLY.
 *
 * A thin wrapper around a {@link VertexConsumerProvider} that multiplies every vertex colour by a fixed tint,
 * used by {@code ShieldSauceTintMixin} to redden a sauced player's shield (banner shields included — the pattern
 * still renders, just reddened, since we scale colour rather than replace the model).
 *
 * How it stays safe: {@link VertexConsumer} is an interface whose {@code quad(...)} / {@code color(int)} / etc.
 * helpers are DEFAULT methods that call back through {@code this}. The wrapper overrides ONLY the six abstract
 * methods — delegating each side-effect to the real buffer but returning {@code this} so the chain (and the
 * inherited defaults) keep routing through the tinted {@link #color(int, int, int, int)}. Nothing else is touched,
 * so an untinted layer renders byte-for-byte as vanilla would.
 *
 * Depends on: VertexConsumerProvider / VertexConsumer / RenderLayer
 * Called by:  ShieldSauceTintMixin.customblocks$sauceTint
 */
package com.customblocks.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

@Environment(EnvType.CLIENT)
public class TintVertexConsumerProvider implements VertexConsumerProvider {

    private final VertexConsumerProvider delegate;
    private final float r, g, b;

    public TintVertexConsumerProvider(VertexConsumerProvider delegate, float r, float g, float b) {
        this.delegate = delegate;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return new Tint(delegate.getBuffer(layer), r, g, b);
    }

    /** Delegates the six abstract methods to the real buffer, tinting only colour, and returns {@code this}. */
    private static final class Tint implements VertexConsumer {
        private final VertexConsumer d;
        private final float r, g, b;

        Tint(VertexConsumer d, float r, float g, float b) { this.d = d; this.r = r; this.g = g; this.b = b; }

        @Override public VertexConsumer vertex(float x, float y, float z) { d.vertex(x, y, z); return this; }

        @Override public VertexConsumer color(int cr, int cg, int cb, int ca) {
            d.color(scale(cr, r), scale(cg, g), scale(cb, b), ca);
            return this;
        }

        @Override public VertexConsumer texture(float u, float v) { d.texture(u, v); return this; }
        @Override public VertexConsumer overlay(int u, int v) { d.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { d.light(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { d.normal(x, y, z); return this; }

        private static int scale(int c, float f) {
            int v = (int) (c * f);
            return v < 0 ? 0 : Math.min(v, 255);
        }
    }
}
