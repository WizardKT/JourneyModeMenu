package wizicl.mre.autoresearch;

import wizicl.mre.Reference;
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

/// Этот класс отвечает за обработку событий связанных с крафтом предметов.
// Он реагирует на событие PlayerEvent.ItemCrafted, которое срабатывает при крафте предмета игроком.
// В случае успешного крафта, если включена опция авто-изучения (Ar), система автоматически изучает полученный предмет.
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class CraftingEventHandler {

    /// Этот метод вызывается при крафте предмета игроком.
    // Он проверяет, включена ли опция авто-изучения и изучает полученный предмет.
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        EntityPlayer player = event.player;
        ItemStack craftedStack = event.crafting;

        // Проверка на наличие игрока, предмета и валидность капы (Research)
        if (player.world.isRemote || craftedStack.isEmpty() || !(player instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        IResearch cap = playerMP.getCapability(ResearchProvider.RESEARCH, null);

        // Проверка на включённость опции авто-изучения.
        if (cap == null || !cap.getAutoResearchState()) {
            return;
        }

        // Создание ключа исследования для полученного предмета.
        ResearchKey key = new ResearchKey(craftedStack);
        int amountCrafted = craftedStack.getCount();

        // Используем асинхронный таск для изучения предмета после крафта.
        playerMP.getServerWorld().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                /// 1. Попытка изучить предмет. Возвращает количество добавленных пунктов исследования.
                int added = cap.addResearch(key.createItemStack(), amountCrafted);

                if (added > 0) {
                    /// 2. Если предмет успешно изучен, удаляем его с инвентаря.
                    if (ConfigMain.autoResearch.consumeItems) {
                        int remainingToRemove = added;

                        /// 3. Сначала проверяем курсор. Это важно, чтобы не удалять предметы, которые находятся в руке игрока.
                        // Если игрок держит исследуемый предмет в руке, уменьшаем его количество и обновляем статус инвентаря.
                        ItemStack cursorStack = playerMP.inventory.getItemStack();
                        if (!cursorStack.isEmpty() && key.equals(new ResearchKey(cursorStack))) {
                            int toTake = Math.min(cursorStack.getCount(), remainingToRemove);
                            cursorStack.shrink(toTake);
                            remainingToRemove -= toTake;

                            // Если предмет в курсоре исчез, обновляем статус инвентаря.
                            if (cursorStack.isEmpty()) {
                                playerMP.inventory.setItemStack(ItemStack.EMPTY);
                            }

                            // Если предмет в курсоре исчез, обновляем статус инвентаря.
                            playerMP.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(-1, -1, playerMP.inventory.getItemStack()));
                        }

                        /// 4. Если предметы в курсоре не хватает для удаления, удаляем из слотов инвентаря.
                        if (remainingToRemove > 0) {

                            // Цикл по слотам инвентаря для удаления предметов.
                            for (int i = 0; i < playerMP.inventory.getSizeInventory(); i++) {

                                // Удаляем предметы из слотов инвентаря по одному.
                                if (remainingToRemove <= 0) break;

                                ItemStack invStack = playerMP.inventory.getStackInSlot(i);

                                // Проверяем, если предмет в слоте соответствует ключу исследований и удаляем его по одному.
                                // Если предметы в слоте больше, чем требуется, мы уменьшаем их количество.
                                if (!invStack.isEmpty() && key.equals(new ResearchKey(invStack))) {
                                    int toTake = Math.min(invStack.getCount(), remainingToRemove);
                                    invStack.shrink(toTake);
                                    remainingToRemove -= toTake;
                                }
                            }
                            // Обновляем контейнер игрока после удаления предметов.
                            playerMP.openContainer.detectAndSendChanges();
                        }
                    }

                    /// 5. Уведомляем клиента об изменении прогресса исследования.
                    //  Если исследование завершено, удаляем ключ исследования из инвентаря игрока.
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