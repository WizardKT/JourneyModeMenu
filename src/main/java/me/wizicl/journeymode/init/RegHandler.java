package me.wizicl.journeymode.init;

import me.wizicl.journeymode.client.gui.GuiResearch;
import me.wizicl.journeymode.JourneyMode;
import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.client.gui.GuiResearchContainer;
import me.wizicl.journeymode.network.MessageOpenResearchGui;
import me.wizicl.journeymode.network.MessageSyncResearch;
import me.wizicl.journeymode.proxy.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

// Регистрируем наши предметы в Майнкрафте
@Mod.EventBusSubscriber(modid = JourneyMode.MODID)
public class RegHandler {

    /// Регистрации капы на игроке
    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(new ResourceLocation("journeymode", "research"), new ResearchProvider());
        }
    }

    /// Выдача капы игроку если тот умер
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        EntityPlayer oldplayer = event.getOriginal();
        EntityPlayer newplayer = event.getEntityPlayer();

        IResearch oldCap = oldplayer.getCapability(ResearchProvider.RESEARCH, null);
        IResearch newCap = newplayer.getCapability(ResearchProvider.RESEARCH, null);

        if (oldplayer != null && newplayer != null) {
            newCap.getReadOnlyMap().putAll(oldCap.getReadOnlyMap());
        }

        // Создаём переменную игрока и капы
        EntityPlayer player = event.getEntityPlayer();
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);

        // Отправляем игроку пакет данных
        JourneyMode.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap()), (EntityPlayerMP) player);
    }

    /// Выдача капы игроку который зашёл на сервер в МП
    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {

        // Создаём переменную игрока
        EntityPlayer player = event.player;

        // Если это сервер
        if (!player.world.isRemote) {

            // Создаём переменную капы
            IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);

            // Отправляем игроку пакет данных
            JourneyMode.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap()), (EntityPlayerMP) player);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (ClientProxy.keyBindOpenGui.isPressed()) {
            JourneyMode.NETWORK.sendToServer(new MessageOpenResearchGui());
        }
    }
}

