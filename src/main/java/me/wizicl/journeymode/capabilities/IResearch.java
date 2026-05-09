package me.wizicl.journeymode.capabilities;

import java.util.Map;

public interface IResearch {
    void addResearch(String itemName, int amount);
    int getResearchCount(String itemName);
    Map<String, Integer> getResearchMap();
    void set(int amount);
    int getRequiredAmount(String itemName);
}
