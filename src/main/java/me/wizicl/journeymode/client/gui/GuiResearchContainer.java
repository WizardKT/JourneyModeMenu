package me.wizicl.journeymode.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class GuiResearchContainer extends Container{

    /// Слоты инвентаря
    public GuiResearchContainer(InventoryPlayer playerInv) {

        // Ячейка

        // Инвентарь игрока
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 3. Добавляем горячую панель игрока (Hotbar)
        for (int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, 142));
        }
    }

    /// Возможность взаимодействия со слотами
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    /// Обработка shift-клика
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        return itemstack;
    }

}
