package quantumstudios.honed;

import net.minecraft.block.Block;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import quantumstudios.honed.Tags;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.event.RegistryEvent;
import quantumstudios.honed.network.HonedNetworkManager;
import quantumstudios.honed.proxy.CommonProxy;
import quantumstudios.honed.registry.HonedBlocks;
import quantumstudios.honed.registry.HonedGUIs;
import quantumstudios.honed.registry.HonedItems;
import quantumstudios.honed.data.loader.JsonDataLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.item.Item;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class Honed {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.Instance
    private static Honed instance;
    public static Honed instance() {
        return instance;
    }

    @SidedProxy(
            serverSide = CommonProxy.SERVER_PROXY,
            clientSide = CommonProxy.CLIENT_PROXY
    )
    private static CommonProxy sidedProxy;
    public static CommonProxy proxy() {
        return sidedProxy;
    }

    public static final FMLEventChannel NETWORK = NetworkRegistry.INSTANCE.newEventDrivenChannel(Tags.MOD_ID);

    /**
     * <a href="https://cleanroommc.com/wiki/forge-mod-development/event#overview">
     *     Take a look at how many FMLStateEvents you can listen to via the @Mod.EventHandler annotation here
     * </a>
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Hello From {}!", Tags.MOD_NAME);
        NETWORK.register(new HonedNetworkManager());
        JsonDataLoader.setConfigPath(event.getModConfigurationDirectory().toPath().resolve("honed"));
        JsonDataLoader.load();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new HonedGUIs.Handler());
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        HonedBlocks.register(event);
        HonedBlocks.registerTEs();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerTESRs(RegistryEvent.Register<Block> event) {
        HonedBlocks.registerTESRs();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        HonedItems.register(event);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        HonedItems.registerModels();
    }
}
