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

/// Интерфейс для работы с исследованием
// Используется для хранения и управления исследованием игрока
public class Research implements IResearch {

    /**
     * Управление исследованиями
     */

    /// Создаём Map для хранения исследований
    // ResearchKey - это ключ, который представляет собой уникальную комбинацию предмета и его метаданных
    // Integer - это количество исследованного предмета

    // ItemStackHandler - это класс, который реализует IItemHandler и представляет собой инвентарь с фиксированным количеством слотов
    // В данном случае используем его для хранения исследовательского предмета
    private final Map<ResearchKey, Integer> researchMap = new HashMap<>();
    private final ItemStackHandler researchInventory = new ItemStackHandler(1);
    private boolean autoResearchState = false;

    /// Добавление исследования
    // Добавляет исследование для указанного предмета и количество.
    // Возвращает количество исследованного предмета, которое было добавлено.
    // Количество не может превышать максимальное требуемое для предмета.
    @Override
    public int addResearch(ItemStack stack, int amount) {

        // Проверяем, пуст ли предмет или количество исследований не положительно.
        // Если да, возвращаем 0.
        if (stack.isEmpty() || amount <= 0) return 0;

        // Получаем ключ для предмета и максимальное количество исследований, которое можно добавить.
        var key = new ResearchKey(stack);
        int maxRequired = getRequiredAmount(stack);
        int current = researchMap.getOrDefault(key, 0);

        // Если текущее количество исследований уже равно или превышает максимальное, возвращаем 0.
        if (current >= maxRequired) return 0;

        // Вычисляем, сколько исследований можно добавить.
        // Если количество исследований, которое можно добавить, превышает оставшееся требуемое количество,
        //   то устанавливаем полное требуемое количество.
        // В противном случае добавляем указанное количество.
        int allowedToAdd = Math.min(amount, maxRequired - current);
        researchMap.put(key, current + allowedToAdd);
        return allowedToAdd;
    }

    /// Установка исследования
    // Устанавливает исследование для указанного предмета в указанное количество.
    // Количество может превышать максимальное требуемое для предмета.
    @Override
    public void setResearch(ItemStack stack, int amount)  {
        if (stack.isEmpty()) return;
        researchMap.put(new ResearchKey(stack), amount);
    }

    /// Установка состояния автоматического исследования
    @Override
    public boolean setAutoResearchState(boolean state) {
        return this.autoResearchState = state;
    }

    /// Переключение состояния автоматического исследования
    @Override
    public boolean toggleAutoResearchState() {
        this.autoResearchState = !this.autoResearchState;
        return this.autoResearchState;
    }

    /**
     * Получение информации
     */

    /// Получение прогресса исследования предмета.
    // Возвращает значение от 0 до максимального количества требуемых исследований.
    // Если предмет не исследуется, возвращает 0.
    @Override
    public int getResearch(ItemStack stack) {

        // Проверка на пустой стак.
        if (stack.isEmpty()) return 0;

        // Получаем данные предмета для сборки полного ключа.
        var targetId = stack.getItem().getRegistryName();
        int targetMeta = stack.getMetadata();
        var targetNbt = stack.getTagCompound();

        /// Сбор полного ключа для предмета.
        int bestProgress = 0;

        // Проход по всему словарю и поиск совпадений.
        for (var entry : researchMap.entrySet()) {
            var Key = entry.getKey();

            // Быстрая проверка: Сравниваем идентификатор и метаданные предмета.
            if (Key.getRegistryName().equals(targetId) && Key.getMeta() == targetMeta) {

                // Сравниваем NBT-данные предмета. Если они совпадают, то считаем это верным совпадением.
                // Обязательно используем чистые NBT-данные для корректного сравнения.
                // Иначе они могут быть "загрязнены" и не совпадать.
                if (JourneyUtils.isNbtSubset(Key.getCleanedNbt(), targetNbt)) {

                    // Если совпадение найдено, то выдаём путь к предмету и его прогресс исследования.
                    bestProgress = Math.max(bestProgress, entry.getValue());
                }
            }
        }

        return bestProgress;
    }

    /// Получения объекта исследования
    @Override
    public int getResearch(ResearchKey key) {
        return key == null ? 0 : researchMap.getOrDefault(key, 0);
    }

    /// Проверка на исследование предмета или ключа исследования.
    // Специальная для Стэка
    // Если исследовано, возвращает прогресс. Если нет - возвращает 0.
    @Override
    public boolean isResearched(ItemStack stack) {
        return !stack.isEmpty() && isResearched(new ResearchKey(stack));
    }

    /// Проверка на исследование ключа исследования.
    // Специальная для Ключа
    // Если исследовано, возвращает прогресс. Если нет - возвращает 0.
    @Override
    public boolean isResearched(ResearchKey key) {
        if (key == null) return false;

        var dummyStack = key.createItemStack();
        int required = JourneyUtils.getRequiredAmount(dummyStack);

        return getResearch(key) >= required;
    }

    /// Получение общего прогресса исследования.
    @Override
    public int getProgress() {
        return researchMap.values().stream().mapToInt(Integer::intValue).sum();
    }

    /// Получение состояния кнопки автоматического исследования.
    @Override
    public boolean getAutoResearchState() {
        return this.autoResearchState;
    }

    /// Получение карты исследования.
    @Override
    public Map<ResearchKey, Integer> getReadOnlyMap() {
        return Collections.unmodifiableMap(researchMap);
    }

    /// Сброс всего прогресса исследования.
    @Override
    public void clear() {
        researchMap.clear();
    }

    /// Удаление исследования по предмету. Возвращает true если исследование было удалено.
    @Override
    public boolean remove(ItemStack stack) {
        researchMap.remove(new ResearchKey(stack));
        return true;
    }

    /// Получение требуемого количества для исследования предмета. Возвращает 0 если не требуется исследование.
    public int getRequiredAmount(ItemStack stack) {
        return JourneyUtils.getRequiredAmount(stack);
    }

    /// Синхронизация данных с сервера. Вызывается только на клиенте.
    public void refreshFromServer(Map<ResearchKey, Integer> newData) {
        this.researchMap.clear();
        this.researchMap.putAll(newData);
    }

    /// Возвращает доступ к инвентарю для хранения исследуемых предметов.
    @Override
    public IItemHandler getResearchInventory() {
        return this.researchInventory;
    }

    /// Сериализация данных в формат NBT. Вызывается при сохранении игры.
    public NBTTagCompound serializeNBT() {
        return ResearchSerializer.serialize(this);
    }

    /// Десериализация данных из формата NBT. Вызывается при загрузке игры.
    public void deserializeNBT(NBTTagCompound nbt) {
        ResearchSerializer.deserialize(this, nbt);
    }
}
