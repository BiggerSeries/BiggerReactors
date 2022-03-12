package net.roguelogix.biggerreactors.items.dusts;

import net.minecraft.world.item.Item;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

import javax.annotation.Nonnull;

public class BlutoniumDust extends Item {
    
    @RegisterItem(name = "blutonium_dust")
    public static final BlutoniumDust INSTANCE = new BlutoniumDust(new Item.Properties());
    
    @SuppressWarnings("unused")
    public BlutoniumDust(@Nonnull Properties properties) {
        super(properties);
    }
}