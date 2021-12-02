package net.roguelogix.biggerreactors.fluids;

import net.roguelogix.phosphophyllite.registry.PhosphophylliteFluid;
import net.roguelogix.phosphophyllite.registry.RegisterFluid;

import javax.annotation.Nonnull;

//@RegisterFluid(name = "superheated_sodium", registerBucket = true)
public class SuperheatedSodium extends PhosphophylliteFluid {
    
    @RegisterFluid.Instance
    public static SuperheatedSodium INSTANCE;
    
    public SuperheatedSodium(@Nonnull Properties properties) {
        super(properties);
    }
}
