package net.roguelogix.biggerreactors.items.ingots;

import net.minecraft.world.item.Item;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

import javax.annotation.Nonnull;

@RegisterItem(name = "uranium_ingot")
public class UraniumIngot extends Item {
    
    @RegisterItem.Instance
    public static UraniumIngot INSTANCE;
    
    @SuppressWarnings("unused")
    public UraniumIngot(@Nonnull Properties properties) {
        super(properties);
    }
}
