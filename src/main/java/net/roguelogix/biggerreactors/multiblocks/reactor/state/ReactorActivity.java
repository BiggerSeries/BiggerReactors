package net.roguelogix.biggerreactors.multiblocks.reactor.state;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nonnull;
import java.util.Locale;

public enum ReactorActivity implements StringRepresentable {
    ACTIVE(1),
    INACTIVE(0);

    public static final EnumProperty<ReactorActivity> REACTOR_ACTIVITY_ENUM_PROPERTY = EnumProperty.create("reactoractivity", ReactorActivity.class);

    private final int state;

    ReactorActivity(int state) {
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
    public static ReactorActivity fromInt(int state) {
        switch (state) {
            case 1:
                return ReactorActivity.ACTIVE;
            case 0:
                return ReactorActivity.INACTIVE;
        }
        throw new IndexOutOfBoundsException("Invalid index while deciphering reactor activity");
    }
}
