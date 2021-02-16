package net.roguelogix.biggerreactors.multiblocks.heatexchanger;

import net.minecraft.block.AirBlock;
import net.minecraft.world.World;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerCasingBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile;
import net.roguelogix.phosphophyllite.multiblock.generic.Validator;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockController;

import javax.annotation.Nonnull;

public class HeatExchangerMultiblockController extends RectangularMultiblockController<HeatExchangerMultiblockController, HeatExchangerBaseTile, HeatExchangerBaseBlock> {
    public HeatExchangerMultiblockController(@Nonnull World world) {
        super(world, tile -> tile instanceof HeatExchangerBaseTile, block -> block instanceof HeatExchangerBaseBlock);
        minSize.set(4, 3, 3);
        maxSize.set(-1, -1, -1);
        setAssemblyValidator(HeatExchangerMultiblockController::validate);
        frameValidator = block -> block instanceof HeatExchangerCasingBlock;
        exteriorValidator = Validator.or(frameValidator, block -> false);
        interiorValidator = block -> block instanceof AirBlock;
    }
    

    private final Set<HeatExchangerCoolantPortTile> coolantPorts = new LinkedHashSet<>();
    private boolean validate() {
        if (coolantPorts.size() != 4) {
            throw new ValidationError("heat exchangers require exactly 4 coolant ports //TODO lang file this");
        }
        
        return true;
    }
    @Override
    protected void onPartPlaced(@Nonnull HeatExchangerBaseTile placed) {
        onPartAttached(placed);
    }
    
    @Override
    protected void onPartAttached(@Nonnull HeatExchangerBaseTile toAttach) {
        if (toAttach instanceof HeatExchangerCoolantPortTile) {
            coolantPorts.add((HeatExchangerCoolantPortTile) toAttach);
        }
    }
    
    @Override
    protected void onPartBroken(@Nonnull HeatExchangerBaseTile broken) {
        onPartDetached(broken);
    }
    
    @Override
    protected void onPartDetached(@Nonnull HeatExchangerBaseTile toDetach) {
        if (toDetach instanceof HeatExchangerCoolantPortTile) {
            coolantPorts.remove(toDetach);
        }
    }
    
    public void setInletPort(HeatExchangerCoolantPortTile port, boolean inlet) {
        port.setInlet(inlet);
        for (HeatExchangerCoolantPortTile coolantPort : coolantPorts) {
            if(coolantPort != port && coolantPort.isCondenser() == port.isCondenser()){
                coolantPort.setInlet(!inlet);
            }
        }
    }
}
