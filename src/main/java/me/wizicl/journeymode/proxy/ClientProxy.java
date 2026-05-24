package me.wizicl.journeymode.proxy;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.Research;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.client.event.TooltipHandler;
import me.wizicl.journeymode.config.ConfigMain;
import me.wizicl.journeymode.network.MessageSyncResearch;
import me.wizicl.journeymode.network.MessageSyncSingleResearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    public static KeyBinding keyBindOpenGui;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        keyBindOpenGui = new KeyBinding(
                "key.journeymode.open_menu",
                Keyboard.KEY_J,
                "key.categories.journeymode"
        );
        ClientRegistry.registerKeyBinding(keyBindOpenGui);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new TooltipHandler());
    }

    @Override
    public void handleSyncResearch(MessageSyncResearch message) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            EntityPlayer player = Minecraft.getMinecraft().player;

            if (player != null) {
                IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                if (cap instanceof Research) {
                    ((Research) cap).refreshFromServer(message.data);

                    System.out.println("CLIENT-SIDE: Данные исследований успешно синхронизированы! Размер: " + message.data.size());
                }
            }
        });
    }

    @Override
    public void handleSyncSingleResearch(MessageSyncSingleResearch message) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();

        // Используем костыль ваниллы, чтобы выполнить код в основном потоке клиента
        mc.addScheduledTask(() -> {
            net.minecraft.entity.player.EntityPlayer player = mc.player;
            if (player != null) {
                // 1. Достаем клиентскую капу исследований
                me.wizicl.journeymode.capabilities.IResearch cap = player.getCapability(
                        me.wizicl.journeymode.capabilities.ResearchProvider.RESEARCH, null
                );

                if (cap instanceof me.wizicl.journeymode.capabilities.Research) {

                    net.minecraft.item.ItemStack dummyStack = new net.minecraft.item.ItemStack(
                            net.minecraft.item.Item.REGISTRY.getObject(message.key.getRegistryName()),
                            1,
                            message.key.getMeta()
                    );
                    if (message.key.getCleanedNbt() != null) {
                        dummyStack.setTagCompound(message.key.getCleanedNbt().copy());
                    }

                    // Обновляем прогресс ТОЛЬКО для этого предмета в клиентской капе
                    cap.setResearch(dummyStack, message.progress);

                    // 2. Если у игрока прямо сейчас открыт интерфейс GuiResearch,
                    // принудительно заставляем его обновить лог и запустить бегущую строку
                    if (mc.currentScreen instanceof me.wizicl.journeymode.client.gui.GuiResearch) {
                        me.wizicl.journeymode.client.gui.GuiResearch gui = (me.wizicl.journeymode.client.gui.GuiResearch) mc.currentScreen;

                        // Вызываем метод, который бросит marqueeTimer в 0
                        // и запустит строку с самого начала с новыми данными
                        gui.updateResearchLog(message.key, message.progress);
                    }
                }
            }
        });
    }
}
