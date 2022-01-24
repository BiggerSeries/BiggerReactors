package net.roguelogix.biggerreactors.blocks.materials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.phosphophyllite.registry.IPhosphophylliteOre;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;
import net.roguelogix.phosphophyllite.registry.RegisterOre;


public class UraniumOre extends Block implements IPhosphophylliteOre {
    
    @RegisterOre
    @RegisterBlock(name = "uranium_ore")
    public static final UraniumOre INSTANCE = new UraniumOre();
    
    public UraniumOre() {
        super(
                Properties.of(Material.METAL)
                        .sound(SoundType.STONE)
                        .explosionResistance(1.0F)
                        .destroyTime(2.0f)
						.requiresCorrectToolForDrops()
        );
    }
    
    @Override
    public int size() {
        return Config.CONFIG.WorldGen.UraniumMaxOrePerCluster;
    }
    
    @Override
    public int count() {
        return Config.CONFIG.WorldGen.UraniumOreMaxClustersPerChunk;
    }
    
    @Override
    public int maxLevel() {
        return Config.CONFIG.WorldGen.UraniumOreMaxSpawnY;
    }
    
    @Override
    public String[] spawnBiomes() {
        return new String[0];
    }
    
    @Override
    public boolean doSpawn() {
        return Config.CONFIG.WorldGen.EnableUraniumGeneration;
    }
}
