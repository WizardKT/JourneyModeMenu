package me.wizicl.journeymode;

import me.wizicl.journeymode.capabilities.IResearch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;

public class JourneyUtils {

    // Сколько надо на изучение предмета
    public static int getRequiredAmount(ItemStack stack) {

        // Если пусто - выводить 0
        if (stack.isEmpty()) return 0;

        // Добавляем переменную максимального стака
        int max = stack.getMaxStackSize();

        // Умножаем стак на коэффициент сложности
        return max * JourneyConfig.stackMult;
    }

    public static boolean researchFinished(EntityPlayer player, IResearch cap, ItemStack stack) {

        player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        return true;
    }
}
