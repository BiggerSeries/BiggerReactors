package net.roguelogix.biggerreactors.multiblocks.turbine.state;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Locale;

public enum TurbineShaftRotationState implements StringRepresentable {
    X,
    Y,
    Z;
    
    @SuppressWarnings("SpellCheckingInspection")
    public static final EnumProperty<TurbineShaftRotationState> TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY = EnumProperty.create("turbineshaftrotation", TurbineShaftRotationState.class);
    
    @Override
    public String getSerializedName() {
        return toString().toLowerCase(Locale.US);
    }
}
