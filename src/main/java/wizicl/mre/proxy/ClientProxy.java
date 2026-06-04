package wizicl.mre.proxy;

import wizicl.mre.capabilities.IResearch;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

import java.util.HashSet;
import java.util.Map;
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

        try {
            Class<?> textureMapClass = net.minecraft.client.renderer.texture.TextureMap.class;

            java.lang.reflect.Method method;
            try {
                method = textureMapClass.getDeclaredMethod("func_174938_a");
            } catch (NoSuchMethodException e) {
                method = textureMapClass.getDeclaredMethod("initMissingImage");
            }

            method.setAccessible(true);
            System.out.println("[JourneyMode] Успешно открыт доступ к initMissingImage для JEI!");
        } catch (Exception e) {
            System.out.println("[JourneyMode] Не удалось пропатчить метод рефлексией: " + e.getMessage());
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        keyBindOpenGui = new KeyBinding(
                "key.journeymode.open_menu",
                Keyboard.KEY_J,
                "key.categories.journeymode"
        );

        keyBindDelItem = new KeyBinding(
                "key.journeymode.del_item",
                Keyboard.KEY_DELETE,
                "key.categories.journeymode"
        );

        ClientRegistry.registerKeyBinding(keyBindOpenGui);
        ClientRegistry.registerKeyBinding(keyBindDelItem);
        MinecraftForge.EVENT_BUS.register(new TooltipHandler());
    }

    @Override
    public void handleSyncResearch(MessageSyncResearch message) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            EntityPlayer player = Minecraft.getMinecraft().player;

            if (player != null) {
                IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                if (cap instanceof Research) {
                    ((Research) cap).refreshFromServer(message.data);
                    CACHED_UNLOCKED_ITEMS.clear();
                    for (Map.Entry<ResearchKey, Integer> entry : message.data.entrySet()) {
                        ItemStack stack = entry.getKey().createItemStack();
                        if (stack.isEmpty()) continue;

                        int required = JourneyUtils.getRequiredAmount(stack);
                        if (entry.getValue() >= required) {
                            ResearchKey key = entry.getKey();
                            CACHED_UNLOCKED_ITEMS.add(key);
                        }
                    }
                    cap.setAutoResearchState(message.getAutoResearchState());
                    System.out.println("CLIENT-SIDE: Данные исследований синхронизированы! Кэш предметов готов. Размер: " + CACHED_UNLOCKED_ITEMS.size());
                }
            }
        });
    }

    @Override
    public void handleSyncSingleResearch(MessageSyncSingleResearch message) {
        Minecraft mc = Minecraft.getMinecraft();

        // Используем планировщик ваниллы для безопасного выполнения в основном потоке клиента
        mc.addScheduledTask(() -> {
            EntityPlayer player = mc.player;
            if (player == null) return;

            // Достаем клиентскую капу исследований
            IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
            if (cap instanceof Research) {

                ItemStack dummyStack = message.key.createItemStack();
                if (dummyStack.isEmpty()) return;

                // ОБНОВЛЯЕМ КАПУ ВСЕГДА. Для тултипов нужен любой прогресс (даже 1 / 50).
                cap.setResearch(dummyStack, message.progress);
                int required = JourneyUtils.getRequiredAmount(dummyStack);

                // Логика ПОЛНОГО изучения
                if (message.progress >= required) {
                    CACHED_UNLOCKED_ITEMS.add(message.key);
                    mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F));
                } else if (message.progress == 0){
                    CACHED_UNLOCKED_ITEMS.remove(message.key);
                }

                // Динамически обновляем интерфейс, если у игрока прямо сейчас открыта книга
                if (mc.currentScreen instanceof GuiResearch) {
                    GuiResearch gui = (GuiResearch) mc.currentScreen;
                    gui.updateResearchLog(message.key, message.progress);
                }
            }
        });
    }
}
