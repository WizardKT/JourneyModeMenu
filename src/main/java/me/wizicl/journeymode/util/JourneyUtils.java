package me.wizicl.journeymode.util;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchKey;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.config.ConfigMain;
import me.wizicl.journeymode.network.MessageSyncResearch;
import me.wizicl.journeymode.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public class JourneyUtils {

    public static int getRequiredAmount(ResearchKey key) {
        ItemStack stack = key.createItemStack();
        if (stack.isEmpty()) return 0;
        return getRequiredAmount(stack);
    }

    /// Сколько надо на изучение предмета
    public static int getRequiredAmount(ItemStack stack) {

        // Если пусто - выводить 0
        if (stack.isEmpty()) return 0;

        // Добавляем переменную максимального стака
        int max = stack.getMaxStackSize();

        // Умножаем стак на коэффициент сложности
        return max * ConfigMain.stackMultiplier;
    }

    /// Обновление капы в реальном времени
    public static void addResearchAndSync(EntityPlayer player, ItemStack stack, int amount) {
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
        if (cap != null) {

            // Обновление данных капы
            cap.addResearch(stack, amount);

            // Если игрок в мультиплеере, отправляем пакет данных
            if (!player.world.isRemote) {
                CommonProxy.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap()), (EntityPlayerMP) player);
            }
        }
    }
}
