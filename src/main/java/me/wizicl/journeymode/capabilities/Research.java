package me.wizicl.journeymode.capabilities;

import java.util.HashMap;
import java.util.Map;

public class Research implements IResearch {
    Map<String, Integer> researchMap = new HashMap<>();

    @Override
    public void addResearch(String itemName, int amount) {
        int currentAmount = researchMap.getOrDefault(itemName, 0);
        researchMap.put(itemName, currentAmount + amount);
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
