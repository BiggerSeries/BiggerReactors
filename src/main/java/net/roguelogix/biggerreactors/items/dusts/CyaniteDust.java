package net.roguelogix.biggerreactors.items.dusts;

import net.minecraft.world.item.Item;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

import javax.annotation.Nonnull;

public class CyaniteDust extends Item {
    
    @RegisterItem(name = "cyanite_dust")
    public static final CyaniteDust INSTANCE = new CyaniteDust(new Item.Properties());
    
    @SuppressWarnings("unused")
    public CyaniteDust(@Nonnull Properties properties) {
        super(properties);
    }
}