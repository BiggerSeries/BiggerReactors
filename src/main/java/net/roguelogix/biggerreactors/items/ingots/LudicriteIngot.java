package net.roguelogix.biggerreactors.items.ingots;

import net.minecraft.world.item.Item;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

import javax.annotation.Nonnull;

public class LudicriteIngot extends Item {
    
    @RegisterItem(name = "ludicrite_ingot")
    public static final LudicriteIngot INSTANCE = new LudicriteIngot(new Properties());
    
    @SuppressWarnings("unused")
    public LudicriteIngot(@Nonnull Properties properties) {
        super(properties);
    }
}
