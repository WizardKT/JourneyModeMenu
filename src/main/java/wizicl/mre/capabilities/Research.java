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
    private boolean AutoResearchState = false;

    /// --- Управление исследованиями ---
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
    public boolean setAutoResearchState(boolean state) {
        return this.AutoResearchState = state;
    }

    @Override
    public boolean toggleAutoResearchState() {
        this.AutoResearchState = !this.AutoResearchState;
        return this.AutoResearchState;
    }

    /// --- Получение информации ---
    // Старый метод теперь просто делегирует задачу новому
    @Override
    public int getResearch(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        ResourceLocation targetId = stack.getItem().getRegistryName();
        int targetMeta = stack.getMetadata();
        NBTTagCompound targetNbt = stack.getTagCompound(); // БЕРЕМ СЫРОЙ, ГРЯЗНЫЙ NBT

        int bestProgress = 0;

        // Перебираем базу данных
        for (Map.Entry<ResearchKey, Integer> entry : researchMap.entrySet()) {
            ResearchKey dbKey = entry.getKey();

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

    // НОВЫЙ МЕТОД: Ищет в мапе напрямую по ключу
    @Override
    public int getResearch(ResearchKey key) {
        if (key == null) return 0;
        return researchMap.getOrDefault(key, 0);
    }

    // Старый метод проверки "Изучено ли" тоже переводим на рельсы ключа
    @Override
    public boolean isResearched(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return isResearched(new ResearchKey(stack));
    }

    // ТОТ САМЫЙ НОВЫЙ МЕТОД: Проверяет изученность без посредничества ItemStack
    @Override
    public boolean isResearched(ResearchKey key) {
        if (key == null) return false;

        // Для расчета необходимого количества всё равно нужен стак, создаем его из ключа
        ItemStack dummyStack = key.createItemStack();
        int required = JourneyUtils.getRequiredAmount(dummyStack);

        return getResearch(key) >= required;
    }

    @Override
    public int getProgress() {
        int total = 0;
        // Берем только значения мапы (Integer), игнорируя ключи
        for (int amount : this.researchMap.values()) {
            total += amount;
        }
        return total;
    }

    @Override
    public boolean getAutoResearchState() {
        return this.AutoResearchState;
    }

    /// --- На всякий случай ---
    @Override
    public Map<ResearchKey, Integer> getReadOnlyMap() {
        return Collections.unmodifiableMap(researchMap);
    }

    /// --- Стирание информации ---
    @Override
    public void clear() {
        researchMap.clear();
    }

    @Override
    public boolean remove(ItemStack stack) {
        researchMap.remove(new ResearchKey(stack));
        return true;
    }

    // --- Остальное ---
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
