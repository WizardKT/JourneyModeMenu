package wizicl.mre;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import wizicl.mre.Reference;
import wizicl.mre.capabilities.IResearch;
import wizicl.mre.capabilities.Research;
import wizicl.mre.command.CommandJourney;
import wizicl.mre.proxy.CommonProxy;
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

/// Этот класс является основным классом мода Matter Replication Engine (MRE).
// Он содержит методы для инициализации, настройки и обработки сообщений.
@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION)
public class MatterReplicationEngine {

    /// Задаём instance для мода.
    // Это используется для доступа к моду из других частей кода.
    @Mod.Instance
    public static MatterReplicationEngine instance;

    /// Объявляем логгер для отладки и информации.
    // Это позволяет выводить сообщения в лог игры или консоль.
    // А так же объявлением логера мы используем его в различных частях кода мода.
    public static Logger logger;

    /// Объявляем Capability для исследования.
    // Это позволяет другим модам и системам взаимодействовать с исследованием.
    @CapabilityInject(IResearch.class)
    public static Capability<IResearch> RESEARCH = null;

    /// Объявляем SidedProxy для отдельной обработки клиентского и серверного кода.
    // Это позволяет разделять логику между клиентской и серверной стороной.
    @SidedProxy(
            clientSide = "wizicl.mre.proxy.ClientProxy",
            serverSide = "wizicl.mre.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    /// Регистрируем пред-инициализацию мода. Она выполняется перед загрузкой конфигурации.
    // Эта стадия используется для регистрации предметов, блоков и других элементов.
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Начинаю инициализацию мода...");
        proxy.preInit(event);

        CapabilityManager.INSTANCE.register(IResearch.class, new Capability.IStorage<>() {
            @Override
            public NBTBase writeNBT(Capability<IResearch> capability, IResearch instance, EnumFacing side) {
                return new NBTTagCompound();
            }

            @Override
            public void readNBT(Capability<IResearch> capability, IResearch instance, EnumFacing side, NBTBase nbt) {
            }
        }, Research::new);

    }

    /// Регистрируем инициализацию мода. Она выполняется после загрузки конфигурации.
    // Эта стадия используется для настройки параметров и подключения к сетям событий.
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        logger.info("Загружаю рецепты и события...");
    }

    /// Регистрируем пост-инициализацию. Она выполняется после загрузки всех рецептов и событий.
    // Эта стадия используется для настройки взаимодействия с другими модами и выполнения дополнительных задач.
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        logger.info("Оформляю совместимость с другими модами");
    }

    /// Регистрируем запуска сервера.
    // Эта стадия используется для регистрации команд и выполнения других задач, связанных с сервером.
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandJourney());
    }
}
