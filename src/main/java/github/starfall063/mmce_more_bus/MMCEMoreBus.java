package github.starfall063.mmce_more_bus;

import github.starfall063.mmce_more_bus.init.ModBlocks;
import github.starfall063.mmce_more_bus.module.mmce.me.MEItemInventoryNetwork;
import github.starfall063.mmce_more_bus.proxy.CommonProxy;
import github.starfall063.mmce_more_bus.tile.MEItemInventoryInputBusConfig;
import github.starfall063.mmce_more_bus.tile.MEUniversalOutputBusConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public final class MMCEMoreBus {
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    public static final String CLIENT_PROXY = "github.starfall063.mmce_more_bus.proxy.ClientProxy";
    public static final String SERVER_PROXY = "github.starfall063.mmce_more_bus.proxy.CommonProxy";

    @Mod.Instance(Tags.MOD_ID)
    public static MMCEMoreBus instance;

    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = SERVER_PROXY)
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        ModBlocks.init();
        MEItemInventoryNetwork.init();
        MEItemInventoryInputBusConfig.init();
        MEUniversalOutputBusConfig.init();
        proxy.construction();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }
}

