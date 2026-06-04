package wizicl.mre;

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

//Айди мода, его название и его версия, а так же версия майнкрафта
@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION)

//Основной, открытый класс Main, доступный из любого участка кода
public class MatterReplicationEngine {

    @Mod.Instance
    public static MatterReplicationEngine instance;

    public static Logger logger;

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandJourney());
    }

    @CapabilityInject(IResearch.class)
    public static Capability<IResearch> RESEARCH = null;

    @SidedProxy(
            clientSide = "wizicl.MatterReplicationEngine.proxy.ClientProxy",
            serverSide = "wizicl.MatterReplicationEngine.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Создаю конфиги...");
        proxy.preInit(event);

        CapabilityManager.INSTANCE.register(IResearch.class, new Capability.IStorage<IResearch>() {
            @Override
            public net.minecraft.nbt.NBTBase writeNBT(Capability<IResearch> capability, IResearch instance, EnumFacing side) {
                return new net.minecraft.nbt.NBTTagCompound(); // Возвращаем
            }

            @Override
            public void readNBT(Capability<IResearch> capability, IResearch instance, EnumFacing side, net.minecraft.nbt.NBTBase nbt) {}
        }, Research::new);

    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        logger.info("Загружаю рецепты и события...");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        logger.info("Оформляю совместимость с другими модами");
    }
}
