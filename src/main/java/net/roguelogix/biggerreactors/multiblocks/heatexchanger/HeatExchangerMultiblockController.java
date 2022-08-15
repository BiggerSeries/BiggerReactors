package net.roguelogix.biggerreactors.multiblocks.heatexchanger;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerCasingBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerCondenserChannelTile;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerEvaporatorChannelTile;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.multiblock.MultiblockTileModule;
import net.roguelogix.phosphophyllite.multiblock.ValidationError;
import net.roguelogix.phosphophyllite.multiblock.Validator;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockController;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.util.HeatBody;
import net.roguelogix.phosphophyllite.util.Util;

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
public class HeatExchangerMultiblockController extends RectangularMultiblockController<HeatExchangerBaseTile, HeatExchangerMultiblockController> {

    public HeatExchangerMultiblockController(Level world) {
        super(world, tile -> tile instanceof HeatExchangerBaseTile, block -> block instanceof HeatExchangerBaseBlock);
        minSize.set(4, 3, 3);
        maxSize.set(Config.CONFIG.HeatExchanger.MaxLength, Config.CONFIG.HeatExchanger.MaxHeight, Config.CONFIG.HeatExchanger.MaxWidth);
        setAssemblyValidator(HeatExchangerMultiblockController::validate);
        frameValidator = block -> block instanceof HeatExchangerCasingBlock;
        exteriorValidator = Validator.or(frameValidator, block -> false);
        interiorValidator = block -> block instanceof AirBlock;
    }

    public final Set<HeatExchangerCondenserChannelTile> condenserChannels = new LinkedHashSet<>();
    public final Set<HeatExchangerEvaporatorChannelTile> evaporatorChannels = new LinkedHashSet<>();
    private final Set<HeatExchangerFluidPortTile> fluidPorts = new LinkedHashSet<>();

    private boolean validate() {
        //TODO lang file the errors

        if (condenserChannels.isEmpty() || evaporatorChannels.isEmpty()) {
            throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.heat_exchanger.missing_channel_type"));
        }

        if (fluidPorts.size() != 4) {
            throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.heat_exchanger.invalid_port_count"));
        }

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
                if (tile instanceof HeatExchangerEvaporatorChannelTile) {
                    evaporatorPorts++;
                    channelFound = true;
                    break;
                } else if (tile instanceof HeatExchangerCondenserChannelTile) {
                    condenserPorts++;
                    channelFound = true;
                    break;
                }
            }
            if (!channelFound) {
                throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.heat_exchanger.fluid_port_unconnected", portPos.getX(), portPos.getY(), portPos.getZ()));
            }
        }
        if (condenserPorts != 2 || evaporatorPorts != 2) {
            // technically this isn't the problem im checking, but this is a secondary check that happens, without having to march the channels
            throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.heat_exchanger.duplicate_port_types"));
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
                if (tile instanceof HeatExchangerCondenserChannelTile || tile instanceof HeatExchangerEvaporatorChannelTile) {
                    nextDirection = value;
                    break;
                }
            }
            mutableBlockPos.set(fluidPort.getBlockPos());
            if (nextDirection == null) {
                throw new ValidationError("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
            }
            MultiblockTileModule<HeatExchangerBaseTile, HeatExchangerMultiblockController> currentModule = fluidPort.multiblockModule();
            while (true) {
                mutableBlockPos.move(nextDirection);
                currentModule = currentModule.getNeighbor(nextDirection);
                if (currentModule == null) {
                    throw new ValidationError("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
                }
                HeatExchangerBaseTile channelTile = currentModule.iface;
                if (channelTile instanceof HeatExchangerFluidPortTile) {
                    break;
                }
                if (!(channelTile instanceof HeatExchangerCondenserChannelTile || channelTile instanceof HeatExchangerEvaporatorChannelTile)) {
                    throw new ValidationError("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
                }
                if (channelTile instanceof HeatExchangerCondenserChannelTile) {
                    ((HeatExchangerCondenserChannelTile) channelTile).lastCheckedTick = tick;
                }
                if (channelTile instanceof HeatExchangerEvaporatorChannelTile) {
                    ((HeatExchangerEvaporatorChannelTile) channelTile).lastCheckedTick = tick;
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
                throw new ValidationError("Unknown channel verification error, this shouldn't be possible " + mutableBlockPos);
            }
        }

        for (HeatExchangerCondenserChannelTile condenserChannel : condenserChannels) {
            if (condenserChannel.lastCheckedTick != tick) {
                BlockPos channelPos = condenserChannel.getBlockPos();
                throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.heat_exchanger.dangling_channel", channelPos.getX(), channelPos.getY(), channelPos.getZ()));
            }
        }

        for (HeatExchangerEvaporatorChannelTile evaporatorChannel : evaporatorChannels) {
            if (evaporatorChannel.lastCheckedTick != tick) {
                BlockPos channelPos = evaporatorChannel.getBlockPos();
                throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.heat_exchanger.dangling_channel", channelPos.getX(), channelPos.getY(), channelPos.getZ()));
            }
        }

        Util.chunkCachedBlockStateIteration(minCoord(), maxCoord(), world, (block, pos) -> {
            if (block.getBlock() instanceof HeatExchangerBaseBlock) {
                mutableBlockPos.set(pos.x, pos.y, pos.z);
                if (!blocks.containsPos(mutableBlockPos)) {
                    throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.heat_exchanger.dangling_internal_part", pos.x, pos.y, pos.z));
                }
            }
        });

        return true;
    }

    private void verifyFluidChannels(Set<? extends HeatExchangerBaseTile> channels) {
        channels.forEach(tile -> {
            BlockState state = tile.getBlockState();
            int connectedSides = 0;
            connectedSides += state.getValue(TOP_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(BOTTOM_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(NORTH_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(SOUTH_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(EAST_CONNECTED_PROPERTY) ? 1 : 0;
            connectedSides += state.getValue(WEST_CONNECTED_PROPERTY) ? 1 : 0;

            if (connectedSides != 2) {
                throw new ValidationError("all fluid channels must have exactly two connections " + tile.getBlockPos());
            }
        });
    }

    @Override
    protected void onPartPlaced(HeatExchangerBaseTile placed) {
        onPartAttached(placed);
    }

    @Override
    protected void onPartAttached(HeatExchangerBaseTile toAttach) {
        if (toAttach instanceof HeatExchangerCondenserChannelTile) {
            condenserChannels.add((HeatExchangerCondenserChannelTile) toAttach);
        }
        if (toAttach instanceof HeatExchangerEvaporatorChannelTile) {
            evaporatorChannels.add((HeatExchangerEvaporatorChannelTile) toAttach);
        }
        if (toAttach instanceof HeatExchangerFluidPortTile) {
            fluidPorts.add((HeatExchangerFluidPortTile) toAttach);
        }
    }

    @Override
    protected void onPartBroken(HeatExchangerBaseTile broken) {
        onPartDetached(broken);
    }

    @Override
    protected void onPartDetached(HeatExchangerBaseTile toDetach) {
        if (toDetach instanceof HeatExchangerCondenserChannelTile) {
            condenserChannels.remove(toDetach);
        }
        if (toDetach instanceof HeatExchangerEvaporatorChannelTile) {
            evaporatorChannels.remove(toDetach);
        }
        if (toDetach instanceof HeatExchangerFluidPortTile) {
            fluidPorts.remove(toDetach);
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
        // its in kelvin, 150C and 20C
        double ambientTemperature = world.dimensionType().ultraWarm() ? 423.15 : 293.15; // TODO config these, also the end
        ambientHeatBody.setTemperature(ambientTemperature);
        airHeatBody.setTemperature(ambientTemperature);
        condenserHeatBody.setTemperature(ambientTemperature);
        evaporatorHeatBody.setTemperature(ambientTemperature);
    }

    @Override
    protected void onValidationPassed() {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        condenserHeatBody.setRfPerKelvin(condenserChannels.size() * Config.CONFIG.HeatExchanger.ChannelFEPerKelvinUnitVolume);
        evaporatorHeatBody.setRfPerKelvin(evaporatorChannels.size() * Config.CONFIG.HeatExchanger.ChannelFEPerKelvinUnitVolume);

        condenserTank.perSideCapacity = condenserChannels.size() * Config.CONFIG.HeatExchanger.ChannelTankVolumePerBlock;
        evaporatorTank.perSideCapacity = evaporatorChannels.size() * Config.CONFIG.HeatExchanger.ChannelTankVolumePerBlock;

        Vector3i vec = new Vector3i(maxCoord()).sub(minCoord()).add(1, 1, 1);
        int airVolume = vec.x * vec.y * vec.z;
        airVolume -= condenserChannels.size();
        airVolume -= evaporatorChannels.size();
        airHeatBody.setRfPerKelvin(airVolume * Config.CONFIG.HeatExchanger.AirFEPerKelvinUnitVolume);

        int channelContactArea = 0;
        int evaporatorAirContactArea = 0;
        int condenserAirContactArea = 0;
        int airAmbientContactArea = 0;

        for (HeatExchangerCondenserChannelTile condenserChannel : condenserChannels) {
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(condenserChannel.getBlockPos());
                mutableBlockPos.move(value);
                HeatExchangerBaseTile tile = blocks.getTile(mutableBlockPos);
                if (tile instanceof HeatExchangerEvaporatorChannelTile) {
                    channelContactArea++;
                } else if (!(tile instanceof HeatExchangerCondenserChannelTile)) {
                    condenserAirContactArea++;
                }
            }
        }

        for (HeatExchangerEvaporatorChannelTile condenserChannel : evaporatorChannels) {
            for (Direction value : Direction.values()) {
                mutableBlockPos.set(condenserChannel.getBlockPos());
                mutableBlockPos.move(value);
                HeatExchangerBaseTile tile = blocks.getTile(mutableBlockPos);
                if (!(tile instanceof HeatExchangerCondenserChannelTile) && !(tile instanceof HeatExchangerEvaporatorChannelTile)) {
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
                if (tile instanceof HeatExchangerEvaporatorChannelTile) {
                    coolantPort.setHETank(evaporatorTank);
                    coolantPort.setCondenser(false);
                    break;
                } else if (tile instanceof HeatExchangerCondenserChannelTile) {
                    coolantPort.setHETank(condenserTank);
                    coolantPort.setCondenser(true);
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
            markDirty();
        }
    }

    @Override
    protected void read(CompoundTag nbt) {
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
    protected CompoundTag write() {
        CompoundTag nbt = super.write();
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
