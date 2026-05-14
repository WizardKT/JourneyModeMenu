package me.wizicl.journeymode.init;

import me.wizicl.journeymode.main.GuiResearch;
import me.wizicl.journeymode.proxy.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = "journeymode")
public class InputHandler {

    // Регистрируем нажатие игрока
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (ClientProxy.keyBindOpenResearch.isPressed()) {

            // Добавляем переменную игрока
            EntityPlayer player = Minecraft.getMinecraft().player;

            // Открываем GUI
            Minecraft.getMinecraft().displayGuiScreen(new GuiResearch());
        }
    }
}
