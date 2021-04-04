package net.roguelogix.biggerreactors.multiblocks.heatexchanger.state;

import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerCoolantPortTile;
import net.roguelogix.phosphophyllite.gui.GuiSync;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class HeatExchangerCoolantPortState implements GuiSync.IGUIPacket {

    /**
     * The direction of the port. True for input, false for output.
     */
    public boolean direction = false;

    /**
     * The channel type this is connected to.
     * If true: red for input, blue for output (condenser).
     * If false: blue for input, red for output (evaporator).
     */
    public boolean condenser = false;

    /**
     * The tile whose information this belongs to.
     */
    HeatExchangerCoolantPortTile heatExchangerCoolantPortTile;

    public HeatExchangerCoolantPortState(HeatExchangerCoolantPortTile heatExchangerCoolantPortTile) {
        this.heatExchangerCoolantPortTile = heatExchangerCoolantPortTile;
    }

    @Override
    public void read(@Nonnull Map<?, ?> data) {
        this.direction = (Boolean) data.get("direction");
        this.condenser = (Boolean) data.get("condenser");
    }

    @Override
    @Nullable
    public Map<?, ?> write() {
        this.heatExchangerCoolantPortTile.updateState();
        HashMap<String, Object> data = new HashMap<>();

        data.put("direction", this.direction);
        data.put("condenser", this.condenser);

        return data;
    }
}
