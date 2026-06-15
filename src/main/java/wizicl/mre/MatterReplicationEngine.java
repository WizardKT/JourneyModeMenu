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

//Айди мода, его название и его версия, а так же версия майнкрафт
@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION)
public class MatterReplicationEngine {

    @Mod.Instance
    public static MatterReplicationEngine instance;

    public static Logger logger;

    @CapabilityInject(IResearch.class)
    public static Capability<IResearch> RESEARCH = null;

    @SidedProxy(
            clientSide = "wizicl.mre.proxy.ClientProxy",
            serverSide = "wizicl.mre.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

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

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        logger.info("Загружаю рецепты и события...");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        logger.info("Оформляю совместимость с другими модами");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandJourney());
    }
}
