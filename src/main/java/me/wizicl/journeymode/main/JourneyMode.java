package me.wizicl.journeymode.main;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.Research;
import me.wizicl.journeymode.capabilities.ResearchStorage;
import me.wizicl.journeymode.init.CommandJourney;
import me.wizicl.journeymode.network.MessageSyncResearch;
import me.wizicl.journeymode.proxy.CommonProxy;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

//Айди мода, его название и его версия, а так же версия майнкрафта
@Mod(modid = JourneyMode.MODID, name = JourneyMode.NAME, version = JourneyMode.VERSION, acceptedMinecraftVersions = JourneyMode.MC_VERSION)

//Основной, открытый класс Main, доступный из любого участка кода
public class JourneyMode {

    public static final String MODID = "journeymode";
    public static final String NAME = "Journey Mode";
    public static final String VERSION = "1.5.0";
    public static final String MC_VERSION = "1.12.2";

    public static Logger logger;
    public static SimpleNetworkWrapper NETWORK;

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandJourney());
    }

    @CapabilityInject(IResearch.class)
    public static Capability<IResearch> RESEARCH = null;

    @SidedProxy(
            clientSide = "me.wizicl.journeymode.proxy.ClientProxy",
            serverSide = "me.wizicl.journeymode.proxy.ServerProxy"
    )

    public static CommonProxy proxy;

    // Пред-инициализация. Регистрирует блоки, предметы и отправку сообщий в лог.
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Создаю конфиги...");
        proxy.preInit(event);

        // Работа с капой
        CapabilityManager.INSTANCE.register(IResearch.class, new ResearchStorage(), Research::new);

        // Работа с сетью
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("journeymode");
        NETWORK.registerMessage(MessageSyncResearch.Handler.class, MessageSyncResearch.class, 0, Side.CLIENT);
    }

    // Инициализация. Загрузка рецептов, событий, сущностей.
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        logger.info("Загружаю рецепты и события...");
    }

    // Пост-инициализация. Совместимость с другими модами.
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        logger.info("Оформляю совместимость с другими модами");
    }
}
