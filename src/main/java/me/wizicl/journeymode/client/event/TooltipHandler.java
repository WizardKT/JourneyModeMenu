package me.wizicl.journeymode.client.event;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.protecter.StackValidator;
import me.wizicl.journeymode.util.JourneyUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
public class TooltipHandler {
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {

        /// === Базовая защита от дурака ===

        ItemStack stack = event.getItemStack();
        EntityPlayer player = event.getEntityPlayer();
        if (!StackValidator.check(player, stack).get()) return;

        IResearch cap = event.getEntityPlayer().getCapability(ResearchProvider.RESEARCH, null);
        if (cap == null) return;

        /// === Основная часть системы тултипов отображения ===

        int meta = stack.getMetadata();
        List<String> tooltip = event.getToolTip();
        int currentResearched = cap.getResearch(stack);
        int maxRequired = JourneyUtils.getRequiredAmount(stack);

        if (currentResearched >= maxRequired) {
            TextComponentTranslation text = new TextComponentTranslation("tooltip.journeymode.researchFull");
            text.getStyle().setColor(TextFormatting.GREEN);
            tooltip.add(text.getFormattedText());
        } else if (currentResearched > 0){
            TextComponentTranslation text = new TextComponentTranslation("tooltip.journeymode.research");
            text.getStyle().setColor(TextFormatting.GRAY);
            tooltip.add(text.getFormattedText() + TextFormatting.AQUA + " " + currentResearched + TextFormatting.GRAY + " / " + maxRequired);
        } else {
                TextComponentTranslation text = new TextComponentTranslation("tooltip.journeymode.notResearched");
                text.getStyle().setColor(TextFormatting.RED);
                tooltip.add(text.getFormattedText() + TextFormatting.DARK_GRAY + " (" + TextFormatting.AQUA + "0" + TextFormatting.DARK_GRAY + " / " + maxRequired + ")");
        }
    }
}
