package wizicl.mre.util;

import wizicl.mre.capabilities.ResearchProvider;
import wizicl.mre.config.ConfigMain;
import wizicl.mre.network.MessageSyncResearch;
import wizicl.mre.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/// Этот класс отвечает за дополнительные вспомогательные инструменты.
public class JourneyUtils {

    /// Метод для получения количества исследований для предмета.
    public static int getRequiredAmount(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        var max = stack.getMaxStackSize();
        return max * ConfigMain.costMultiplier;
    }

    /// Метод для добавления данных исследований и их синхронизации на клиенте.
    public static void addResearchAndSync(EntityPlayer player, ItemStack stack, int amount) {

        // Быстрая проверка на наличие компонента исследований у игрока.
        var cap = player.getCapability(ResearchProvider.RESEARCH, null);
        if (cap == null) return;

        cap.addResearch(stack, amount);

        // Если игрок в мультиплеере, отправляем пакет данных
        if (!player.world.isRemote && player instanceof EntityPlayerMP playerMP) {
            CommonProxy.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap(), cap.getAutoResearchState()), playerMP);
        }
    }

    /// Метод для проверки того, является ли нынешние данные актуальными.
    // Работает по принципу "если у нас есть что-то нужное и это не совпадает с тем, что уже есть, но содержит
    // одинаковую базу - то мы считаем, что данные актуальны."
    // Короче говоря: `required` — это то, что нужно проверить, `actual` — это то, что уже есть.
    // Если `required` — это `{a:1, b:2}`, а `actual` — это `{a:1, b:2, c:3}`, то данные актуальны.
    // Если же `required` — это `{a:1, b:2}`, а `actual` — это `{c:3}`, то данные НЕ актуальны.
    public static boolean isNbtSubset(NBTBase required, NBTBase actual) {
        if (required == null) return true;  // Если базе ничего не нужно — подходит всё
        if (actual == null) return false;   // Если базе что-то нужно, а у предмета нет — отказ
        if (required.getId() != actual.getId()) return false; // Если типы тегов не совпадают — отказ. Они должны быть одинаковыми.

        /// Проверка для каждого типа тега.
        // Используем switch expression, чтобы избежать множества if-ов.
        // Для каждого типа тега мы проверяем, что `actual` содержит все данные из `required`.
        switch (required) {

            // Для простых типов данных проводим сравнение значений.
            case NBTTagCompound reqComp when actual instanceof NBTTagCompound -> {
                var actComp = (NBTTagCompound) actual;

                // Для словарей проверяем наличие обязательных ключей и совпадение значений.
                for (var key : reqComp.getKeySet()) {
                    if (!actComp.hasKey(key)) return false; // Если ключа нет — отказ
                    if (!isNbtSubset(reqComp.getTag(key), actComp.getTag(key))) return false; // Если значения не совпадают — отказ
                }

                // Остальное - одобряем.
                return true;
            }

            // Для сложных типов данных с NBT проводим проверку поэлементно.
            case NBTTagList reqList when actual instanceof NBTTagList -> {
                var actList = (NBTTagList) actual;

                // Если списка нет или его длина меньше — отказ
                if (reqList.tagCount() > actList.tagCount()) return false;

                // Сравниваем элементы в списках по порядку.
                // Нужно найти хотя бы один совпадающий элемент для каждого элемента из запроса.
                for (int i = 0; i < reqList.tagCount(); i++) {
                    NBTBase reqElement = reqList.get(i);
                    boolean foundMatch = false;

                    // Идём глубже.
                    // Если вложенные списки совпадают - подтверждаем совпадение.
                    for (int j = 0; j < actList.tagCount(); j++) {
                        if (isNbtSubset(reqElement, actList.get(j))) {
                            foundMatch = true;
                            break;
                        }
                    }

                    // Всё остальное - отказ.
                    if (!foundMatch) return false;
                }

                // Остальное - одобряем.
                return true;
            }

            // Для всех остальных базовых типов (String, Int, Float и т.д.) используем обычное сравнение.
            default -> {
                return required.equals(actual);
            }
        }
    }
}
