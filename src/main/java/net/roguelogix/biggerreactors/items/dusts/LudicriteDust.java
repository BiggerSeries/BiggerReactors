package net.roguelogix.biggerreactors.items.dusts;

import net.minecraft.world.item.Item;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

import javax.annotation.Nonnull;


public class LudicriteDust extends Item {
    
    @RegisterItem(name = "ludicrite_dust")
    public static final LudicriteDust INSTANCE = new LudicriteDust(new Properties());
    
    @SuppressWarnings("unused")
    public LudicriteDust(@Nonnull Properties properties) {
        super(properties);
    }
}