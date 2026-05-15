package me.wizicl.journeymode.capabilities;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Research implements IResearch {
    private List<ItemStack> researchedItems = new ArrayList<>();

    @Override
    public void addResearch(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        researchedItems.add(copy);
    }

    @Override
    public int getResearchCount(String itemName) {
        return researchMap.getOrDefault(itemName, 0);
    }

    @Override
    public Map<String, Integer> getResearchMap() {
        return researchMap;
    }

    @Override
    public int getRequiredAmount(String itemName) {
        return researchMap.getOrDefault(itemName, 0);
    }
}
