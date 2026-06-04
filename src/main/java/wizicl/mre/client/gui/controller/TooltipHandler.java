package wizicl.mre.client.gui.controller;

import wizicl.mre.capabilities.IResearch;
import wizicl.mre.capabilities.ResearchKey;
import wizicl.mre.capabilities.ResearchProvider;
import wizicl.mre.protecter.StackValidator;
import wizicl.mre.util.JourneyUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
        ItemStack stack = event.getItemStack();
        EntityPlayer player = event.getEntityPlayer();
        if (!StackValidator.check(player, stack).get()) return;

        IResearch cap = event.getEntityPlayer().getCapability(ResearchProvider.RESEARCH, null);
        if (cap == null) return;

        ResearchKey key = new ResearchKey(stack);

        int meta = stack.getMetadata();
        List<String> tooltip = event.getToolTip();
        int currentResearched = cap.getResearch(key);
        int maxRequired = JourneyUtils.getRequiredAmount(key.createItemStack());

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

        /// === РЕЖИМ РАЗРАБОТЧИКА (ДЕБАГ) ===
        // Работает только если включены расширенные тултипы (F3+H) и зажат левый CTRL
        if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips && GuiScreen.isCtrlKeyDown()) {
            tooltip.add(""); // Пустая строка для отступа
            tooltip.add(TextFormatting.DARK_GRAY + "=== JM Debug ===");

            // 1. Оригинальные данные предмета
            tooltip.add(TextFormatting.GRAY + "ID: " + TextFormatting.WHITE + stack.getItem().getRegistryName());
            tooltip.add(TextFormatting.GRAY + "Meta: " + TextFormatting.WHITE + stack.getMetadata());

            // 2. Грязный NBT (то, что видит игра)
            if (stack.hasTagCompound()) {
                tooltip.add(TextFormatting.GRAY + "Raw NBT: " + TextFormatting.YELLOW + stack.getTagCompound().toString());
            } else {
                tooltip.add(TextFormatting.GRAY + "Raw NBT: " + TextFormatting.DARK_GRAY + "null");
            }

            // 3. Чистый NBT (то, что видит твой рентген-фильтр)
            NBTTagCompound cleaned = key.getCleanedNbt();
            if (cleaned != null) {
                tooltip.add(TextFormatting.GRAY + "Clean NBT: " + TextFormatting.GREEN + cleaned.toString());
            } else {
                tooltip.add(TextFormatting.GRAY + "Clean NBT: " + TextFormatting.DARK_GRAY + "null");
            }

            // 4. Статус в базе данных (сколько штук реально сохранено)
            int dbProgress = cap.getReadOnlyMap().getOrDefault(key, 0);
            tooltip.add(TextFormatting.GRAY + "In Database: " + TextFormatting.AQUA + dbProgress);

            tooltip.add(TextFormatting.DARK_GRAY + "================");
        }
    }
}
