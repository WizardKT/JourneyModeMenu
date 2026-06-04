package wizicl.mre.util;

import wizicl.mre.capabilities.IResearch;
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

public class JourneyUtils {

    /// Сколько надо на изучение предмета
    public static int getRequiredAmount(ItemStack stack) {

        // Если пусто - выводить 0
        if (stack.isEmpty()) return 0;

        // Добавляем переменную максимального стака
        int max = stack.getMaxStackSize();

        // Умножаем стак на коэффициент сложности
        return max * ConfigMain.costMultiplier;
    }

    /// Обновление капы в реальном времени
    public static void addResearchAndSync(EntityPlayer player, ItemStack stack, int amount) {
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
        if (cap != null) {

            // Обновление данных капы
            cap.addResearch(stack, amount);

            // Если игрок в мультиплеере, отправляем пакет данных
            if (!player.world.isRemote) {
                CommonProxy.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap(), cap.getAutoResearchState()), (EntityPlayerMP) player);
            }
        }
    }

    // Движок сравнения (Компаратор подмножеств)
    public static boolean isNbtSubset(NBTBase required, NBTBase actual) {
        // Если базе ничего не нужно — подходит любой мусор
        if (required == null) return true;
        // Если базе что-то нужно, а предмет пустой — отказ
        if (actual == null) return false;

        // Типы тегов должны совпадать (Compound с Compound, Int с Int)
        if (required.getId() != actual.getId()) return false;

        if (required instanceof NBTTagCompound) {
            NBTTagCompound reqComp = (NBTTagCompound) required;
            NBTTagCompound actComp = (NBTTagCompound) actual;

            // Проверяем КАЖДЫЙ тег, который требует база
            for (String key : reqComp.getKeySet()) {
                if (!actComp.hasKey(key)) return false; // Нет обязательного тега
                if (!isNbtSubset(reqComp.getTag(key), actComp.getTag(key))) return false; // Тег есть, но значения не совпали
            }
            return true; // В actComp может быть еще +100 других тегов, мы их просто игнорируем!
        }
        else if (required instanceof NBTTagList) {
            NBTTagList reqList = (NBTTagList) required;
            NBTTagList actList = (NBTTagList) actual;

            if (reqList.tagCount() > actList.tagCount()) return false;

            // Для списков (например, чары) проверяем поэлементно
            for (int i = 0; i < reqList.tagCount(); i++) {
                if (!isNbtSubset(reqList.get(i), actList.get(i))) return false;
            }
            return true;
        }
        else {
            // Базовые типы (String, Int, Float) должны совпадать точно
            return required.equals(actual);
        }
    }
}
