package net.roguelogix.biggerreactors.fluids;

import net.roguelogix.phosphophyllite.registry.PhosphophylliteFluid;
import net.roguelogix.phosphophyllite.registry.RegisterFluid;

import javax.annotation.Nonnull;

@RegisterFluid(name = "liquid_obsidian", registerBucket = true)
public class LiquidObsidian extends PhosphophylliteFluid {
    
    @RegisterFluid.Instance
    public static LiquidObsidian INSTANCE;
    
    public LiquidObsidian(@Nonnull Properties properties) {
        super(properties);
    }
}
