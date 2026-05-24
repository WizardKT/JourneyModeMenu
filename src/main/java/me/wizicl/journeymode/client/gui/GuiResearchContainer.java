package me.wizicl.journeymode.client.gui;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchKey;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.config.ConfigMain;
import me.wizicl.journeymode.network.MessageSyncSingleResearch;
import me.wizicl.journeymode.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class GuiResearchContainer extends Container{


    private final EntityPlayer player;
    private final IItemHandler researchInventory;
    private GuiState currentState = GuiState.RESEARCH;
    private boolean isShiftPressedRightNow = false;

    private static final int PLAYER_INV_START = 0;
    private static final int PLAYER_HOTBAR_START = 27;
    private static final int PLAYER_INV_END = 36;
    private static final int RESEARCH_SLOT_INDEX = 36;



    public GuiResearchContainer(InventoryPlayer playerInv) {

        this.player = playerInv.player;
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);

        this.researchInventory = (cap != null) ? cap.getResearchInventory() : new ItemStackHandler(1);

        this.addPlayerInventory(playerInv);
        this.addPlayerHotbar(playerInv);
        this.addResearchSlot();
    }

    private void addPlayerInventory(InventoryPlayer playerInv) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(InventoryPlayer playerInv) {
        for (int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, 142));
        }
    }

    private void addResearchSlot() {
        this.addSlotToContainer(new SlotItemHandler(this.researchInventory, 0, 98, 32) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return !stack.isEmpty();
            }

            @Override
            public boolean isEnabled() {
                return currentState == GuiState.RESEARCH;
            }

            @Override
            public void putStack(ItemStack stack) {
                // Проверяем настройку конфига и внутренний флаг шифта
                if (!stack.isEmpty() && ConfigMain.easy_research_shift && isShiftPressedRightNow) {
                    EntityPlayer player = GuiResearchContainer.this.player;
                    IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                    int amount = stack.getCount();

                    if (cap != null) {
                        if (!player.world.isRemote) {
                            int added = cap.addResearch(stack, amount);

                            if (added > 0 && player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                                me.wizicl.journeymode.capabilities.ResearchKey key = new me.wizicl.journeymode.capabilities.ResearchKey(stack);
                                int newProgress = cap.getResearch(stack);

                                // Шлем клиенту только этот предмет и его цифру прогресса
                                CommonProxy.NETWORK.sendTo(
                                        new MessageSyncSingleResearch(key, newProgress),
                                        (net.minecraft.entity.player.EntityPlayerMP) player
                                );
                            }
                        }

                        super.putStack(ItemStack.EMPTY);
                        return;
                    }
                }

                // Если шифт не был зажат (предмет принесли мышкой),
                // или автоизучение выключено — предмет просто ложится в слот
                super.putStack(stack);
            }
        });
    }

    public void switchState(GuiState newState) {
        if (this.currentState != newState) {
            this.currentState = newState;
        }
    }

    public ItemStack getResearchTargetStack() {
        return this.researchInventory.getStackInSlot(0);
    }

    public boolean isResearchSlotEmpty() {
        return this.getResearchTargetStack().isEmpty();
    }

    public GuiState getCurrentState() {
        return this.currentState;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        this.isShiftPressedRightNow = true;

        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        // Проверяем, что слот существует и в нём вообще есть предмет
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy(); // Сохраняем копию для сверки количества в конце

            /// === СИТУАЦИЯ А: Игрок кликнул по СЛОТУ ИССЛЕДОВАНИЯ (вынимает предмет) ===

            if (index == RESEARCH_SLOT_INDEX) {
                // Пытаемся переместить предмет из слота исследования в инвентарь игрока
                if (!this.mergeItemStack(itemstack1, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY; // Если инвентарь забит, ничего не делаем
                }
                slot.onSlotChange(itemstack1, itemstack);
            }

            /// === СИТУАЦИЯ Б: Игрок кликнул по своему ИНВЕНТАРЮ / ХОТБАРУ ===
            else {
                // Если сейчас открыто окно исследований, пытаемся засунуть предмет в слот исследования
                if (this.currentState == GuiState.RESEARCH) {

                    // Ловим момент ДО того, как майнкрафт уменьшит или переместит стек
                    if (playerIn.world.isRemote) {
                        net.minecraft.client.gui.GuiScreen currentScreen = net.minecraft.client.Minecraft.getMinecraft().currentScreen;

                        if (currentScreen instanceof GuiResearch) {
                            GuiResearch gui = (GuiResearch) currentScreen;

                            // Создаем ключ из предмета, по которому кликнули, и берем его количество
                            ResearchKey key = new ResearchKey(itemstack1);
                            int amount = itemstack1.getCount();

                            // Запускаем бегущую строку в GUI!
                            gui.updateResearchLog(key, amount);
                        }
                    }

                    if (!this.mergeItemStack(itemstack1, RESEARCH_SLOT_INDEX, RESEARCH_SLOT_INDEX + 1, false)) {

                        // Если слот исследования уже занят — перекидываем между Хотбаром и Инвентарем
                        if (index < PLAYER_HOTBAR_START) {

                            // Кликнули в инвентаре -> перекидываем в хотбар
                            if (!this.mergeItemStack(itemstack1, PLAYER_HOTBAR_START, PLAYER_INV_END, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {

                            // Кликнули в хотбаре -> перекидываем в инвентарь
                            if (!this.mergeItemStack(itemstack1, PLAYER_INV_START, PLAYER_HOTBAR_START, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                }
            }

            /// === ФИНАЛЬНАЯ ЗАЧИСТКА И ОБНОВЛЕНИЕ СЛОТОВ ===

            // Если стак полностью опустел — очищаем слот
            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                // Если что-то осталось — сообщаем слоту, что его содержимое изменилось
                slot.onSlotChanged();
            }

            // Если количество предметов не изменилось — выходим
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            // Вызываем событие взятия предмета (для триггеров и ачивок майна)
            slot.onTake(playerIn, itemstack1);
        }

        this.isShiftPressedRightNow = false;
        return itemstack;
    }


    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
    }
}
