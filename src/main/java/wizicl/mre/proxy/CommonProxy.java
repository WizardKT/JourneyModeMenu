package wizicl.mre.proxy;

import wizicl.mre.MRE;
import wizicl.mre.gui.controller.GuiHandler;
import wizicl.mre.network.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = "journeymode")
public class CommonProxy {

    public static SimpleNetworkWrapper NETWORK;
    public static int packetId = 0;

    public void preInit(FMLPreInitializationEvent e) {
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("journeymode");

        // Серверные пакеты
        NETWORK.registerMessage(MessageOpenResearchGui.Handler.class, MessageOpenResearchGui.class, packetId++, Side.SERVER);
        NETWORK.registerMessage(MessageGuiAction.Handler.class, MessageGuiAction.class, packetId++, Side.SERVER);
        NETWORK.registerMessage(MessageToggleAutoResearch.Handler.class, MessageToggleAutoResearch.class, packetId++, Side.SERVER);

        // Клиентские пакеты
        NETWORK.registerMessage(MessageSyncResearch.Handler.class, MessageSyncResearch.class, packetId++, Side.CLIENT);
        NETWORK.registerMessage(MessageSyncSingleResearch.Handler.class, MessageSyncSingleResearch.class, packetId++, Side.CLIENT);
    }

    public void init(FMLInitializationEvent e) {
        NetworkRegistry.INSTANCE.registerGuiHandler(MRE.instance, new GuiHandler());
    }
    public void postInit(FMLPostInitializationEvent e) {}

    // Метод-заглушка. На сервере он ничего не делает
    public void handleSyncResearch(MessageSyncResearch message) {
        // Пусто
    }
    public void handleSyncSingleResearch(MessageSyncSingleResearch message) {
        // Пусто
    }
}
