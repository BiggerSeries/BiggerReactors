package net.roguelogix.biggerreactors;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.roguelogix.biggerreactors.machine.client.CyaniteReprocessorScreen;
import net.roguelogix.biggerreactors.machine.containers.CyaniteReprocessorContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.client.HeatExchangerFluidPortScreen;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.client.HeatExchangerTerminalScreen;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.containers.HeatExchangerFluidPortContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.containers.HeatExchangerTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.client.*;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.*;
import net.roguelogix.biggerreactors.multiblocks.turbine.client.TurbineFluidPortScreen;
import net.roguelogix.biggerreactors.multiblocks.turbine.client.TurbineTerminalScreen;
import net.roguelogix.biggerreactors.multiblocks.turbine.containers.TurbineFluidPortContainer;
import net.roguelogix.biggerreactors.multiblocks.turbine.containers.TurbineTerminalContainer;
import net.roguelogix.biggerreactors.registries.FluidTransitionRegistry;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.biggerreactors.registries.TurbineCoilRegistry;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.event.ReloadDataEvent;
import net.roguelogix.phosphophyllite.registry.Registry;
import net.roguelogix.quartz.Quartz;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
@Mod(BiggerReactors.modid)
public class BiggerReactors {

    public static final String modid = "biggerreactors";

    public static final Logger LOGGER = LogManager.getLogger();
    public static final boolean LOG_DEBUG = LOGGER.isDebugEnabled();
    
    public BiggerReactors() {
        new Registry(new ReferenceArrayList<>(), ReferenceArrayList.of(new ResourceLocation(Phosphophyllite.modid, "creative_tab"), new ResourceLocation(Quartz.modid, "creative_tab")));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onReloadData);
        version = FMLLoader.getLoadingModList().getModFileById(modid).versionString();
    }
    
    public void onReloadData(final ReloadDataEvent reloadDataEvent) {
        ReactorModeratorRegistry.loadRegistry();
        TurbineCoilRegistry.loadRegistry();
        FluidTransitionRegistry.loadRegistry();
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
        MenuScreens.register(TurbineFluidPortContainer.INSTANCE,
                TurbineFluidPortScreen::new);
        MenuScreens.register(HeatExchangerTerminalContainer.INSTANCE,
                HeatExchangerTerminalScreen::new);
        MenuScreens.register(HeatExchangerFluidPortContainer.INSTANCE,
                HeatExchangerFluidPortScreen::new);
    }
    
    private static String version;
    public static String modVersion(){
        return version;
    }
}
