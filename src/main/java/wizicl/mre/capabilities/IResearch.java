package wizicl.mre.capabilities;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.Map;

public interface IResearch {
    // Управление исследованиями
    int addResearch(ItemStack stack, int amount);
    void setResearch(ItemStack stack, int amount);
    boolean setAutoResearchState(boolean state);
    boolean toggleAutoResearchState();

    // --- ПОЛУЧЕНИЕ ИНФОРМАЦИИ ---
    // Старые методы оставляем для совместимости с ванильными контейнерами
    int getResearch(ItemStack stack);
    boolean isResearched(ItemStack stack);

    // НОВЫЕ МЕТОДЫ: Пуленепробиваемые проверки напрямую через чистый ключ
    int getResearch(ResearchKey key);
    boolean isResearched(ResearchKey key);

    // На всякий случай
    Map<ResearchKey, Integer> getReadOnlyMap();

    // Для прогресс-бара
    int getProgress();
    boolean getAutoResearchState();

    // Стирание информации
    void clear();
    boolean remove(ItemStack stack);

    // Инвентарь исследовательского слота
    IItemHandler getResearchInventory();
}
