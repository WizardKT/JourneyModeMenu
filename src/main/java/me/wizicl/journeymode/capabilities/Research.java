package me.wizicl.journeymode.capabilities;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Research implements IResearch {
    private final Map<ResearchKey, Integer> researchMap = new HashMap<>();

    @Override
    public void addResearch(ItemStack stack, int amount) {
        if (stack.isEmpty()) return;

        ResearchKey that = new ResearchKey(stack);
        int current = researchMap.getOrDefault(that, 0);
        researchMap.put(that, current + amount);
    }

    @Override
    public int getResearchCount(ItemStack stack) {
        return researchMap.getOrDefault(new ResearchKey(stack), 0);
    }

    @Override
    public Map<ResearchKey, Integer> getResearchMap() {
        return researchMap;
    }

    @Override
    public int getRequiredAmount(ItemStack stack) {
        return researchMap.getOrDefault(new ResearchKey(stack), 0);
    }
}
