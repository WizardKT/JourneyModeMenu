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

/// Данный класс отвечает за инициализацию клиентской части мода.
// Он наследуется от `CommonProxy` и реализует специфические методы для клиента.
// К примеру: - инициализирует GUI для исследования.
//            - Регистрирует клавиши управления.
//            - Обрабатывает синхронизацию данных исследований.
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    /// Создаём переменные для управления клавишами.
    // Эти переменные используются для отслеживания нажатия клавиш и выполнения действий в зависимости от них.
    public static KeyBinding keyBindOpenGui;
    public static KeyBinding keyBindDelItem;

    /// Создаём кэш для хранения разблокированных предметов.
    // Это используется для оптимизации проверки состояния исследований и ускорения обновления GUI.
    public static final Set<ResearchKey> CACHED_UNLOCKED_ITEMS = new HashSet<>();
    public static void clearCache() {
        CACHED_UNLOCKED_ITEMS.clear();
    }

    /// Метод вызывается перед инициализацией мода.
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    /// Метод вызывается при инициализации мода.
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // Регистрируем клавиши управления GUI.
        keyBindOpenGui = new KeyBinding("key.journeymode.open_menu",
                Keyboard.KEY_J, "key.categories.journeymode");
        keyBindDelItem = new KeyBinding("key.journeymode.del_item",
                Keyboard.KEY_DELETE, "key.categories.journeymode");

        // Регистрируем клавиши в клиентской части игры.
        ClientRegistry.registerKeyBinding(keyBindOpenGui);
        ClientRegistry.registerKeyBinding(keyBindDelItem);

        // Регистрируем тултипы.
        MinecraftForge.EVENT_BUS.register(new TooltipHandler());
    }

    /// Метод для синхронизации исследований с клиентом.
    // Именно этот синхронизирует сразу все исследования с сервера на клиент игрока.
    // Используется в основном при запуске игры и при загрузке мира,
    // из-за слишком большого размера пакета, который мы отправляем клиенту.
    @Override
    public void handleSyncResearch(MessageSyncResearch message) {
        var mc = Minecraft.getMinecraft();

        // Мы используем addScheduledTask, чтобы не блокировать основной поток игры.
        mc.addScheduledTask(() -> {
            var player = mc.player;
            if (player == null) return;

            /// Паттерн-матчинг капы.
            // Объяснение: Паттерн-матчик это способ для проверки и извлечения значений из объектов.
            // В данном случае мы проверяем, является ли player.getCapability(ResearchProvider.RESEARCH, null)
            // экземпляром класса Research. Если да, то присваиваем его переменной researchCap.
            // Это позволяет нам безопасно работать с этим объектом без необходимости проверки на null.
            // В случае, если player.getCapability(...) не является экземпляром Research, код после if не будет выполняться.

            /// Проще говоря:
            // Мы проверяем, что у игрока есть возможность исследования (researchCap), и если да, то мы обновляем его данные с сервера.
            if (player.getCapability(ResearchProvider.RESEARCH, null) instanceof Research researchCap) {
                researchCap.refreshFromServer(message.data);
                CACHED_UNLOCKED_ITEMS.clear();

                // Проходимся через все элементы в сообщении и проверяем, есть ли они в кэше.
                // Если да, то добавляем их в CACHED_UNLOCKED_ITEMS. Это позволяет нам отслеживать, какие предметы игрок уже исследовал.
                for (var entry : message.data.entrySet()) {
                    var stack = entry.getKey().createItemStack();
                    if (stack.isEmpty()) continue;

                    // Если количество исследований больше или равно требуемому для исследования, добавляем его в кэш.
                    int required = JourneyUtils.getRequiredAmount(stack);
                    if (entry.getValue() >= required) {
                        CACHED_UNLOCKED_ITEMS.add(entry.getKey());
                    }
                }

                // Обновляем состояние автоматического исследования на основе данных в сообщении.
                // Записано в общий пакет обновления данных чтобы не плодить лишние пакеты.
                researchCap.setAutoResearchState(message.getAutoResearchState());
                MatterReplicationEngine.logger.info("CLIENT: Исследования синхронизированы. Кэш предметов готов, размер: {}", CACHED_UNLOCKED_ITEMS.size());
            }
        });
    }

    /// Обрабатывает сообщение о синхронизации отдельного исследования.
    // Работает по немного другому принципу:
    // Вместо синхронизации всего прогресса, точечно синхронизирует прогресс одного исследования, по принципу
    // 0 + 1 = 1, а так же 1 + 1 = 2.
    @Override
    public void handleSyncSingleResearch(MessageSyncSingleResearch message) {
        var mc = Minecraft.getMinecraft();

        mc.addScheduledTask(() -> {
            var player = mc.player;
            if (player == null) return;

            /// Паттерн-матчинг капы
            if (player.getCapability(ResearchProvider.RESEARCH, null) instanceof Research researchCap) {

                // Создаём предмет-пустышку на основе ключа и проверяем чтобы оно не было пустим.
                var dummyStack = message.key.createItemStack();
                if (dummyStack.isEmpty()) return;

                // Вместо добавления мы жёстко устанавливаем прогресс. Это позволяет избежать добавления лишних очков прогрессии.
                researchCap.setResearch(dummyStack, message.progress);
                int required = JourneyUtils.getRequiredAmount(dummyStack);

                // Проверяем на достижение максимального прогресса и добавляем предмет в список открытых.
                if (message.progress >= required && CACHED_UNLOCKED_ITEMS.add(message.key)) {
                    mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F));
                } else if (message.progress == 0) { // Если прогресс равен нулю, удаляем предмет из списка открытых.
                    CACHED_UNLOCKED_ITEMS.remove(message.key);
                }

                // Паттерн-матчинг для динамического обновления GUI книги рецептов/исследований
                if (mc.currentScreen instanceof GuiResearch gui) {
                    gui.updateResearchLog(message.key, message.progress);
                }

                // TODO: При удалении предметов придумать способ для обновления GUI. Вероятно, придётся изменять пакет синхронизации.
            }
        });
    }
}
