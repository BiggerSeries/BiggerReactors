package net.roguelogix.biggerreactors.items.dusts;

import net.minecraft.world.item.Item;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

import javax.annotation.Nonnull;

public class GraphiteDust extends Item {
    
    @RegisterItem(name = "graphite_dust")
    public static final GraphiteDust INSTANCE = new GraphiteDust(new Properties());
    
    @SuppressWarnings("unused")
    public GraphiteDust(@Nonnull Properties properties) {
        super(properties);
    }
}