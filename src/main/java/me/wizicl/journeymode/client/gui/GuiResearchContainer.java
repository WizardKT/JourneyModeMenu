package me.wizicl.journeymode.client.gui;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class GuiResearchContainer extends Container{

    private final IItemHandler researchInventory;
    private GuiState currentState = GuiState.RESEARCH;

    private static final int PLAYER_INV_START = 0;
    private static final int PLAYER_HOTBAR_START = 27;
    private static final int PLAYER_INV_END = 36;
    private static final int RESEARCH_SLOT_INDEX = 36;

    public GuiResearchContainer(InventoryPlayer playerInv) {

        EntityPlayer player = playerInv.player;
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
                    if (!this.mergeItemStack(itemstack1, RESEARCH_SLOT_INDEX, RESEARCH_SLOT_INDEX + 1, false)) {
                        if (index < PLAYER_HOTBAR_START) {
                            if (!this.mergeItemStack(itemstack1, PLAYER_HOTBAR_START, PLAYER_INV_END, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else if (!this.mergeItemStack(itemstack1, PLAYER_INV_START, PLAYER_HOTBAR_START, false)) {
                            return ItemStack.EMPTY;
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

        return itemstack;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
    }
}
