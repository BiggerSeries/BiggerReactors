package net.roguelogix.biggerreactors.fluids;

import net.roguelogix.phosphophyllite.registry.PhosphophylliteFluid;
import net.roguelogix.phosphophyllite.registry.RegisterFluid;

import javax.annotation.Nonnull;

@RegisterFluid(name = "steam", registerBucket = true)
public class Steam extends PhosphophylliteFluid {
    
    @RegisterFluid.Instance
    public static Steam INSTANCE;
    
    public Steam(@Nonnull Properties properties) {
        super(properties);
    }
}
