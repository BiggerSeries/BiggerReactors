package net.roguelogix.biggerreactors.items.dusts;

import net.minecraft.world.item.Item;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

import javax.annotation.Nonnull;

@RegisterItem(name = "uranium_dust")
public class UraniumDust extends Item {
    
    @RegisterItem.Instance
    public static UraniumDust INSTANCE;
    
    @SuppressWarnings("unused")
    public UraniumDust(@Nonnull Properties properties) {
        super(properties);
    }
}