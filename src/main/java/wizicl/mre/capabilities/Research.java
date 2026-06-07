package wizicl.mre.capabilities;

import wizicl.mre.util.JourneyUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Research implements IResearch {

    private final Map<ResearchKey, Integer> researchMap = new HashMap<>();
    private final ItemStackHandler researchInventory = new ItemStackHandler(1);
    private boolean autoResearchState = false;

    /// --- Управление исследованиями ---

    @Override
    public int addResearch(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return 0;

        var key = new ResearchKey(stack);
        int maxRequired = getRequiredAmount(stack);
        int current = researchMap.getOrDefault(key, 0);

        if (current >= maxRequired) return 0;

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
    public boolean setAutoResearchState(boolean state) {
        return this.autoResearchState = state;
    }

    @Override
    public boolean toggleAutoResearchState() {
        this.autoResearchState = !this.autoResearchState;
        return this.autoResearchState;
    }

    /// --- Получение информации ---

    @Override
    public int getResearch(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        var targetId = stack.getItem().getRegistryName();
        int targetMeta = stack.getMetadata();
        var targetNbt = stack.getTagCompound(); // Грязный NBT из стака

        int bestProgress = 0;

        // Перебираем базу данных
        for (var entry : researchMap.entrySet()) {
            var dbKey = entry.getKey();

            // Быстрая проверка: ID и Мета совпадают?
            if (dbKey.getRegistryName().equals(targetId) && dbKey.getMeta() == targetMeta) {

                // Броня из Nokia 3310: Содержит ли грязный стак обязательные теги из базы?
                if (JourneyUtils.isNbtSubset(dbKey.getCleanedNbt(), targetNbt)) {

                    // Если у игрока изучено несколько вариаций, берем ту, где прогресс больше
                    bestProgress = Math.max(bestProgress, entry.getValue());
                }
            }
        }

        return bestProgress;
    }

    @Override
    public int getResearch(ResearchKey key) {
        return key == null ? 0 : researchMap.getOrDefault(key, 0);
    }

    @Override
    public boolean isResearched(ItemStack stack) {
        return !stack.isEmpty() && isResearched(new ResearchKey(stack));
    }

    @Override
    public boolean isResearched(ResearchKey key) {
        if (key == null) return false;

        var dummyStack = key.createItemStack();
        int required = JourneyUtils.getRequiredAmount(dummyStack);

        return getResearch(key) >= required;
    }

    @Override
    public int getProgress() {
        return researchMap.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public boolean getAutoResearchState() {
        return this.autoResearchState;
    }

    /// --- Стирание и утилиты ---

    @Override
    public Map<ResearchKey, Integer> getReadOnlyMap() {
        return Collections.unmodifiableMap(researchMap);
    }


    @Override
    public void clear() {
        researchMap.clear();
    }

    @Override
    public boolean remove(ItemStack stack) {
        researchMap.remove(new ResearchKey(stack));
        return true;
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
