package me.wizicl.journeymode.capabilities;

import net.minecraft.item.ItemStack;

import java.util.Map;

public interface IResearch {
    void addResearch(ItemStack stack);
    int getResearchCount(String itemName);
    Map<String, Integer> getResearchMap();
    int getRequiredAmount(String itemName);
}
