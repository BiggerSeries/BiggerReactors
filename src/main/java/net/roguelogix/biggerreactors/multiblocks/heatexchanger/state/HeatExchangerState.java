package net.roguelogix.biggerreactors.multiblocks.heatexchanger.state;

import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerTerminalTile;
import net.roguelogix.phosphophyllite.client.gui.GuiSync;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class HeatExchangerState implements GuiSync.IGUIPacket {

    /**
     * The fluid entering the condenser channels. This should match the value of the fluid's ResourceLocation.
     * This fluid should be hot; its cold (output) variant should match condenserExhaustFluid.
     */
    public String condenserIntakeFluid = "minecraft:empty";
    
    /**
     * How much fluid is stored in each side of the condenser.
     */
    public long condenserTankSize = 0;
    
    /**
     * How much hot (input) fluid is stored in the condenser.
     */
    public long condenserIntakeFluidAmount = 0;

    /**
     * The fluid exiting the condenser channels. This should match the value of the fluid's ResourceLocation.
     * This fluid should be cold; its hot (input) variant should match condenserIntakeFluid.
     */
    public String condenserExhaustFluid = "minecraft:empty";

    /**
     * How much cold (output) fluid is stored in the condenser.
     */
    public long condenserExhaustFluidAmount = 0;

    /**
     * Temperature for the condenser channels.
     */
    public double condenserChannelTemperature = 0;

    /**
     * Flow rate for the condenser channels.
     */
    public double condenserChannelFlowRate = 0;

    /**
     * The fluid entering the evaporator channels. This should match the value of the fluid's ResourceLocation.
     * This fluid should be cold; its hot (output) variant should match evaporatorExhaustFluid.
     */
    public String evaporatorIntakeFluid = "minecraft:empty";
    
    /**
     * How much fluid is stored in each side of the evaporator.
     */
    public long evaporatorTankSize = 0;
    
    /**
     * How much cold (input) fluid is stored in the evaporator.
     */
    public long evaporatorIntakeFluidAmount = 0;

    /**
     * The fluid exiting the evaporator channels. This should match the value of the fluid's ResourceLocation.
     * This fluid should be hot; its cold (input) variant should match evaporatorIntakeFluid.
     */
    public String evaporatorExhaustFluid = "minecraft:empty";

    /**
     * How much hot (output) fluid is stored in the evaporator.
     */
    public long evaporatorExhaustFluidAmount = 0;

    /**
     * Temperature for the evaporator channels.
     */
    public double evaporatorChannelTemperature = 0;

    /**
     * Flow rate for the evaporator channels.
     */
    public double evaporatorChannelFlowRate = 0;

    /**
     * The tile whose information this belongs to.
     */
    HeatExchangerTerminalTile heatExchangerTerminalTile;

    public HeatExchangerState(HeatExchangerTerminalTile heatExchangerTerminalTile) {
        this.heatExchangerTerminalTile = heatExchangerTerminalTile;
        this.heatExchangerTerminalTile.updateState();
    }

    @Override
    public void read(@Nonnull Map<?, ?> data) {
        this.condenserTankSize = (Long) data.get("condenserTankSize");
        
        this.condenserIntakeFluid = (String) data.get("condenserIntakeFluid");
        this.condenserIntakeFluidAmount = (Long) data.get("condenserIntakeFluidAmount");

        this.condenserExhaustFluid = (String) data.get("condenserExhaustFluid");
        this.condenserExhaustFluidAmount = (Long) data.get("condenserExhaustFluidAmount");

        this.condenserChannelTemperature = (Double) data.get("condenserChannelTemperature");
        this.condenserChannelFlowRate = (Double) data.get("condenserChannelFlowRate");
    
        this.evaporatorTankSize = (Long) data.get("evaporatorTankSize");
        
        this.evaporatorIntakeFluid = (String) data.get("evaporatorIntakeFluid");
        this.evaporatorIntakeFluidAmount = (Long) data.get("evaporatorIntakeFluidAmount");

        this.evaporatorExhaustFluid = (String) data.get("evaporatorExhaustFluid");
        this.evaporatorExhaustFluidAmount = (Long) data.get("evaporatorExhaustFluidAmount");

        this.evaporatorChannelTemperature = (Double) data.get("evaporatorChannelTemperature");
        this.evaporatorChannelFlowRate = (Double) data.get("evaporatorChannelFlowRate");
    }

    @Nullable
    @Override
    public Map<?, ?> write() {
        this.heatExchangerTerminalTile.updateState();
        HashMap<String, Object> data = new HashMap<>();

        data.put("condenserTankSize", this.condenserTankSize);
        
        data.put("condenserIntakeFluid", this.condenserIntakeFluid);
        data.put("condenserIntakeFluidAmount", this.condenserIntakeFluidAmount);

        data.put("condenserExhaustFluid", this.condenserExhaustFluid);
        data.put("condenserExhaustFluidAmount", this.condenserExhaustFluidAmount);

        data.put("condenserChannelTemperature", this.condenserChannelTemperature);
        data.put("condenserChannelFlowRate", this.condenserChannelFlowRate);

        data.put("evaporatorTankSize", this.evaporatorTankSize);
        
        data.put("evaporatorIntakeFluid", this.evaporatorIntakeFluid);
        data.put("evaporatorIntakeFluidAmount", this.evaporatorIntakeFluidAmount);

        data.put("evaporatorExhaustFluid", this.evaporatorExhaustFluid);
        data.put("evaporatorExhaustFluidAmount", this.evaporatorExhaustFluidAmount);

        data.put("evaporatorChannelTemperature", this.evaporatorChannelTemperature);
        data.put("evaporatorChannelFlowRate", this.evaporatorChannelFlowRate);
        return data;
    }
}
