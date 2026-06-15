package wizicl.mre.proxy;

import wizicl.mre.MatterReplicationEngine;
import wizicl.mre.capabilities.Research;
import wizicl.mre.capabilities.ResearchKey;
import wizicl.mre.capabilities.ResearchProvider;
import wizicl.mre.client.gui.controller.TooltipHandler;
import wizicl.mre.client.gui.view.GuiResearch;
import wizicl.mre.network.MessageSyncResearch;
import wizicl.mre.network.MessageSyncSingleResearch;
import wizicl.mre.util.JourneyUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    public static KeyBinding keyBindOpenGui;
    public static KeyBinding keyBindDelItem;

    public static final Set<ResearchKey> CACHED_UNLOCKED_ITEMS = new HashSet<>();
    public static void clearCache() {
        CACHED_UNLOCKED_ITEMS.clear();
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        keyBindOpenGui = new KeyBinding("key.journeymode.open_menu",
                Keyboard.KEY_J, "key.categories.journeymode");
        keyBindDelItem = new KeyBinding("key.journeymode.del_item",
                Keyboard.KEY_DELETE, "key.categories.journeymode");

        ClientRegistry.registerKeyBinding(keyBindOpenGui);
        ClientRegistry.registerKeyBinding(keyBindDelItem);

        MinecraftForge.EVENT_BUS.register(new TooltipHandler());
    }

    @Override
    public void handleSyncResearch(MessageSyncResearch message) {
        var mc = Minecraft.getMinecraft();

        mc.addScheduledTask(() -> {
            var player = mc.player;
            if (player == null) return;

            // Паттерн-матчинг капы
            if (player.getCapability(ResearchProvider.RESEARCH, null) instanceof Research researchCap) {
                researchCap.refreshFromServer(message.data);
                CACHED_UNLOCKED_ITEMS.clear();

                for (var entry : message.data.entrySet()) {
                    var stack = entry.getKey().createItemStack();
                    if (stack.isEmpty()) continue;

                    int required = JourneyUtils.getRequiredAmount(stack);
                    if (entry.getValue() >= required) {
                        CACHED_UNLOCKED_ITEMS.add(entry.getKey());
                    }
                }

                researchCap.setAutoResearchState(message.getAutoResearchState());
                MatterReplicationEngine.logger.info("CLIENT: Исследования синхронизированы. Кэш предметов готов, размер: {}", CACHED_UNLOCKED_ITEMS.size());
            }
        });
    }

    @Override
    public void handleSyncSingleResearch(MessageSyncSingleResearch message) {
        var mc = Minecraft.getMinecraft();

        mc.addScheduledTask(() -> {
            var player = mc.player;
            if (player == null) return;

            // Паттерн-матчинг капы
            if (player.getCapability(ResearchProvider.RESEARCH, null) instanceof Research researchCap) {
                var dummyStack = message.key.createItemStack();
                if (dummyStack.isEmpty()) return;

                researchCap.setResearch(dummyStack, message.progress);
                int required = JourneyUtils.getRequiredAmount(dummyStack);

                if (message.progress >= required) {
                    CACHED_UNLOCKED_ITEMS.add(message.key);
                    mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F));
                } else if (message.progress == 0) {
                    CACHED_UNLOCKED_ITEMS.remove(message.key);
                }

                // Паттерн-матчинг для динамического обновления GUI книги рецептов/исследований
                if (mc.currentScreen instanceof GuiResearch gui) {
                    gui.updateResearchLog(message.key, message.progress);
                }
            }
        });
    }
}
