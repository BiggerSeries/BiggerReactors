package net.roguelogix.biggerreactors.multiblocks.heatexchanger;

import net.minecraft.block.AirBlock;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.classic.reactor.blocks.ReactorBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerCasingBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerEvaporatorChannelTile;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerCoolantPortTile;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerCondensorChannelTile;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;
import net.roguelogix.phosphophyllite.multiblock.generic.ValidationError;
import net.roguelogix.phosphophyllite.multiblock.generic.Validator;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockController;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.util.HeatBody;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nonnull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static net.roguelogix.phosphophyllite.multiblock.generic.ConnectedTextureStates.*;

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
    
    public final Set<HeatExchangerCondensorChannelTile> condenserChannels = new LinkedHashSet<>();
    public final Set<HeatExchangerEvaporatorChannelTile> evaporatorChannels = new LinkedHashSet<>();
    private final Set<HeatExchangerCoolantPortTile> coolantPorts = new LinkedHashSet<>();
    
    private boolean validate() {
        //TODO lang file the errors
        
        if (condenserChannels.isEmpty() || evaporatorChannels.isEmpty()) {
            throw new ValidationError("at least one of each coolant channel type is required //TODO lang file this");
        }
        
        if (coolantPorts.size() != 4) {
            throw new ValidationError("heat exchangers require exactly 4 coolant ports //TODO lang file this");
        }
        
        BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
        
        int condenserPorts = 0;
        int evaporatorPorts = 0;
        
        for (HeatExchangerCoolantPortTile coolantPort : coolantPorts) {
            BlockPos portPos = coolantPort.getPos();
            boolean channelFound = false;
            for (Direction value : Direction.values()) {
                mutableBlockPos.setPos(portPos);
                mutableBlockPos.move(value);
                TileEntity tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerEvaporatorChannelTile) {
                    evaporatorPorts++;
                    channelFound = true;
                    break;
                } else if (tile instanceof HeatExchangerCondensorChannelTile) {
                    condenserPorts++;
                    channelFound = true;
                    break;
                }
            }
            if (!channelFound) {
                throw new ValidationError("all coolant ports must be connected to a coolant channel //TODO lang file this");
            }
        }
        if (condenserPorts != 2 || evaporatorPorts != 2) {
            // technically this isn't the problem im checking, but this is a secondary check that happens, without having to march the channels
            throw new ValidationError("all coolant channels must terminate at a coolant port //TODO lang file this");
        }
        
        verifyFluidChannels(condenserChannels);
        verifyFluidChannels(evaporatorChannels);
        
        Util.chunkCachedBlockStateIteration(minCoord(), maxCoord(), world, (block, pos) -> {
            if (block.getBlock() instanceof ReactorBaseBlock) {
                mutableBlockPos.setPos(pos.x, pos.y, pos.z);
                if (!blocks.containsPos(mutableBlockPos)) {
                    throw new ValidationError(new TranslationTextComponent("multiblock.error.biggerreactors.dangling_internal_part", pos.x, pos.y, pos.z));
                }
            }
        });
        
        return true;
    }
    
    private void verifyFluidChannels(Set<? extends HeatExchangerBaseTile> channels) {
        channels.forEach(tile -> {
            BlockState state = tile.getBlockState();
            int connectedSides = 0;
            connectedSides += state.get(TOP_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.get(BOTTOM_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.get(NORTH_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.get(SOUTH_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.get(EAST_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.get(WEST_CONNECTED_PROPERTY) ? 1 : 0;
            
            if (connectedSides != 2) {
                throw new ValidationError("all fluid channels must have exactly two connections " + tile.getPos());
            }
        });
    }
    
    @Override
    protected void onPartPlaced(@Nonnull HeatExchangerBaseTile placed) {
        onPartAttached(placed);
    }
    
    @Override
    protected void onPartAttached(@Nonnull HeatExchangerBaseTile toAttach) {
        if (toAttach instanceof HeatExchangerCondensorChannelTile) {
            condenserChannels.add((HeatExchangerCondensorChannelTile) toAttach);
        }
        if (toAttach instanceof HeatExchangerEvaporatorChannelTile) {
            evaporatorChannels.add((HeatExchangerEvaporatorChannelTile) toAttach);
        }
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
        if (toDetach instanceof HeatExchangerCondensorChannelTile) {
            condenserChannels.remove(toDetach);
        }
        if (toDetach instanceof HeatExchangerEvaporatorChannelTile) {
            evaporatorChannels.remove(toDetach);
        }
        if (toDetach instanceof HeatExchangerCoolantPortTile) {
            coolantPorts.remove(toDetach);
        }
    }
    
    public final ReadWriteLock locks = new ReentrantReadWriteLock();
    
    public final FluidTransitionTank evaporatorTank = new FluidTransitionTank(false);
    public final FluidTransitionTank condenserTank = new FluidTransitionTank(true);
    
    public final HeatBody ambientHeatBody = new HeatBody();
    public final HeatBody airHeatBody = new HeatBody();
    public final HeatBody condenserHeatBody = new HeatBody();
    public final HeatBody evaporatorHeatBody = new HeatBody();
    
    public double channelRFKT;
    public double evaporatorAirRFKT;
    public double condenserAirRFKT;
    public double airAmbientRFKT;
    
    @Override
    protected void onAssembled() {
        onUnpaused();
        // its in kelvin, 150C and 20C
        double ambientTemperature = world.getDimensionType().isUltrawarm() ? 423.15 : 293.15; // TODO config these, also the end
        ambientHeatBody.setTemperature(ambientTemperature);
        airHeatBody.setTemperature(ambientTemperature);
        condenserHeatBody.setTemperature(ambientTemperature);
        evaporatorHeatBody.setTemperature(ambientTemperature);
    }
    
    @Override
    protected void onUnpaused() {
        BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
        
        condenserHeatBody.setRfPerKelvin(condenserChannels.size() * Config.HeatExchanger.ChannelFEPerKelvinUnitVolume);
        evaporatorHeatBody.setRfPerKelvin(evaporatorChannels.size() * Config.HeatExchanger.ChannelFEPerKelvinUnitVolume);
    
        condenserTank.perSideCapacity = condenserChannels.size() * Config.HeatExchanger.ChannelTankVolumePerBlock;
        evaporatorTank.perSideCapacity = evaporatorChannels.size() * Config.HeatExchanger.ChannelTankVolumePerBlock;
    
        Vector3i vec = new Vector3i(maxCoord()).sub(minCoord()).add(1, 1, 1);
        int airVolume = vec.x * vec.y * vec.z;
        airVolume -= condenserChannels.size();
        airVolume -= evaporatorChannels.size();
        airHeatBody.setRfPerKelvin(airVolume * Config.HeatExchanger.AirFEPerKelvinUnitVolume);
        
        int channelContactArea = 0;
        int evaporatorAirContactArea = 0;
        int condenserAirContactArea = 0;
        int airAmbientContactArea = 0;
        
        for (HeatExchangerCondensorChannelTile condenserChannel : condenserChannels) {
            for (Direction value : Direction.values()) {
                mutableBlockPos.setPos(condenserChannel.getPos());
                mutableBlockPos.move(value);
                HeatExchangerBaseTile tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerEvaporatorChannelTile) {
                    channelContactArea++;
                } else if (!(tile instanceof HeatExchangerCondensorChannelTile)) {
                    condenserAirContactArea++;
                }
            }
        }
        
        for (HeatExchangerEvaporatorChannelTile condenserChannel : evaporatorChannels) {
            for (Direction value : Direction.values()) {
                mutableBlockPos.setPos(condenserChannel.getPos());
                mutableBlockPos.move(value);
                HeatExchangerBaseTile tile = blocks.getTile(mutableBlockPos);
                if (!(tile instanceof HeatExchangerCondensorChannelTile) && !(tile instanceof HeatExchangerEvaporatorChannelTile)) {
                    evaporatorAirContactArea++;
                }
            }
        }
        
        airAmbientContactArea = vec.x * vec.y + vec.x * vec.z + vec.y * vec.z;
        airAmbientContactArea *= 2;
        
        channelRFKT = channelContactArea * Config.HeatExchanger.ChannelFEPerKelvinMetreSquared;
        condenserAirRFKT = condenserAirContactArea * Config.HeatExchanger.AirFEPerKelvinMetreSquared;
        evaporatorAirRFKT = evaporatorAirContactArea * Config.HeatExchanger.AirFEPerKelvinMetreSquared;
        airAmbientRFKT = airAmbientContactArea * Config.HeatExchanger.AmbientFEPerKelvinMetreSquared;
        
        for (HeatExchangerCoolantPortTile coolantPort : coolantPorts) {
            BlockPos portPos = coolantPort.getPos();
            for (Direction value : Direction.values()) {
                mutableBlockPos.setPos(portPos);
                mutableBlockPos.move(value);
                TileEntity tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerEvaporatorChannelTile) {
                    coolantPort.setHETank(evaporatorTank);
                    coolantPort.setCondenser(false);
                    break;
                } else if (tile instanceof HeatExchangerCondensorChannelTile) {
                    coolantPort.setHETank(condenserTank);
                    coolantPort.setCondenser(true);
                    break;
                }
            }
        }
    
        // ensure that we only have one inlet and one outlet port for each channel type
        // if you already have them configured, this wont modify your settings
        for (HeatExchangerCoolantPortTile coolantPort : coolantPorts) {
            for (HeatExchangerCoolantPortTile port : coolantPorts) {
                if(port.isCondenser() == coolantPort.isCondenser() && port != coolantPort){
                    port.setInlet(!coolantPort.isInlet());
                }
            }
        }
    
        ambientHeatBody.setInfinite(true);
    }
    
    @Override
    public void tick() {
        condenserTank.transferWith(condenserHeatBody, condenserChannels.size() * 4);
        condenserHeatBody.transferWith(airHeatBody, condenserAirRFKT);
        condenserHeatBody.transferWith(evaporatorHeatBody, channelRFKT);
        evaporatorHeatBody.transferWith(airHeatBody, evaporatorAirRFKT);
        evaporatorTank.transferWith(evaporatorHeatBody, evaporatorChannels.size() * 4);
        airHeatBody.transferWith(ambientHeatBody, airAmbientRFKT);
        coolantPorts.forEach(HeatExchangerCoolantPortTile::pushFluid);
    }
    
    
    @Override
    protected void read(@Nonnull CompoundNBT nbt) {
        super.read(nbt);
        condenserTank.deserializeNBT(nbt.getCompound("condenserTank"));
        evaporatorTank.deserializeNBT(nbt.getCompound("evaporatorTank"));
        ambientHeatBody.setTemperature(nbt.getDouble("ambientHeatBody"));
        airHeatBody.setTemperature(nbt.getDouble("airHeatBody"));
        condenserHeatBody.setTemperature(nbt.getDouble("condenserHeatBody"));
        evaporatorHeatBody.setTemperature(nbt.getDouble("evaporatorHeatBody"));
        channelRFKT = nbt.getDouble("channelRFKT");
        evaporatorAirRFKT = nbt.getDouble("evaporatorAirRFKT");
        condenserAirRFKT = nbt.getDouble("condenserAirRFKT");
        airAmbientRFKT = nbt.getDouble("airAmbientRFKT");
    }
    
    @Nonnull
    @Override
    protected CompoundNBT write() {
        CompoundNBT nbt = super.write();
        nbt.put("condenserTank", condenserTank.serializeNBT());
        nbt.put("evaporatorTank", evaporatorTank.serializeNBT());
        nbt.putDouble("ambientHeatBody", ambientHeatBody.temperature());
        nbt.putDouble("airHeatBody", airHeatBody.temperature());
        nbt.putDouble("condenserHeatBody", condenserHeatBody.temperature());
        nbt.putDouble("evaporatorHeatBody", evaporatorHeatBody.temperature());
        nbt.putDouble("channelRFKT", channelRFKT);
        nbt.putDouble("evaporatorAirRFKT", evaporatorAirRFKT);
        nbt.putDouble("condenserAirRFKT", condenserAirRFKT);
        nbt.putDouble("airAmbientRFKT", airAmbientRFKT);
        return nbt;
    }
    
    public void setInletPort(HeatExchangerCoolantPortTile port, boolean inlet) {
        port.setInlet(inlet);
        for (HeatExchangerCoolantPortTile coolantPort : coolantPorts) {
            if (coolantPort != port && coolantPort.isCondenser() == port.isCondenser()) {
                coolantPort.setInlet(!inlet);
            }
        }
    }
}
