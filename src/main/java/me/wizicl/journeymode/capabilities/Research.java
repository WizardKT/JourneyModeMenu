package me.wizicl.journeymode.capabilities;

import me.wizicl.journeymode.util.JourneyUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Research implements IResearch {
    private final Map<ResearchKey, Integer> researchMap = new HashMap<>();

    private final ItemStackHandler researchInventory = new ItemStackHandler(1);

    @Override
    public int addResearch(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return 0;
        ResearchKey key = new ResearchKey(stack);
        int maxRequired = getRequiredAmount(stack);
        int current = researchMap.getOrDefault(key, 0);

        if (current >= maxRequired) {
            return 0;
        }

        int allowedToAdd = Math.min(amount, maxRequired - current);

        researchMap.put(key, current + allowedToAdd);

        return allowedToAdd;
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

        int required = getRequiredAmount(stack);
        return getResearch(stack) >= required;
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

    @Override
    public IItemHandler getResearchInventory() {
        return this.researchInventory;
    }

    public NBTTagCompound serializeNBT() {
        return ResearchSerializer.serialize(this);
    }

    public void deserializeNBT(NBTTagCompound nbt) {
        ResearchSerializer.deserialize(this, nbt);
    }
}
