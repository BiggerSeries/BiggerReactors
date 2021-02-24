package net.roguelogix.biggerreactors.classic.reactor.state;

import net.roguelogix.biggerreactors.classic.reactor.tiles.ReactorTerminalTile;
import net.roguelogix.phosphophyllite.gui.GuiSync;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ReactorState implements GuiSync.IGUIPacket {

    /**
     * The activity status for the reactor.
     */
    public ReactorActivity reactorActivity = ReactorActivity.INACTIVE;
    /**
     * The type of reactor.
     */
    public ReactorType reactorType = ReactorType.PASSIVE;

    /**
     * Is auto-ejection of waste enabled.
     */
    public boolean doAutoEject = false;

    /**
     * The amount of energy stored in the reactor.
     */
    public long energyStored = 0;
    /**
     * The max energy capacity of the reactor.
     */
    public long energyCapacity = 0;

    /**
     * The amount of waste stored in the reactor.
     */
    public long wasteStored = 0;
    /**
     * The amount of fuel stored in the reactor.
     */
    public long fuelStored = 0;
    /**
     * The max fuel capacity of the reactor.
     */
    public long fuelCapacity = 0;

    /**
     * The temperature of the fuel in the reactor.
     */
    public double fuelHeatStored = 0;
    /**
     * The temperature of the case of the reactor.
     */
    public double caseHeatStored = 0;

    /**
     * The rate at which reactions occur (per tick).
     */
    public double reactivityRate = 0;
    /**
     * The rate at which fuel is consumed (per tick).
     */
    public double fuelUsageRate = 0;
    /**
     * Output rate of the reactor (RF for passive, Steam for active).
     */
    public double reactorOutputRate = 0;

    /**
     * [Active-Only] The amount of coolant stored in the reactor.
     */
    public long coolantStored = 0;
    /**
     * [Active-Only] The max coolant capacity of the reactor.
     */
    public long coolantCapacity = 0;

    /**
     * [Active-Only] The type of coolant being stored.
     */
    public String coolantResourceLocation = "";

    /**
     * [Active-Only] The amount of hot exhaust stored in the reactor.
     */
    public long exhaustStored = 0;
    /**
     * [Active-Only] The max hot exhaust capacity of the reactor.
     */
    public long exhaustCapacity = 0;

    /**
     * [Active-Only] The type of exhaust being generated.
     */
    public String exhaustResourceLocation = "";

    /**
     * The tile whose information this belongs to.
     */
    ReactorTerminalTile reactorTerminalTile;

    public ReactorState(ReactorTerminalTile reactorTerminalTile) {
        this.reactorTerminalTile = reactorTerminalTile;
    }

    @Override
    public void read(@Nonnull Map<?, ?> data) {
        reactorActivity = ReactorActivity.fromInt((Integer) data.get("reactorActivity"));
        reactorType = ReactorType.fromInt((Integer) data.get("reactorType"));

        doAutoEject = (Boolean) data.get("doAutoEject");

        energyStored = (Long) data.get("energyStored");
        energyCapacity = (Long) data.get("energyCapacity");

        wasteStored = (Long) data.get("wasteStored");
        fuelStored = (Long) data.get("fuelStored");
        fuelCapacity = (Long) data.get("fuelCapacity");

        coolantStored = (Long) data.get("coolantStored");
        coolantCapacity = (Long) data.get("coolantCapacity");
        coolantResourceLocation = (String) data.get("coolantResourceLocation");

        exhaustStored = (Long) data.get("exhaustStored");
        exhaustCapacity = (Long) data.get("exhaustCapacity");
        exhaustResourceLocation = (String) data.get("exhaustResourceLocation");

        caseHeatStored = (Double) data.get("caseHeatStored");
        fuelHeatStored = (Double) data.get("fuelHeatStored");

        reactivityRate = (Double) data.get("reactivityRate");
        fuelUsageRate = (Double) data.get("fuelUsageRate");
        reactorOutputRate = (Double) data.get("reactorOutputRate");
    }

    @Nullable
    @Override
    public Map<?, ?> write() {
        reactorTerminalTile.updateState();
        HashMap<String, Object> data = new HashMap<>();
        data.put("reactorActivity", reactorActivity.toInt());
        data.put("reactorType", reactorType.toInt());

        data.put("doAutoEject", doAutoEject);

        data.put("energyStored", energyStored);
        data.put("energyCapacity", energyCapacity);

        data.put("wasteStored", wasteStored);
        data.put("fuelStored", fuelStored);
        data.put("fuelCapacity", fuelCapacity);

        data.put("coolantStored", coolantStored);
        data.put("coolantCapacity", coolantCapacity);
        data.put("coolantResourceLocation", coolantResourceLocation);

        data.put("exhaustStored", exhaustStored);
        data.put("exhaustCapacity", exhaustCapacity);
        data.put("exhaustResourceLocation", exhaustResourceLocation);

        data.put("caseHeatStored", caseHeatStored);
        data.put("fuelHeatStored", fuelHeatStored);

        data.put("reactivityRate", reactivityRate);
        data.put("fuelUsageRate", fuelUsageRate);
        data.put("reactorOutputRate", reactorOutputRate);

        return data;
    }
}
