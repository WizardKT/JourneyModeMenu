package wizicl.mre.capabilities;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.Map;

public interface IResearch {

    /// Управление исследованиями
    int addResearch(ItemStack stack, int amount);
    void setResearch(ItemStack stack, int amount);
    boolean setAutoResearchState(boolean state);
    boolean toggleAutoResearchState();

    /// Получение информации
    // По стеку
    int getResearch(ItemStack stack);
    boolean isResearched(ItemStack stack);

    // По ключу
    int getResearch(ResearchKey key);
    boolean isResearched(ResearchKey key);

    // Из карты
    Map<ResearchKey, Integer> getReadOnlyMap();

    /// Прогресс-бар
    int getProgress();
    boolean getAutoResearchState();

    /// Стирание информации
    void clear();
    boolean remove(ItemStack stack);

    /// Инвентарь исследовательского слота
    IItemHandler getResearchInventory();
}
