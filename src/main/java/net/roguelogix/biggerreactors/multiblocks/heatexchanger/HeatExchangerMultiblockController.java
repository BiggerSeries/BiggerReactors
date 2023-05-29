package net.roguelogix.biggerreactors.multiblocks.heatexchanger;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerChannelTile;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockTileModule;
import net.roguelogix.phosphophyllite.multiblock2.ValidationException;
import net.roguelogix.phosphophyllite.multiblock2.common.IEventMultiblock;
import net.roguelogix.phosphophyllite.multiblock2.common.IPersistentMultiblock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IRectangularMultiblock;
import net.roguelogix.phosphophyllite.multiblock2.touching.ITouchingMultiblock;
import net.roguelogix.phosphophyllite.util.HeatBody;
import net.roguelogix.phosphophyllite.util.Util;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static net.roguelogix.phosphophyllite.modular.block.IConnectedTexture.Module.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerMultiblockController extends MultiblockController<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController> implements
        IRectangularMultiblock<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController>,
        IPersistentMultiblock<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController>,
        ITouchingMultiblock<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController>,
        IEventMultiblock<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController> {
    
    public HeatExchangerMultiblockController(Level level) {
        super(level, HeatExchangerBaseTile.class, HeatExchangerBaseBlock.class);
    }
    
    public final Set<HeatExchangerChannelTile> condenserChannels = new LinkedHashSet<>();
    public final Set<HeatExchangerChannelTile> evaporatorChannels = new LinkedHashSet<>();
    private final Set<HeatExchangerFluidPortTile> fluidPorts = new LinkedHashSet<>();
    
    @Nullable
    @Override
    public Vector3ic minSize() {
        return new Vector3i(4, 3, 3);
    }
    
    @Nullable
    @Override
    public Vector3ic maxSize() {
        return new Vector3i(Config.CONFIG.HeatExchanger.MaxLength, Config.CONFIG.HeatExchanger.MaxHeight, Config.CONFIG.HeatExchanger.MaxWidth);
    }
    
    @Override
    public boolean allowedInteriorBlock(Block block) {
        return block instanceof AirBlock;
    }
    
    @Override
    public void validateStage1() throws ValidationException {
        if (condenserChannels.isEmpty() || evaporatorChannels.isEmpty()) {
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.heat_exchanger.missing_channel_type"));
        }
        
        if (fluidPorts.size() != 4) {
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.heat_exchanger.invalid_port_count"));
        }
    }
    
    @Override
    public void validateStage2() throws ValidationException {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        
        int condenserPorts = 0;
        int evaporatorPorts = 0;
        
        for (HeatExchangerFluidPortTile fluidPort : fluidPorts) {
            BlockPos portPos = fluidPort.getBlockPos();
            boolean channelFound = false;
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(portPos);
                mutableBlockPos.move(value);
                BlockEntity tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerChannelTile channelTile) {
                    if (channelTile.CONDENSER) {
                        condenserPorts++;
                    } else {
                        evaporatorPorts++;
                    }
                    channelFound = true;
                    break;
                }
            }
            if (!channelFound) {
                throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.heat_exchanger.fluid_port_unconnected", portPos.getX(), portPos.getY(), portPos.getZ()));
            }
        }
        if (condenserPorts != 2 || evaporatorPorts != 2) {
            // technically this isn't the problem im checking, but this is a secondary check that happens, without having to march the channels
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.heat_exchanger.duplicate_port_types"));
        }
        
        verifyFluidChannels(condenserChannels);
        verifyFluidChannels(evaporatorChannels);
        
        long tick = Phosphophyllite.tickNumber();
        
        // *not that i dont have to march them anyway*
        for (HeatExchangerFluidPortTile fluidPort : fluidPorts) {
            if (fluidPort.lastCheckedTick == tick) {
                continue;
            }
            Direction nextDirection = null;
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(fluidPort.getBlockPos());
                mutableBlockPos.move(value);
                HeatExchangerBaseTile tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerChannelTile) {
                    nextDirection = value;
                    break;
                }
            }
            mutableBlockPos.set(fluidPort.getBlockPos());
            if (nextDirection == null) {
                throw new ValidationException("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
            }
            MultiblockTileModule<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController> currentModule = fluidPort.multiblockModule();
            while (true) {
                mutableBlockPos.move(nextDirection);
                currentModule = currentModule.getNeighbor(nextDirection);
                if (currentModule == null) {
                    throw new ValidationException("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
                }
                HeatExchangerBaseTile channelTile = currentModule.iface;
                if (channelTile instanceof HeatExchangerFluidPortTile) {
                    break;
                }
                if (!(channelTile instanceof HeatExchangerChannelTile)) {
                    throw new ValidationException("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
                }
                if (channelTile instanceof HeatExchangerChannelTile) {
                    ((HeatExchangerChannelTile) channelTile).lastCheckedTick = tick;
                }
                BlockState channelState = channelTile.getBlockState();
                if (nextDirection != Direction.DOWN && channelState.getValue(TOP_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.UP;
                    continue;
                }
                if (nextDirection != Direction.UP && channelState.getValue(BOTTOM_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.DOWN;
                    continue;
                }
                
                if (nextDirection != Direction.SOUTH && channelState.getValue(NORTH_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.NORTH;
                    continue;
                }
                if (nextDirection != Direction.NORTH && channelState.getValue(SOUTH_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.SOUTH;
                    continue;
                }
                
                if (nextDirection != Direction.WEST && channelState.getValue(EAST_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.EAST;
                    continue;
                }
                if (nextDirection != Direction.EAST && channelState.getValue(WEST_CONNECTED_PROPERTY)) {
                    nextDirection = Direction.WEST;
                    continue;
                }
                throw new ValidationException("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
            }
        }
        
        for (HeatExchangerChannelTile condenserChannel : condenserChannels) {
            if (condenserChannel.lastCheckedTick != tick) {
                BlockPos channelPos = condenserChannel.getBlockPos();
                throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.heat_exchanger.dangling_channel", channelPos.getX(), channelPos.getY(), channelPos.getZ()));
            }
        }
        
        for (HeatExchangerChannelTile evaporatorChannel : evaporatorChannels) {
            if (evaporatorChannel.lastCheckedTick != tick) {
                BlockPos channelPos = evaporatorChannel.getBlockPos();
                throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.heat_exchanger.dangling_channel", channelPos.getX(), channelPos.getY(), channelPos.getZ()));
            }
        }
    }
    
    @Override
    public void validateStage3() throws ValidationException {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        Util.chunkCachedBlockStateIteration(min(), max(), level, (block, pos) -> {
            if (block.getBlock() instanceof HeatExchangerBaseBlock) {
                mutableBlockPos.set(pos.x, pos.y, pos.z);
                if (!blocks.containsPos(mutableBlockPos)) {
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.heat_exchanger.dangling_internal_part", pos.x, pos.y, pos.z));
                }
            }
        });
    }
    
    private void verifyFluidChannels(Set<? extends HeatExchangerBaseTile> channels) throws ValidationException {
        for (HeatExchangerBaseTile tile : channels) {
            BlockState state = tile.getBlockState();
            int connectedSides = 0;
            connectedSides += state.getValue(TOP_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(BOTTOM_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(NORTH_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(SOUTH_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(EAST_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(WEST_CONNECTED_PROPERTY) ? 1 : 0;
            
            if (connectedSides != 2) {
                throw new ValidationException("all fluid channels must have exactly two connections " + tile.getBlockPos());
            }
        }
    }

    @Override
    protected void onPartAdded(HeatExchangerBaseTile toAttach) {
        if (toAttach instanceof HeatExchangerChannelTile channelTile) {
            if (channelTile.CONDENSER) {
                condenserChannels.add(channelTile);
            } else {
                evaporatorChannels.add(channelTile);
            }
        }
        if (toAttach instanceof HeatExchangerFluidPortTile) {
            fluidPorts.add((HeatExchangerFluidPortTile) toAttach);
        }
    }
    
    @Override
    protected void onPartRemoved(HeatExchangerBaseTile toDetach) {
        if (toDetach instanceof HeatExchangerChannelTile) {
            condenserChannels.remove(toDetach);
        }
        if (toDetach instanceof HeatExchangerChannelTile channelTile) {
            if (channelTile.CONDENSER) {
                condenserChannels.remove(channelTile);
            } else {
                evaporatorChannels.remove(channelTile);
            }
        }
        if (toDetach instanceof HeatExchangerFluidPortTile) {
            fluidPorts.remove((HeatExchangerFluidPortTile) toDetach);
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
    public void onAssembled() {
        validationPassed();
    }
    
    @Override
    public void onReassembled() {
        validationPassed();
    }
    
    private void validationPassed() {
        
        // its in kelvin, 150C and 20C
        double ambientTemperature = level.dimensionType().ultraWarm() ? 423.15 : 293.15; // TODO config these, also the end
        ambientHeatBody.setTemperature(ambientTemperature);
        airHeatBody.setTemperature(ambientTemperature);
        condenserHeatBody.setTemperature(ambientTemperature);
        evaporatorHeatBody.setTemperature(ambientTemperature);
        
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        condenserHeatBody.setRfPerKelvin(condenserChannels.size() * Config.CONFIG.HeatExchanger.ChannelFEPerKelvinUnitVolume);
        evaporatorHeatBody.setRfPerKelvin(evaporatorChannels.size() * Config.CONFIG.HeatExchanger.ChannelFEPerKelvinUnitVolume);

        condenserTank.perSideCapacity = condenserChannels.size() * Config.CONFIG.HeatExchanger.ChannelTankVolumePerBlock;
        evaporatorTank.perSideCapacity = evaporatorChannels.size() * Config.CONFIG.HeatExchanger.ChannelTankVolumePerBlock;

        Vector3i vec = new Vector3i(max()).sub(min()).add(1, 1, 1);
        int airVolume = vec.x * vec.y * vec.z;
        airVolume -= condenserChannels.size();
        airVolume -= evaporatorChannels.size();
        airHeatBody.setRfPerKelvin(airVolume * Config.CONFIG.HeatExchanger.AirFEPerKelvinUnitVolume);

        int channelContactArea = 0;
        int evaporatorAirContactArea = 0;
        int condenserAirContactArea = 0;
        int airAmbientContactArea = 0;
        
        for (HeatExchangerChannelTile condenserChannel : condenserChannels) {
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(condenserChannel.getBlockPos());
                mutableBlockPos.move(value);
                HeatExchangerBaseTile tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerChannelTile channelTile && !channelTile.CONDENSER) {
                    channelContactArea++;
                } else if (!(tile instanceof HeatExchangerChannelTile)) {
                    condenserAirContactArea++;
                }
            }
        }

        for (HeatExchangerChannelTile condenserChannel : evaporatorChannels) {
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(condenserChannel.getBlockPos());
                mutableBlockPos.move(value);
                HeatExchangerBaseTile tile = blocks.getTile(mutableBlockPos);
                if (!(tile instanceof HeatExchangerChannelTile)) {
                    evaporatorAirContactArea++;
                }
            }
        }

        airAmbientContactArea = vec.x * vec.y + vec.x * vec.z + vec.y * vec.z;
        airAmbientContactArea *= 2;

        channelRFKT = channelContactArea * Config.CONFIG.HeatExchanger.ChannelFEPerKelvinMetreSquared;
        condenserAirRFKT = condenserAirContactArea * Config.CONFIG.HeatExchanger.AirFEPerKelvinMetreSquared;
        evaporatorAirRFKT = evaporatorAirContactArea * Config.CONFIG.HeatExchanger.AirFEPerKelvinMetreSquared;
        airAmbientRFKT = airAmbientContactArea * Config.CONFIG.HeatExchanger.AmbientFEPerKelvinMetreSquared;

        for (HeatExchangerFluidPortTile coolantPort : fluidPorts) {
            BlockPos portPos = coolantPort.getBlockPos();
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(portPos);
                mutableBlockPos.move(value);
                BlockEntity tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerChannelTile channelTile) {
                    coolantPort.setHETank(channelTile.CONDENSER ? condenserTank : evaporatorTank);
                    coolantPort.setCondenser(channelTile.CONDENSER);
                    break;
                }
            }
        }

        // ensure that we only have one inlet and one outlet port for each channel type
        // if you already have them configured, this wont modify your settings
        for (HeatExchangerFluidPortTile coolantPort : fluidPorts) {
            for (HeatExchangerFluidPortTile port : fluidPorts) {
                if (port.isCondenser() == coolantPort.isCondenser() && port != coolantPort) {
                    port.setInlet(!coolantPort.isInlet());
                }
            }
        }

        ambientHeatBody.setInfinite(true);
    }

    @Override
    public void tick() {
        condenserTank.transferWith(condenserHeatBody, condenserChannels.size() * Config.CONFIG.HeatExchanger.ChannelInternalSurfaceArea);
        condenserHeatBody.transferWith(airHeatBody, condenserAirRFKT);
        condenserHeatBody.transferWith(evaporatorHeatBody, channelRFKT);
        evaporatorHeatBody.transferWith(airHeatBody, evaporatorAirRFKT);
        evaporatorTank.transferWith(evaporatorHeatBody, evaporatorChannels.size() * Config.CONFIG.HeatExchanger.ChannelInternalSurfaceArea);
        airHeatBody.transferWith(ambientHeatBody, airAmbientRFKT);
        fluidPorts.forEach(HeatExchangerFluidPortTile::pushFluid);
        if (Phosphophyllite.tickNumber() % 2 == 0) {
            dirty();
        }
    }
    
    @Override
    public CompoundTag mergeNBTs(CompoundTag nbtA, CompoundTag nbtB) {
        // TODO: this, as a whole, this
        return nbtA;
    }
    
    @Override
    public void read(CompoundTag nbt) {
        condenserTank.deserializeNBT(nbt.getCompound("condenserTank"));
        evaporatorTank.deserializeNBT(nbt.getCompound("evaporatorTank"));
        ambientHeatBody.setTemperature(nbt.getDouble("ambientHeatBody"));
        airHeatBody.setTemperature(nbt.getDouble("airHeatBody"));
        condenserHeatBody.setTemperature(nbt.getDouble("condenserHeatBody"));
        evaporatorHeatBody.setTemperature(nbt.getDouble("evaporatorHeatBody"));
    }

    @Nonnull
    @Override
    public CompoundTag write() {
        final var nbt = new CompoundTag();
        nbt.put("condenserTank", condenserTank.serializeNBT());
        nbt.put("evaporatorTank", evaporatorTank.serializeNBT());
        nbt.putDouble("ambientHeatBody", ambientHeatBody.temperature());
        nbt.putDouble("airHeatBody", airHeatBody.temperature());
        nbt.putDouble("condenserHeatBody", condenserHeatBody.temperature());
        nbt.putDouble("evaporatorHeatBody", evaporatorHeatBody.temperature());
        return nbt;
    }

    public void setInletPort(HeatExchangerFluidPortTile port, boolean inlet) {
        port.setInlet(inlet);
        for (HeatExchangerFluidPortTile coolantPort : fluidPorts) {
            if (coolantPort != port && coolantPort.isCondenser() == port.isCondenser()) {
                coolantPort.setInlet(!inlet);
            }
        }
    }

    public void runRequest(String requestName, @Nullable Object requestData) {
        switch (requestName) {
            // Manually dump tanks.
            case "dumpTanks" -> {
                if (!(requestData instanceof Boolean)) {
                    return;
                }
                // Condenser true, evaporator false
                if((Boolean) requestData) {
                    condenserTank.dumpTank(FluidTransitionTank.IN_TANK);
                    condenserTank.dumpTank(FluidTransitionTank.OUT_TANK);
                } else {
                    evaporatorTank.dumpTank(FluidTransitionTank.IN_TANK);
                    evaporatorTank.dumpTank(FluidTransitionTank.OUT_TANK);
                }
            }
        }
    }
}
