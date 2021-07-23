package net.roguelogix.biggerreactors;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.server.ServerResources;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fmlserverevents.FMLServerStoppedEvent;
import net.roguelogix.biggerreactors.machine.client.CyaniteReprocessorScreen;
import net.roguelogix.biggerreactors.machine.containers.CyaniteReprocessorContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container.HeatExchangerCoolantPortContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container.HeatExchangerTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.screen.HeatExchangerCoolantPortScreen;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.screen.HeatExchangerTerminalScreen;
import net.roguelogix.biggerreactors.multiblocks.reactor.client.*;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.*;
import net.roguelogix.biggerreactors.multiblocks.turbine.client.BladeRenderer;
import net.roguelogix.biggerreactors.multiblocks.turbine.client.TurbineCoolantPortScreen;
import net.roguelogix.biggerreactors.multiblocks.turbine.client.TurbineTerminalScreen;
import net.roguelogix.biggerreactors.multiblocks.turbine.containers.TurbineCoolantPortContainer;
import net.roguelogix.biggerreactors.multiblocks.turbine.containers.TurbineTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineRotorBearingTile;
import net.roguelogix.biggerreactors.registries.FluidTransitionRegistry;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.biggerreactors.registries.TurbineCoilRegistry;
import net.roguelogix.phosphophyllite.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
@Mod(BiggerReactors.modid)
public class BiggerReactors {

    public static final String modid = "biggerreactors";

    public static final Logger LOGGER = LogManager.getLogger();

    public BiggerReactors() {
        new Registry();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onTagsUpdatedEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListenerEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.addListener(this::onRenderWorldLast);
        }

    }

    public static ServerResources dataPackRegistries;

    public void onAddReloadListenerEvent(AddReloadListenerEvent reloadListenerEvent) {
        dataPackRegistries = reloadListenerEvent.getDataPackRegistries();
    }
    
    public void onServerStopped(FMLServerStoppedEvent serverStoppedEvent){
        dataPackRegistries = null;
    }

    public void onTagsUpdatedEvent(final TagsUpdatedEvent.CustomTagTypes tagsUpdatedEvent) {
        if(dataPackRegistries == null){
            return;
        }
        ReactorModeratorRegistry.loadRegistry(tagsUpdatedEvent.getTagManager());
        TurbineCoilRegistry.loadRegistry(tagsUpdatedEvent.getTagManager().getOrEmpty(net.minecraft.core.Registry.BLOCK_REGISTRY));
        FluidTransitionRegistry.loadRegistry(tagsUpdatedEvent.getTagManager().getOrEmpty(net.minecraft.core.Registry.FLUID_REGISTRY));
    }

    public void onClientSetup(final FMLClientSetupEvent e) {
        // TODO: 6/28/20 Registry.
        //  Since I already have the comment here, also need to do a capability registry. I have a somewhat dumb capability to register.
        MenuScreens.register(CyaniteReprocessorContainer.INSTANCE,
                CyaniteReprocessorScreen::new);
        MenuScreens.register(ReactorTerminalContainer.INSTANCE,
                CommonReactorTerminalScreen::new);
        MenuScreens.register(ReactorCoolantPortContainer.INSTANCE,
                ReactorCoolantPortScreen::new);
        MenuScreens.register(ReactorAccessPortContainer.INSTANCE,
                ReactorAccessPortScreen::new);
        MenuScreens.register(ReactorControlRodContainer.INSTANCE,
                ReactorControlRodScreen::new);
        MenuScreens.register(ReactorRedstonePortContainer.INSTANCE,
                ReactorRedstonePortScreen::new);
        MenuScreens.register(TurbineTerminalContainer.INSTANCE,
                TurbineTerminalScreen::new);
        MenuScreens.register(TurbineCoolantPortContainer.INSTANCE,
                TurbineCoolantPortScreen::new);
        MenuScreens.register(HeatExchangerTerminalContainer.INSTANCE,
                HeatExchangerTerminalScreen::new);
        MenuScreens.register(HeatExchangerCoolantPortContainer.INSTANCE,
                HeatExchangerCoolantPortScreen::new);
    
    
        BlockEntityRenderers.register(TurbineRotorBearingTile.TYPE, BladeRenderer::new);
    }

    public static long lastRenderTime = 0;

    public void onRenderWorldLast(RenderWorldLastEvent event) {
        lastRenderTime = System.nanoTime();
    }

}
