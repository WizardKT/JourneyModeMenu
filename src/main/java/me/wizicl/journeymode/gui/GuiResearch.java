package me.wizicl.journeymode.gui;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.gui.base.GuiBase;
import me.wizicl.journeymode.gui.base.GuiItemSlot;
import me.wizicl.journeymode.gui.base.IGuiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GuiResearch extends GuiBase {

    private final List<ItemStack> researchedStacks = new ArrayList<>();

    public void updateCache() {
        this.researchedStacks.clear();

        IResearch cap = Minecraft.getMinecraft().player.getCapability(ResearchProvider.RESEARCH, null);
        if (cap != null) {
            for (String itemID : cap.getResearchMap().keySet()) {
                Item item = Item.getByNameOrId(itemID);
                if (item != null) {
                    this.researchedStacks.add(new ItemStack(item));
                }
            }
        }
    }

    @Override
    public void initGui() {
        this.updateCache();
        super.initGui();
    }

    public GuiResearch() {
        super(250, 180);
    }

    @Override
    public void addComponents() {
        for (int i = 0; i < researchedStacks.size(); i++) {
            int posX = guiLeft + 10 + (i % 10) * 18;
            int posY = guiTop + 30 + (i / 10) * 18;

            this.components.add (new GuiItemSlot(this,posX, posY, 18, researchedStacks.get(i)));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void drawTooltips(int mouseX, int mouseY) {
        for (IGuiElement component : components) {
            if (component instanceof GuiItemSlot && component.isHovered(mouseX, mouseY)) {
                renderToolTip(((GuiItemSlot) component).getStack(), mouseX, mouseY);
            }
        }
    }
}
