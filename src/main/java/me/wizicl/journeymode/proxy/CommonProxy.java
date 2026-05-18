package me.wizicl.journeymode.proxy;

import me.wizicl.journeymode.JourneyMode;
import me.wizicl.journeymode.client.gui.GuiHandler;
import me.wizicl.journeymode.network.MessageOpenResearchGui;
import me.wizicl.journeymode.network.MessageRequestResearch;
import me.wizicl.journeymode.network.MessageSyncResearch;
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
        NetworkRegistry.INSTANCE.registerGuiHandler(JourneyMode.instance, new GuiHandler());
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("journeymode");

        // Серверные пакеты
        NETWORK.registerMessage(MessageRequestResearch.Handler.class, MessageRequestResearch.class, packetId++, Side.SERVER);
        NETWORK.registerMessage(MessageOpenResearchGui.Handler.class, MessageOpenResearchGui.class, packetId++, Side.SERVER);

        // Регистрируем тут! Теперь сервер знает ID пакета и не паникует при отправке
        NETWORK.registerMessage(MessageSyncResearch.Handler.class, MessageSyncResearch.class, packetId++, Side.CLIENT);
    }

    public void init(FMLInitializationEvent e) {}
    public void postInit(FMLPostInitializationEvent e) {}

    // Метод-заглушка. На сервере он ничего не делает
    public void handleSyncResearch(MessageSyncResearch message) {
        // Пусто
    }
}
