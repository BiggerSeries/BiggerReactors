package net.roguelogix.biggerreactors.items.dusts;

import net.minecraft.world.item.Item;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

import javax.annotation.Nonnull;

public class UraniumDust extends Item {
    
    @RegisterItem(name = "uranium_dust")
    public static final UraniumDust INSTANCE = new UraniumDust(new Properties());
    
    @SuppressWarnings("unused")
    public UraniumDust(@Nonnull Properties properties) {
        super(properties);
    }
}