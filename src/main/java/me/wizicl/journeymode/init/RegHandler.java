package me.wizicl.journeymode.init;

import me.wizicl.journeymode.JourneyMode;
import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

// Регистрируем наши предметы в Майнкрафте
@Mod.EventBusSubscriber(modid = JourneyMode.MODID)
public class RegHandler {
    @SubscribeEvent
    public static void RegItems(RegistryEvent.Register<Item> event) {
//        event.getRegistry().register(ModItems.ITEM_TEST_ITEM);
//        JourneyMode.logger.info("Зарегестрирован новый предмет!");
    }

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(new ResourceLocation("journeymode", "research"), new ResearchProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            EntityPlayer oldplayer = event.getOriginal();
            EntityPlayer newplayer = event.getEntityPlayer();

            IResearch oldCap = oldplayer.getCapability(ResearchProvider.RESEARCH, null);
            IResearch newCap = newplayer.getCapability(ResearchProvider.RESEARCH, null);

            if (oldplayer != null && newplayer != null) {
                newCap.getResearchMap().putAll(oldCap.getResearchMap());
            }

        }
    }

}

