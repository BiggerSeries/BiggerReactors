package net.roguelogix.biggerreactors.multiblocks.reactor.state;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nonnull;
import java.util.Locale;

public enum ReactorType implements StringRepresentable {
    ACTIVE(1),
    PASSIVE(0);
    
    public static final EnumProperty<ReactorType> REACTOR_TYPE_ENUM_PROPERTY = EnumProperty.create("reactortype", ReactorType.class);

    private final int state;

    ReactorType(int state) {
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
    public static ReactorType fromInt(int state) {
        switch (state) {
            case 1:
                return ReactorType.ACTIVE;
            case 0:
                return ReactorType.PASSIVE;
        }
        throw new IndexOutOfBoundsException("Invalid index while deciphering reactor type");
    }
}
