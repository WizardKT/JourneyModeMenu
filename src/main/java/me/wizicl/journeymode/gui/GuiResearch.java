package me.wizicl.journeymode.gui;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.gui.base.GuiBase;
import me.wizicl.journeymode.gui.base.GuiItemSlot;
import me.wizicl.journeymode.gui.base.IGuiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiResearch extends GuiBase {

    GuiTextField searchField;
    private String lastFilter = "";
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
        searchField = new GuiTextField(0, Minecraft.getMinecraft().fontRenderer, guiLeft + 10, guiTop + 10, 150, 15);
        super.initGui();
    }

    public GuiResearch() {
        super(250, 180);
    }

    @Override
    public void addComponents() {
        searchField.setFocused(true);
        searchField.setCanLoseFocus(false);

        String filter = searchField.getText().toLowerCase();
        int i = 0;
        for (ItemStack stack : researchedStacks) {
            if (filter.isEmpty() || stack.getDisplayName().toLowerCase().contains(filter)) {
                int posX = guiLeft + 10 + (i % 10) * 18;
                int posY = guiTop + 30 + (i / 10) * 18;

                this.components.add(new GuiItemSlot(this, posX, posY, 18, stack));
                i++;
            }
        }

        int rows = (int) Math.ceil(i / 10.0);
        this.maxScrollAmount = Math.max(0, (rows * 18) - (ySize - 40));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawCenteredString(Minecraft.getMinecraft().fontRenderer,"Journey Mode", this.width / 2, this.height, 0xFFFFFF);
        searchField.drawTextBox();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.searchField.updateCursorCounter();
    }

    @Override
    public void drawTooltips(int mouseX, int mouseY) {
        for (IGuiElement component : components) {
            if (component instanceof GuiItemSlot && component.isHovered(mouseX, mouseY)) {
                renderToolTip(((GuiItemSlot) component).getStack(), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.searchField.textboxKeyTyped(typedChar, keyCode)) {
            this.components.clear();
            this.addComponents();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
