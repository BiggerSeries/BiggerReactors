package net.roguelogix.biggerreactors.items.raw;

import net.minecraft.world.item.Item;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

import javax.annotation.Nonnull;

public class UraniumChunk extends Item {

    @RegisterItem(name = "uranium_chunk")
    public static final UraniumChunk INSTANCE = new UraniumChunk(new Properties());

    @SuppressWarnings("unused")
    public UraniumChunk(@Nonnull Properties properties) {
        super(properties);
    }
}
