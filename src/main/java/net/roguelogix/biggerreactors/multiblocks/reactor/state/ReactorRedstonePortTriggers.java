package net.roguelogix.biggerreactors.multiblocks.reactor.state;

import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;
import java.util.Locale;

public enum  ReactorRedstonePortTriggers implements StringRepresentable {
    PULSE_OR_ABOVE(false),
    SIGNAL_OR_BELOW(true);

    private final boolean state;

    ReactorRedstonePortTriggers(boolean state) {
        this.state = state;
    }

    @Override
    @Nonnull
    public String getSerializedName() {
        return toString().toLowerCase(Locale.US);
    }

    /**
     * Get a boolean state usable with ROBN.
     *
     * @return A boolean usable with ROBN.
     */
    public boolean toBool() {
        return this.state;
    }

    /**
     * Get a boolean from an integer state.
     *
     * @param state A boolean usable with ROBN.
     * @return A value representing the state.
     */
    public static ReactorRedstonePortTriggers fromBool(boolean state) {
        return (state) ? SIGNAL_OR_BELOW : PULSE_OR_ABOVE;
    }
}