package net.roguelogix.biggerreactors.multiblocks.turbine.state;

import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineFluidPortTile;
import net.roguelogix.phosphophyllite.client.gui.GuiSync;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class TurbineFluidPortState implements GuiSync.IGUIPacket {

    /**
     * The direction of the port. True for input, false for output.
     */
    public boolean direction = false;

    /**
     * The tile whose information this belongs to.
     */
    TurbineFluidPortTile turbineFluidPortTile;

    public TurbineFluidPortState(TurbineFluidPortTile turbineFluidPortTile) {
        this.turbineFluidPortTile = turbineFluidPortTile;
    }

    @Override
    public void read(@Nonnull Map<?, ?> data) {
        direction = (Boolean) data.get("direction");
    }

    @Override
    @Nullable
    public Map<?, ?> write() {
        turbineFluidPortTile.updateState();
        HashMap<String, Object> data = new HashMap<>();

        data.put("direction", direction);

        return data;
    }
}
