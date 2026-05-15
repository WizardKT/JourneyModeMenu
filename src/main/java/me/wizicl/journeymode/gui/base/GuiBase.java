package me.wizicl.journeymode.gui.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class GuiBase extends GuiScreen {

    /// Все переменные
    protected int xSize, ySize, guiLeft, guiTop, scrollAmount = 0, maxScrollAmount = 100;
    protected List<IGuiElement> components = new ArrayList<>();

    public GuiBase(int xSize, int ySize) {
        this.xSize = xSize;
        this.ySize = ySize;
    }

    /// Запуск GUI
    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        this.components.clear();
        this.addComponents();
    }

    /// Добавление компанентов
    protected abstract void addComponents();

    /// Отрисовка GUI
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        this.drawDefaultBackground();
        drawBackground(mouseX, mouseY, partialTicks);
        cutScissor(guiLeft + 5, guiTop + 30, xSize - 10, ySize - 40);

        for (IGuiElement component : this.components) {
            component.draw(mouseX, mouseY, partialTicks);
        }

        // Отрисовка тултипов
        drawTooltips(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /// Отрисовка заднего фона
    protected void drawBackground(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xF0101010, 0xF0252525);
    }

    /// Отрисовка тултипов
    protected abstract void drawTooltips(int mouseX, int mouseY);

    /// Проверка нажатия курсора
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (IGuiElement component : this.components) {
            component.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    /// Отрисовка предметов
    protected void drawItemStack(ItemStack stack, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.renderItemIntoGUI(stack, x, y);
        this.itemRender.renderItemOverlays(this.fontRenderer, stack, x, y);
        RenderHelper.disableStandardItemLighting();
    }

    /// Отрисовка слота
    protected void drawSlot(int x, int y, int size, int mouseX, int mouseY) {
        int color = isHovered(x, y, size, mouseX, mouseY) ? 0x80FFFFFF : 0x80000000;
        drawRect(x, y, x + size, y + size, color);
    }

    /// Проверка позиции курсора
    private boolean isHovered(int x, int y, int size, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    /// Скроллбар
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            if (dWheel > 0) dWheel = -1; // Прокрутка вверх
            else dWheel = 1; // Прокрутка вниз

            this.scrollAmount += dWheel * 15;

            if (this.scrollAmount < 0) this.scrollAmount = 0;
            if (this.scrollAmount > maxScrollAmount) this.scrollAmount = maxScrollAmount;
        }
    }

    /// Вырезание вылезающих элементов
    protected void cutScissor(int x, int y, int width, int height) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Minecraft mc = Minecraft.getMinecraft();
        int scale = new ScaledResolution(mc).getScaleFactor();

        GL11.glScissor(x * scale, (mc.displayHeight - (y + height) * scale), width * scale, height * scale);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
