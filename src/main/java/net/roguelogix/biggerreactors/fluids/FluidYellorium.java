package net.roguelogix.biggerreactors.fluids;

import net.roguelogix.phosphophyllite.registry.PhosphophylliteFluid;
import net.roguelogix.phosphophyllite.registry.RegisterFluid;

import javax.annotation.Nonnull;

@RegisterFluid(name = "fluid_yellorium", color = 0xFFBCBA50)
public class FluidYellorium extends PhosphophylliteFluid {
    
    @RegisterFluid.Instance
    public static FluidYellorium INSTANCE;
    
    public FluidYellorium(@Nonnull Properties properties) {
        super(properties);
    }
}
