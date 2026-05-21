package me.wizicl.journeymode.protecter;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class StackValidator {
    private final ItemStack stack;
    private final EntityPlayer player;
    private boolean isValid = true;

    /// Проверка на дурака, ультимативная версия

    private StackValidator(EntityPlayer player, ItemStack stack) {
        this.player = player;
        this.stack = stack;

        if (stack == null || stack.isEmpty() || player == null || stack.getItem().getRegistryName() == null) {
            this.isValid = false;
        }
    }

    // --- Проверка стака и игрока ---

    public static StackValidator check(EntityPlayer player, ItemStack stack) {
        return new StackValidator(player, stack);
    }

    // Проверка на состояние, блок ли это

    public StackValidator isBlock() {
        if (!isValid) return this;
        Item item = stack.getItem();
        if (!(item instanceof net.minecraft.item.ItemBlock)) this.isValid = false;

        return this;
    }

    // Проверка предмета на то, можно ли его сломать

    public StackValidator isDamagable() {
        if (!isValid) return this;
        if (!this.stack.isItemStackDamageable()) this.isValid = false;

        return this;
    }

    // Проверка на существование капы у стака

    public StackValidator hasCapability() {
        if (!this.isValid) return this;

        IResearch cap = this.player.getCapability(ResearchProvider.RESEARCH, null);
        if (cap == null) this.isValid = false;

        return this;
    }

    // Проверка на режим игры креатив

    public StackValidator isCreative() {
        if (!this.isValid) return this;

        if (!this.player.capabilities.isCreativeMode) this.isValid = false;
        return this;
    }

    /// === Вывод ===

    public boolean get() {
        return this.isValid;
    }
}
