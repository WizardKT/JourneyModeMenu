package me.wizicl.journeymode.capabilities;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.Map;

public interface IResearch {
    // Управление исследованиями
    int addResearch(ItemStack stack, int amount);
    void setResearch(ItemStack stack, int amount);

    // Получение информации
    int getResearch(ItemStack stack);
    boolean isResearched(ItemStack stack);

    // На всякий случай
    Map<ResearchKey, Integer> getReadOnlyMap();

    // Стирание информации
    void clear();
    boolean remove(ItemStack stack);

    // Инвентарь исследовательского слота
    IItemHandler getResearchInventory();
}
