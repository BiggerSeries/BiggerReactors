package net.roguelogix.biggerreactors.fluids;

import net.roguelogix.phosphophyllite.registry.PhosphophylliteFluid;
import net.roguelogix.phosphophyllite.registry.RegisterFluid;

import javax.annotation.Nonnull;

@RegisterFluid(name = "liquid_uranium", color = 0xFFBCBA50)
public class LiquidUranium extends PhosphophylliteFluid {
    
    @RegisterFluid.Instance
    public static LiquidUranium INSTANCE;
    
    public LiquidUranium(@Nonnull Properties properties) {
        super(properties);
    }
}
