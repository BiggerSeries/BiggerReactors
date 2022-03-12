package net.roguelogix.biggerreactors.items.ingots;

import net.minecraft.world.item.Item;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

import javax.annotation.Nonnull;

public class BlutoniumIngot extends Item {
    
    @RegisterItem(name = "blutonium_ingot")
    public static final BlutoniumIngot INSTANCE = new BlutoniumIngot(new Properties());
    
    @SuppressWarnings("unused")
    public BlutoniumIngot(@Nonnull Properties properties) {
        super(properties);
    }
}
