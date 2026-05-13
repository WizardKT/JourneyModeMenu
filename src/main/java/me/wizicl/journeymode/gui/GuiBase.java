package me.wizicl.journeymode.gui;

import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;

public class GuiBase extends GuiScreen {
    protected int xSize, ySize, guiLeft, guiTop;

    protected List<IGuiElement> components = new ArrayList<>();

    public GuiBase(int xSize, int ySize) {
        this.xSize = xSize;
        this.ySize = ySize;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        this.components.clear();
        this.addComponents();
    }

    protected abstract void addComponents();

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        drawBackground(mouseX, mouseY, partialTicks);

        for (IGuiElement element : this.components) {
            components.draw(mouseX, mouseY);
        }

        drawTooltips(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected void drawBackground(int mouseX, int mouseY, float partialTicks) {

        drawGradientRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xF0101010, 0xF0252525);
    }

    protected abstract void drawTooltips(int mouseX, int mouseY);

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (IGuiElement component : this.components) {
            component.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }
}
