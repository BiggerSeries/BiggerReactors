package net.roguelogix.biggerreactors.blocks.materials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.phosphophyllite.registry.IPhosphophylliteOre;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;
import net.roguelogix.phosphophyllite.registry.RegisterOre;


public class YelloriteOre extends Block implements IPhosphophylliteOre {
    
    @RegisterOre
    @RegisterBlock(name = "yellorite_ore")
    public static final YelloriteOre INSTANCE = new YelloriteOre();
    
    public YelloriteOre() {
        super(
                Properties.of(Material.METAL)
                        .sound(SoundType.STONE)
                        .explosionResistance(1.0F)
        );
    }
    
    @Override
    public int size() {
        return Config.WorldGen.YelloriteMaxOrePerCluster;
    }
    
    @Override
    public int count() {
        return Config.WorldGen.YelloriteOreMaxClustersPerChunk;
    }
    
    @Override
    public int maxLevel() {
        return Config.WorldGen.YelloriteOreMaxSpawnY;
    }
    
    @Override
    public String[] spawnBiomes() {
        return new String[0];
    }
    
    @Override
    public boolean doSpawn() {
        return Config.WorldGen.EnableYelloriteGeneration;
    }
}
