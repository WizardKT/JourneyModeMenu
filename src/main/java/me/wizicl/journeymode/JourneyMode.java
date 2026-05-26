package me.wizicl.journeymode;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.Research;
import me.wizicl.journeymode.command.CommandJourney;
import me.wizicl.journeymode.proxy.CommonProxy;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

//Айди мода, его название и его версия, а так же версия майнкрафта
@Mod(modid = JourneyMode.MODID, name = JourneyMode.NAME, version = JourneyMode.VERSION, acceptedMinecraftVersions = JourneyMode.MC_VERSION)

//Основной, открытый класс Main, доступный из любого участка кода
public class JourneyMode {

    @Mod.Instance
    public static JourneyMode instance;

    /// Инициализация переменных для модификации

    public static final String MODID = "journeymode";
    public static final String NAME = "Journey Mode";
    public static final String VERSION = "1.7.0";
    public static final String MC_VERSION = "1.12.2";

    public static Logger logger;

    /// Создание инстанса для сервера и клиента

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandJourney());
    }

    @CapabilityInject(IResearch.class)
    public static Capability<IResearch> RESEARCH = null;

    @SidedProxy(
            clientSide = "me.wizicl.journeymode.proxy.ClientProxy",
            serverSide = "me.wizicl.journeymode.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    /// ====================================================
    /// === Предварительная инициализация мода (preInit) ===
    /// ====================================================

    /** * Предварительная инициализация мода. Здесь мы создаем конфиги и регистрируем все наши блоки, предметы, рецепты и т.д.
     */

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog(); // Создание логера для записи сообщений в консоль.
        logger.info("Создаю конфиги...");
        proxy.preInit(event);

        // Работа с капой
        CapabilityManager.INSTANCE.register(IResearch.class, new Capability.IStorage<IResearch>() {
            @Override
            public net.minecraft.nbt.NBTBase writeNBT(Capability<IResearch> capability, IResearch instance, EnumFacing side) {
                return new net.minecraft.nbt.NBTTagCompound(); // Возвращаем
            }

            @Override
            public void readNBT(Capability<IResearch> capability, IResearch instance, EnumFacing side, net.minecraft.nbt.NBTBase nbt) {}
        }, Research::new);

        // Работа с сетью

    }

    /// ==================================
    /// === Инициализация мода (init) ===
    /// ==================================

    /** * Инициализация мода. Здесь мы регистрируем все наши блоки, предметы, рецепты и т.д.
     */

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        logger.info("Загружаю рецепты и события...");
    }

    /// ==================================
    /// === Пост-инициализация (post-init) ===
    /// ==================================

    /** * Пост-инициализация мода. Здесь мы можем выполнить дополнительные действия после того, как все другие моды инициализировались.
     */

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        logger.info("Оформляю совместимость с другими модами");
    }
}
