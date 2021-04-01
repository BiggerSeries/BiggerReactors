package net.roguelogix.biggerreactors.fluids;

import net.roguelogix.phosphophyllite.registry.PhosphophylliteFluid;
import net.roguelogix.phosphophyllite.registry.RegisterFluid;

import javax.annotation.Nonnull;

//TODO: rename this to just steam, cant change registry name for 1.16 though
@RegisterFluid(name = "fluid_irradiated_steam", registerBucket = true)
public class Steam extends PhosphophylliteFluid {
    
    @RegisterFluid.Instance
    public static Steam INSTANCE;
    
    public Steam(@Nonnull Properties properties) {
        super(properties);
    }
}
