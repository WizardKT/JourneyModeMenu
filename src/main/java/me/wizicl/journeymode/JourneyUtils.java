package me.wizicl.journeymode;

import net.minecraft.item.ItemStack;

public class JourneyUtils {

    public static int getRequiredAmount(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int max = stack.getMaxStackSize();

        return max * JourneyConfig.stackMult;
    }
}
