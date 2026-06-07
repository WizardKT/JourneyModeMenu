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

public class JourneyUtils {

    // Сколько надо на изучение предмета
    public static int getRequiredAmount(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        // Коротко и ясно через var
        var max = stack.getMaxStackSize();
        return max * ConfigMain.costMultiplier;
    }

    // Обновление капы в реальном времени
    public static void addResearchAndSync(EntityPlayer player, ItemStack stack, int amount) {
        var cap = player.getCapability(ResearchProvider.RESEARCH, null);
        if (cap == null) return;

        // Обновление данных капы
        cap.addResearch(stack, amount);

        // Если игрок в мультиплеере, отправляем пакет данных
        if (!player.world.isRemote && player instanceof EntityPlayerMP playerMP) {
            CommonProxy.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap(), cap.getAutoResearchState()), playerMP);
        }
    }

    // Движок сравнения (Компаратор подмножеств)
    public static boolean isNbtSubset(NBTBase required, NBTBase actual) {
        if (required == null) return true;  // Если базе ничего не нужно — подходит всё
        if (actual == null) return false;   // Если базе что-то нужно, а у предмета нет — отказ
        if (required.getId() != actual.getId()) return false; // Типы тегов обязаны совпадать

        switch (required) {

            // Проверяем тип required, и ЕСЛИ actual тоже является компаундом, заходим сюда
            case NBTTagCompound reqComp when actual instanceof NBTTagCompound -> {
                var actComp = (NBTTagCompound) actual; // Чистый и быстрый каст одной строкой

                // Проверяем КАЖДЫЙ тег, который требует база
                for (var key : reqComp.getKeySet()) {
                    if (!actComp.hasKey(key)) return false; // Нет обязательного тега
                    if (!isNbtSubset(reqComp.getTag(key), actComp.getTag(key))) return false; // Значения не совпали
                }
                return true;
            }

            case NBTTagList reqList when actual instanceof NBTTagList -> {
                var actList = (NBTTagList) actual;

                if (reqList.tagCount() > actList.tagCount()) return false;

                // Для списков проверяем поэлементно
                for (int i = 0; i < reqList.tagCount(); i++) {
                    if (!isNbtSubset(reqList.get(i), actList.get(i))) return false;
                }
                return true;
            }

            // Для всех остальных базовых типов (String, Int, Float и т.д.)
            default -> {
                return required.equals(actual);
            }
        }
    }
}
