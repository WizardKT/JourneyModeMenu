package wizicl.mre.init;

import wizicl.mre.Reference;
import wizicl.mre.capabilities.IResearch;
import wizicl.mre.capabilities.Research;
import wizicl.mre.capabilities.ResearchProvider;
import wizicl.mre.network.MessageOpenResearchGui;
import wizicl.mre.network.MessageSyncResearch;
import wizicl.mre.proxy.ClientProxy;
import wizicl.mre.proxy.CommonProxy;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

// Регистрируем наши данные в Майнкрафте
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
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
        if (event.isWasDeath()) {
            EntityPlayer oldPlayer = event.getOriginal();
            EntityPlayer newPlayer = event.getEntityPlayer();

            IResearch oldCap = oldPlayer.getCapability(ResearchProvider.RESEARCH, null);
            IResearch newCap = newPlayer.getCapability(ResearchProvider.RESEARCH, null);

            if (oldCap instanceof Research && newCap instanceof Research) {
                ((Research) newCap).refreshFromServer(oldCap.getReadOnlyMap());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        if (!event.player.world.isRemote && event.player instanceof EntityPlayerMP) {
            EntityPlayer player = event.player;
            IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
            if (cap != null) {
                CommonProxy.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap(), cap.getAutoResearchState()), (EntityPlayerMP) player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.player.world.isRemote && event.player instanceof EntityPlayerMP) {
            EntityPlayer player = event.player;
            IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
            if (cap != null) {
                CommonProxy.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap(), cap.getAutoResearchState()), (EntityPlayerMP) player);
                System.out.println("SERVER-SIDE: Игрок сменил измерение. Пакет синхронизации отправлен!");
            }
        }
    }


    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {

        EntityPlayer player = event.player;
        if (!player.world.isRemote) {
            IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
            CommonProxy.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap(), cap.getAutoResearchState()), (EntityPlayerMP) player);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (ClientProxy.keyBindOpenGui.isPressed()) {
            CommonProxy.NETWORK.sendToServer(new MessageOpenResearchGui());
        }
    }
}

