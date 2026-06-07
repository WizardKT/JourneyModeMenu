package wizicl.mre.init;

import com.cleanroommc.modularui.factory.GuiFactories;
import net.minecraft.entity.player.EntityPlayerMP;
import wizicl.mre.MatterReplicationEngine;
import wizicl.mre.Reference;
import wizicl.mre.capabilities.Research;
import wizicl.mre.capabilities.ResearchProvider;
import wizicl.mre.network.MessageOpenResearchGui;
import wizicl.mre.network.MessageSyncResearch;
import wizicl.mre.proxy.ClientProxy;
import wizicl.mre.proxy.CommonProxy;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

// Регистрируем наши данные в Майнкрафте
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class RegHandler {

    // Регистрации капы на игроке
    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        // Используем паттерн-матчинг вместо instanceof + каст
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(new ResourceLocation("mre", "research"), new ResearchProvider());
        }
    }

    // Выдача капы игроку если тот умер (Синхронизация при возрождении)
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            var oldCap = event.getOriginal().getCapability(ResearchProvider.RESEARCH, null);
            var newCap = event.getEntityPlayer().getCapability(ResearchProvider.RESEARCH, null);

            // Красивый сдвоенный паттерн-матчинг без единого явного каста
            if (oldCap instanceof Research oldRes && newCap instanceof Research newRes) {
                newRes.refreshFromServer(oldRes.getReadOnlyMap());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        syncPlayerResearch(event.player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        syncPlayerResearch(event.player);
        MatterReplicationEngine.logger.info("SERVER: Игрок {} сменил измерение. Данные синхронизированы.", event.player.getName());
    }


    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        syncPlayerResearch(event.player);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (ClientProxy.keyBindOpenGui.isPressed()) {
            CommonProxy.NETWORK.sendToServer(new MessageOpenResearchGui());
        }
    }

    private static void syncPlayerResearch(EntityPlayer player) {
        // Проверяем, что мир серверный И что игрок является серверным (EntityPlayerMP)
        if (!player.world.isRemote && player instanceof EntityPlayerMP playerMP) {
            var cap = playerMP.getCapability(ResearchProvider.RESEARCH, null);
            if (cap != null) {
                CommonProxy.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap(), cap.getAutoResearchState()), playerMP);
            }
        }
    }
}

