package wizicl.mre.autoresearch;

import wizicl.mre.capabilities.IResearch;
import wizicl.mre.capabilities.ResearchKey;
import wizicl.mre.capabilities.ResearchProvider;
import wizicl.mre.config.ConfigMain;
import wizicl.mre.network.MessageSyncSingleResearch;
import wizicl.mre.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

@Mod.EventBusSubscriber(modid = "journeymode") // Если у тебя есть константа JourneyMode.MODID, можешь заменить на нее
public class CraftingEventHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        EntityPlayer player = event.player;
        ItemStack craftedStack = event.crafting;

        // Базовые проверки (только на сервере)
        if (player.world.isRemote || craftedStack.isEmpty() || !(player instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        IResearch cap = playerMP.getCapability(ResearchProvider.RESEARCH, null);

        // Проверяем, существует ли капа и включена ли кнопка авто-изучения (Ar)
        if (cap == null || !cap.getAutoResearchState()) {
            return;
        }

        // Запоминаем данные о предмете до того, как ванильный цикл его разорвет
        ResearchKey key = new ResearchKey(craftedStack);
        int amountCrafted = craftedStack.getCount();

        // ОДНА-ЕДИНСТВЕННАЯ ОТЛОЖЕННАЯ ЗАДАЧА (Классический синтаксис для Java 8)
        playerMP.getServerWorld().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                // 1. Изучаем предмет
                int added = cap.addResearch(key.createItemStack(), amountCrafted);

                if (added > 0) {
                    // 2. Если нужно поглощать предметы
                    if (ConfigMain.autoResearch.consumeItems) {
                        int remainingToRemove = added;

                        // ШАГ А: СНАЧАЛА проверяем предмет "на мышке"
                        ItemStack cursorStack = playerMP.inventory.getItemStack();
                        if (!cursorStack.isEmpty() && key.equals(new ResearchKey(cursorStack))) {
                            int toTake = Math.min(cursorStack.getCount(), remainingToRemove);
                            cursorStack.shrink(toTake);
                            remainingToRemove -= toTake;

                            if (cursorStack.isEmpty()) {
                                playerMP.inventory.setItemStack(ItemStack.EMPTY);
                            }

                            // Точечно обновляем ТОЛЬКО курсор, чтобы убить призрака в руке
                            playerMP.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(-1, -1, playerMP.inventory.getItemStack()));
                        }

                        // ШАГ Б: ЗАТЕМ проверяем инвентарь (на случай Shift-клика)
                        if (remainingToRemove > 0) {
                            for (int i = 0; i < playerMP.inventory.getSizeInventory(); i++) {
                                if (remainingToRemove <= 0) break;

                                ItemStack invStack = playerMP.inventory.getStackInSlot(i);

                                if (!invStack.isEmpty() && key.equals(new ResearchKey(invStack))) {
                                    int toTake = Math.min(invStack.getCount(), remainingToRemove);
                                    invStack.shrink(toTake);
                                    remainingToRemove -= toTake;
                                }
                            }
                            // Мягко просим инвентарь перепроверить свои слоты
                            playerMP.openContainer.detectAndSendChanges();
                        }
                    }

                    // 3. Синхронизируем новый прогресс на клиент
                    int newProgress = cap.getResearch(key);
                    CommonProxy.NETWORK.sendTo(
                            new MessageSyncSingleResearch(key, newProgress),
                            playerMP
                    );
                }
            }
        });
    }
}