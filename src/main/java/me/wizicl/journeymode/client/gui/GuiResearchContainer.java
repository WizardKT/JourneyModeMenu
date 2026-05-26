package me.wizicl.journeymode.client.gui;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchKey;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.config.ConfigMain;
import me.wizicl.journeymode.network.MessageSyncSingleResearch;
import me.wizicl.journeymode.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.HashMap;
import java.util.Map;

public class GuiResearchContainer extends Container {

    private final EntityPlayer player;
    private final IItemHandler researchInventory;
    private GuiState currentState = GuiState.RESEARCH;
    private boolean isShiftPressedRightNow = false;

    private static final int PLAYER_INV_START = 0;
    private static final int PLAYER_HOTBAR_START = 27;
    private static final int PLAYER_INV_END = 36;
    private static final int RESEARCH_SLOT_INDEX = 36;

    // Хранилище оригинальных позиций слотов, чтобы не создавать кастомные классы слотов
    private final Map<Slot, SlotPos> originalSlotPositions = new HashMap<>();

    // Вспомогательный мини-класс для хранения координат
    private static class SlotPos {
        final int x, y;
        SlotPos(int x, int y) { this.x = x; this.y = y; }
    }

    public GuiResearchContainer(InventoryPlayer playerInv) {
        this.player = playerInv.player;
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);

        this.researchInventory = (cap != null) ? cap.getResearchInventory() : new ItemStackHandler(1);

        // Логика инициализации слотов
        this.addPlayerInventory(playerInv);
        this.addPlayerHotbar(playerInv);
        this.addResearchSlot();

        // Автоматически запоминает начальные координаты абсолютно всех созданных слотов
        for (Slot slot : this.inventorySlots) {
            this.originalSlotPositions.put(slot, new SlotPos(slot.xPos, slot.yPos));
        }
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
                if (!stack.isEmpty() && ConfigMain.easy_research_shift && isShiftPressedRightNow) {
                    EntityPlayer player = GuiResearchContainer.this.player;
                    IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                    int amount = stack.getCount();

                    if (cap != null) {
                        if (!player.world.isRemote) {
                            int added = cap.addResearch(stack, amount);

                            if (added > 0 && player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                                ResearchKey key = new ResearchKey(stack);
                                int newProgress = cap.getResearch(stack);

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
                super.putStack(stack);
            }
        });
    }

    /// === БЕЗОПАСНОЕ ПЕРЕКЛЮЧЕНИЕ СОСТОЯНИЙ ===

    public void switchState(GuiState newState) {
        this.currentState = newState;

        for (Slot slot : this.inventorySlots) {
            SlotPos originalPos = this.originalSlotPositions.get(slot);
            if (originalPos == null) continue;

            if (newState == GuiState.GIVE) {
                if (slot.slotNumber < PLAYER_HOTBAR_START || slot.slotNumber >= PLAYER_INV_END) {
                    // Прячем все слоты кроме хотбара (инвентарь и слот исследования) далеко за экран
                    slot.xPos = -2000;
                    slot.yPos = -2000;
                }
            } else {
                // Возвращаем слоты на их законные места
                slot.xPos = originalPos.x;
                slot.yPos = originalPos.y;
            }
        }
    }

    /// === ЖЕЛЕЗНАЯ ЗАЩИТА ОТ ЧИТЕРОВ И ЭКСПЛОИТОВ ===

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        // Если открыта вкладка GIVE — полностью блокируем стандартные клики по контейнеру на сервере, кроме хотбара.
        // Читерские пакеты на клики по "невидимым" слотам просто проигнорируются.
        if (this.currentState == GuiState.GIVE) {
            if (slotId < PLAYER_HOTBAR_START || slotId >= PLAYER_INV_END) {
                    return ItemStack.EMPTY;
            }
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        // Двойная защита: если мы в режиме выдачи, Shift-клик не должен ничего перемещать
        if (this.currentState == GuiState.GIVE) {
            if (index < PLAYER_HOTBAR_START || index >= PLAYER_INV_END) {
                return ItemStack.EMPTY;
            }
        }

        this.isShiftPressedRightNow = true;

        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index == RESEARCH_SLOT_INDEX) {
                if (!this.mergeItemStack(itemstack1, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(itemstack1, itemstack);
            }
            else {
                if (this.currentState == GuiState.RESEARCH) {
                    // @TODO: Логи исследований на клиенте должны обновляться внутри GuiResearch#updateScreen или через пакеты.

                    if (!this.mergeItemStack(itemstack1, RESEARCH_SLOT_INDEX, RESEARCH_SLOT_INDEX + 1, false)) {
                        if (index < PLAYER_HOTBAR_START) {
                            if (!this.mergeItemStack(itemstack1, PLAYER_HOTBAR_START, PLAYER_INV_END, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            if (!this.mergeItemStack(itemstack1, PLAYER_INV_START, PLAYER_HOTBAR_START, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        this.isShiftPressedRightNow = false;
        return itemstack;
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
}
