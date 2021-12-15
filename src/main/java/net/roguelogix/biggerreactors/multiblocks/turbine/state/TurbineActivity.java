package net.roguelogix.biggerreactors.multiblocks.turbine.state;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nonnull;
import java.util.Locale;

public enum TurbineActivity implements StringRepresentable {
    ACTIVE(1),
    INACTIVE(0);
    
    @SuppressWarnings("SpellCheckingInspection")
    public static final EnumProperty<TurbineActivity> TURBINE_STATE_ENUM_PROPERTY = EnumProperty.create("turbinestate", TurbineActivity.class);

    private final int state;

    TurbineActivity(int state) {
        this.state = state;
    }

    @Override
    @Nonnull
    public String getSerializedName() {
        return toString().toLowerCase(Locale.US);
    }

    /**
     * Get an integer state usable with ROBN.
     *
     * @return An integer usable with ROBN.
     */
    public int toInt() {
        return this.state;
    }

    /**
     * Get a value from an integer state.
     *
     * @param state An integer usable with ROBN.
     * @return A value representing the state.
     */
    public static TurbineActivity fromInt(int state) {
        switch (state) {
            case 1:
                return TurbineActivity.ACTIVE;
            case 0:
                return TurbineActivity.INACTIVE;
        }
        throw new IndexOutOfBoundsException("Invalid index while deciphering turbine activity");
    }
}
