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

/// Основной потокообразующий класс, являющимся серверным и клиентским провайдером.
// Этот класс отвечает за инициализацию сети, регистрацию пакетов и GUI-обработку.
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class CommonProxy {

    // Объявляем основной обработчик сети Minecraft, а так же универсальное айди для пакетов.
    // Айди сделано так, чтобы не было несовместимости с другими модами и пакетами.
    public static SimpleNetworkWrapper NETWORK;
    private static int packetId = 0;

    /// Пред-инициализация мода.
    // Отвечает за регистрацию пакетов.
    public void preInit(FMLPreInitializationEvent e) {

        // Задаём переменную сети Minecraft.
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);

        // Серверные пакеты. Здесь больше нечего сказать.
        // Да, они летят с клиента на сервер.
        regServer(MessageOpenResearchGui.Handler.class, MessageOpenResearchGui.class);
        regServer(MessageGuiAction.Handler.class, MessageGuiAction.class);
        regServer(MessageToggleAutoResearch.Handler.class, MessageToggleAutoResearch.class);

        // Клиентские пакеты. Те-же пакеты, но выполняемые с сервера на клиент.
        // По типу, синхронизации прогресса, и т.п.
        regClient(MessageSyncResearch.Handler.class, MessageSyncResearch.class);
        regClient(MessageSyncSingleResearch.Handler.class, MessageSyncSingleResearch.class);
    }

    /// Инициализация мода.
    // Здесь регистрируем обработчик открытия GUI.
    public void init(FMLInitializationEvent e) {
        NetworkRegistry.INSTANCE.registerGuiHandler(MatterReplicationEngine.instance, new GuiHandler());
    }

    /// Пост-инициализация мода.
    // Здесь ничего не делаем, но есть возможность инициализировать дополнительные системы.
    public void postInit(FMLPostInitializationEvent e) {}

    /// Синхронизация данных с сервера на клиент.
    // Эти методы вызываются автоматически при получении пакетов.
    public void handleSyncResearch(MessageSyncResearch message) {}
    public void handleSyncSingleResearch(MessageSyncSingleResearch message) {}

    /// Регистрация обработчиков пакетов на сервере и клиенте.
    // Вспомогательные методы для быстрой регистрации пакетов с автогенерацией ID.
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
