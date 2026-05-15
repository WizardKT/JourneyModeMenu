package me.wizicl.journeymode.capabilities;

import net.minecraft.item.ItemStack;

import java.util.Map;

public interface IResearch {
    void addResearch(ItemStack stack, int amount);
    int getResearchCount(ItemStack stack);
    Map<ResearchKey, Integer> getResearchMap();
    int getRequiredAmount(ItemStack stack);
}
