package wizicl.mre.proxy;

import wizicl.mre.MatterReplicationEngine;
import wizicl.mre.Reference;
import wizicl.mre.client.gui.controller.GuiHandler;
import wizicl.mre.network.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class CommonProxy {

    public static SimpleNetworkWrapper NETWORK;
    private static int packetId = 0;

    public void preInit(FMLPreInitializationEvent e) {
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);

        // Серверные пакеты
        regServer(MessageOpenResearchGui.Handler.class, MessageOpenResearchGui.class);
        regServer(MessageGuiAction.Handler.class, MessageGuiAction.class);
        regServer(MessageToggleAutoResearch.Handler.class, MessageToggleAutoResearch.class);

        // Клиентские пакеты
        regClient(MessageSyncResearch.Handler.class, MessageSyncResearch.class);
        regClient(MessageSyncSingleResearch.Handler.class, MessageSyncSingleResearch.class);
    }

    public void init(FMLInitializationEvent e) {
        NetworkRegistry.INSTANCE.registerGuiHandler(MatterReplicationEngine.instance, new GuiHandler());
    }

    public void postInit(FMLPostInitializationEvent e) {}

    public void handleSyncResearch(MessageSyncResearch message) {}
    public void handleSyncSingleResearch(MessageSyncSingleResearch message) {}

    private <REQ extends net.minecraftforge.fml.common.network.simpleimpl.IMessage,
            REPLY extends net.minecraftforge.fml.common.network.simpleimpl.IMessage> void regServer(Class<?
            extends net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler<REQ, REPLY>> handler, Class<REQ> messageType) {
        NETWORK.registerMessage(handler, messageType, packetId++, Side.SERVER);
    }

    private <REQ extends net.minecraftforge.fml.common.network.simpleimpl.IMessage,
            REPLY extends net.minecraftforge.fml.common.network.simpleimpl.IMessage> void regClient(Class<?
            extends net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler<REQ, REPLY>> handler, Class<REQ> messageType) {
        NETWORK.registerMessage(handler, messageType, packetId++, Side.CLIENT);
    }
}
