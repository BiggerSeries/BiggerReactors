package net.roguelogix.biggerreactors.classic.reactor.simulation;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;
import net.roguelogix.biggerreactors.Config;

import javax.annotation.Nonnull;

public class CoolantTank implements INBTSerializable<CompoundNBT> {
    
    private long perSideCapacity = 0;
    private long liquidAmount = 0;
    private long vaporAmount = 0;
    
    private long vaporizationEnergy = 4;
    private double boilingPoint = 100;
    
    private long vaporizedLastTick = 0;
    private long maxVaporizedLastTick = 0;
    
    public long getFluidVaporizedLastTick() {
        return vaporizedLastTick;
    }
    
    public long getMaxFluidVaporizedLastTick() {
        return maxVaporizedLastTick;
    }
    
    public long getVaporAmount() {
        return vaporAmount;
    }
    
    public long getLiquidAmount() {
        return liquidAmount;
    }
    
    public long getPerSideCapacity() {
        return perSideCapacity;
    }
    
    public void setPerSideCapacity(long capacity) {
        perSideCapacity = capacity;
    }
    
    public void setVaporizationEnergy(long vaporizationEnergy) {
        this.vaporizationEnergy = vaporizationEnergy;
    }
    
    public void setBoilingPoint(double boilingPoint) {
        this.boilingPoint = boilingPoint;
    }
    
    public void voidLiquid(){
        liquidAmount = 0;
    }
    
    public void voidVapor(){
        vaporAmount = 0;
    }
    
    public double absorbHeat(double rfTransferred) {
        vaporizedLastTick = 0;
        if (liquidAmount <= 0 || rfTransferred <= 0) {
            return rfTransferred;
        }
        
        long amountVaporized = (long) (rfTransferred / vaporizationEnergy);
        maxVaporizedLastTick = amountVaporized;
        
        amountVaporized = Math.min(liquidAmount, amountVaporized);
        amountVaporized = Math.min(amountVaporized, perSideCapacity - vaporAmount);
        
        if (amountVaporized < 1) {
            return rfTransferred;
        }
        
        vaporizedLastTick = amountVaporized;
        liquidAmount -= amountVaporized;
        vaporAmount += amountVaporized;
        
        double energyUsed = amountVaporized * vaporizationEnergy;
        
        return Math.max(0, rfTransferred - energyUsed);
    }
    
    public double getCoolantTemperature(double reactorHeat) {
        if (liquidAmount <= 0) {
            return reactorHeat;
        }
        return Math.min(reactorHeat, boilingPoint);
    }
    
    public long insertLiquid(long amount, boolean simulated) {
        amount = Math.min(perSideCapacity - liquidAmount, amount);
        if (!simulated) {
            liquidAmount += amount;
        }
        return amount;
    }
    
    public long extractVapor(long amount, boolean simulated) {
        amount = Math.min(vaporAmount, amount);
        if (!simulated) {
            vaporAmount -= amount;
        }
        return amount;
    }
    
    @Override
    @Nonnull
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putLong("perSideCapacity", perSideCapacity);
        nbt.putLong("liquidAmount", liquidAmount);
        nbt.putLong("vaporAmount", vaporAmount);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(@Nonnull CompoundNBT nbt) {
        if (nbt.contains("perSideCapacity")) {
            perSideCapacity = nbt.getLong("perSideCapacity");
        }
        if (nbt.contains("liquidAmount")) {
            liquidAmount = nbt.getLong("liquidAmount");
        } else if (nbt.contains("waterAmount")) {
            // cant go breaking worlds, unfortunately
            liquidAmount = nbt.getLong("waterAmount");
        }
        
        if (nbt.contains("vaporAmount")) {
            vaporAmount = nbt.getLong("vaporAmount");
        } else if (nbt.contains("steamAmount")) {
            vaporAmount = nbt.getLong("steamAmount");
        }
    }
}
