package me.wizicl.journeymode.capabilities;

import net.minecraft.item.ItemStack;

import java.util.Map;

public interface IResearch {
    // Управление исследованиями
    void addResearch(ItemStack stack, int amount);
    void setResearch(ItemStack stack, int amount);

    // Получение информации
    int getResearch(ItemStack stack);
    boolean isResearched(ItemStack stack);

    // На всякий случай
    Map<ResearchKey, Integer> getReadOnlyMap();

    void clear();
}
