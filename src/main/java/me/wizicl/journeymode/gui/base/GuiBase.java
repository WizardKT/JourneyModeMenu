package me.wizicl.journeymode.gui.base;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class GuiBase extends GuiScreen {
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

    // Добавление кнопок и т.п.
    protected abstract void addComponents();

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // Отрисовка фона
        drawBackground(mouseX, mouseY, partialTicks);

        // Отрисовка компанентов
        for (IGuiElement component : this.components) {
            component.draw(mouseX, mouseY, partialTicks);
        }

        // Отрисовка тултипов
        drawTooltips(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected void drawBackground(int mouseX, int mouseY, float partialTicks) {
        // Стандартная рамка
        drawGradientRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xF0101010, 0xF0252525);
    }

    protected abstract void drawTooltips(int mouseX, int mouseY);

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (IGuiElement component : this.components) {
            component.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    protected void drawItemStack(ItemStack stack, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.renderItemIntoGUI(stack, x, y);
        this.itemRender.renderItemOverlays(this.fontRenderer, stack, x, y);
        RenderHelper.disableStandardItemLighting();
    }

    protected void drawSlot(int x, int y, int size, int mouseX, int mouseY) {
        int color = isHovered(x, y, size, mouseX, mouseY) ? 0x80FFFFFF : 0x80000000;
        drawRect(x, y, x + size, y + size, color);
    }

    private boolean isHovered(int x, int y, int size, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }
}
