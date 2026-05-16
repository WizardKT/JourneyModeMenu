package me.wizicl.journeymode.capabilities;

import me.wizicl.journeymode.main.JourneyUtils;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Research implements IResearch {
    private final Map<ResearchKey, Integer> researchMap = new HashMap<>();

    @Override
    public void addResearch(ItemStack stack, int amount) {
        if (stack.isEmpty()) return;
        ResearchKey key = new ResearchKey(stack);
        int current = researchMap.getOrDefault(key, 0);
        researchMap.put(key, current + amount);
    }

    @Override
    public void setResearch(ItemStack stack, int amount)  {
        if (stack.isEmpty()) return;
        researchMap.put(new ResearchKey(stack), amount);
    }

    @Override
    public int getResearch(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return researchMap.getOrDefault(new ResearchKey(stack), 0);
    }

    @Override
    public boolean isResearched(ItemStack stack) {
        if (stack.isEmpty()) return false;

        int requared = getRequiredAmount(stack);
        return getResearch(stack) >= requared;
    }

    @Override
    public Map<ResearchKey, Integer> getReadOnlyMap() {
        return Collections.unmodifiableMap(researchMap);
    }

    @Override
    public void clear() {
        researchMap.clear();
    }

    @Override
    public void remove(ItemStack stack) {
        researchMap.remove(new ResearchKey(stack));
    }

    public int getRequiredAmount(ItemStack stack) {
        return JourneyUtils.getRequiredAmount(stack);
    }

    public void refreshFromServer(Map<ResearchKey, Integer> newData) {
        this.researchMap.clear();
        this.researchMap.putAll(newData);
    }
}
