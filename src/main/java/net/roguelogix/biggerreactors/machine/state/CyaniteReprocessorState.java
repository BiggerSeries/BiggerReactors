//package net.roguelogix.biggerreactors.machine.state;
//
//import net.roguelogix.biggerreactors.machine.tiles.CyaniteReprocessorTile;
//import net.roguelogix.phosphophyllite.client.gui.GuiSync;
//
//import javax.annotation.Nonnull;
//import javax.annotation.Nullable;
//import java.util.HashMap;
//import java.util.Map;
//
//public class CyaniteReprocessorState implements GuiSync.IGUIPacket {
//
//    /**
//     * The number of ticks the current item has processed for.
//     */
//    public int workTime = 0;
//    /**
//     * The total amount of time required to process the item.
//     */
//    public int workTimeTotal = 0;
//
//    /**
//     * The amount of energy stored in the machine.
//     */
//    public int energyStored = 0;
//    /**
//     * The max energy capacity of the machine.
//     */
//    public int energyCapacity = 0;
//
//    /**
//     * The amount of water stored in the machine.
//     */
//    public int waterStored = 0;
//    /**
//     * The max water capacity of the machine.
//     */
//    public int waterCapacity = 0;
//
//    /**
//     * The tile whose information this belongs to.
//     */
//    CyaniteReprocessorTile cyaniteReprocessorTile;
//
//    public CyaniteReprocessorState(CyaniteReprocessorTile cyaniteReprocessorTile) {
//        this.cyaniteReprocessorTile = cyaniteReprocessorTile;
//    }
//
//    @Override
//    public void read(@Nonnull Map<?, ?> data) {
//        this.workTime = (Integer) data.get("workTime");
//        this.workTimeTotal = (Integer) data.get("workTimeTotal");
//        this.energyStored = (Integer) data.get("energyStored");
//        this.energyCapacity = (Integer) data.get("energyCapacity");
//        this.waterStored = (Integer) data.get("waterStored");
//        this.waterCapacity = (Integer) data.get("waterCapacity");
//    }
//
//    @Nullable
//    @Override
//    public Map<?, ?> write() {
//        this.cyaniteReprocessorTile.updateState();
//        HashMap<String, Object> data = new HashMap<>();
//        data.put("workTime", this.workTime);
//        data.put("workTimeTotal", this.workTimeTotal);
//        data.put("energyStored", this.energyStored);
//        data.put("energyCapacity", this.energyCapacity);
//        data.put("waterStored", this.waterStored);
//        data.put("waterCapacity", this.waterCapacity);
//        return data;
//    }
//}
