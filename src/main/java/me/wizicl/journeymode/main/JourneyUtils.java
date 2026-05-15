package me.wizicl.journeymode.main;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.network.MessageSyncResearch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public class JourneyUtils {

    /// Сколько надо на изучение предмета
    public static int getRequiredAmount(ItemStack stack) {

        // Если пусто - выводить 0
        if (stack.isEmpty()) return 0;

        // Добавляем переменную максимального стака
        int max = stack.getMaxStackSize();

        // Умножаем стак на коэффициент сложности
        return max * JourneyConfig.stackMultiplier;
    }

    /// Обновление капы в реальном времени
    public static void addResearchAndSync(EntityPlayer player, String itemName, int amount) {
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
        if (cap != null) {

            // Обновление данных капы
            cap.addResearch(itemName, amount);

            // Если игрок в мультиплеере, отправляем пакет данных
            if (!player.world.isRemote) {
                JourneyMode.NETWORK.sendTo(new MessageSyncResearch(cap.getResearchMap()), (EntityPlayerMP) player);
            }
        }
    }
}
