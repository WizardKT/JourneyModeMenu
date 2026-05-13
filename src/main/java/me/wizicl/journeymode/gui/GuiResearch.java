package me.wizicl.journeymode.gui;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiResearch extends GuiScreen {

    /// Вводим переменные размера окна
    protected int xSize = 250;
    protected int ySize = 180;

    /// Вводим переменные левого верхнего угла окна
    protected int guiLeft;
    protected int guiTop;

    /// Вводим переменные данных слота интерфейса
    int slotSize = 18; // Стандартный слот инвентаря
    int padding = 5;
    int currentX = guiLeft + 10;
    int currentY = guiTop + 30;

    /// Создаём список для предметов
    private final List<ItemStack> allResearchedItems = new ArrayList<>(); // Список всез предметов в интерфейсе

    /// Вызываем метод запуска интерфейса
    @Override
    public void initGui() {
        super.initGui();

        // Высчитывание центра экрана
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        this.updateCache();
    }

    /// Отрисовываем интерфейс
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        // Задний фон и область интерфейса, рамка интерфейса, надпись названия мода
        this.drawDefaultBackground();
        drawGradientRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xF0101010, 0xF0252525);
        this.drawHorizontalLine(guiLeft, guiLeft + xSize, guiTop, 0xFFFFFFFF);
        this.fontRenderer.drawString("Journey Mode",(this.width / 2) - 24, guiTop + padding, 0xFFFFFFFF);

        // Переменная предмета под курсором
        ItemStack hoveredStack = ItemStack.EMPTY;

        // Переменная капы игрока
        IResearch cap = Minecraft.getMinecraft().player.getCapability(ResearchProvider.RESEARCH, null);

        // Извлечение предметов из капы
        for (int i = 0; i < allResearchedItems.size(); i++) {
            ItemStack stack = allResearchedItems.get(i);

            int x = guiLeft + 10 + (i % 10) * 18;
            int y = guiTop + 30 + (i / 10) * 18;

            drawSlot(x, y, 18, mouseX, mouseY);
            drawItemStack(stack, x + 1, y + 1);

            // Проверка позиции курсора = позиции предмета
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                hoveredStack = stack;
            }
            i++;
        }

        // Отрисовка тултипа под предметом
        if (!hoveredStack.isEmpty()) {
            this.renderToolTip(hoveredStack, mouseX, mouseY);
        }

        // Вызываем интерфейс
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /// Обновление списка предметов
    private void updateCache() {
        this.allResearchedItems.clear();

        IResearch cap = Minecraft.getMinecraft().player.getCapability(ResearchProvider.RESEARCH, null);
        if (cap != null) {
            int i = 0;
            for (String itemId : cap.getResearchMap().keySet()) {
                Item item = Item.getByNameOrId(itemId);
                if (item != null) {
                    this.allResearchedItems.add(new ItemStack(item));
                }
            }
        }
    }

    /// Отрисовка предметов в интерфейсе
    private void drawItemStack(ItemStack stack, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.renderItemIntoGUI(stack, x, y);
        this.itemRender.renderItemOverlays(this.fontRenderer, stack, x, y);
        RenderHelper.disableStandardItemLighting();
    }

    /// Отрисовка слотов под предметы
    public void drawSlot(int x, int y, int size, int mouseX, int mouseY) {

        // Проверка позиции курсора
        boolean isHovered = mouseX >= x && mouseY >= y && mouseX < x + size && mouseY < y + size;

        // Отрисовка основного фона слота
        drawRect(x, y, x + size, y + size, 0xFFFFFFFF);

        // Цвет рамки вокруг слота
        int borderColor = isHovered ? 0xFFFFFFFF : 0xFF555555;

        // Отрисовка рамки вокруг слота
        this.drawHorizontalLine(x, x + size - 1, y, borderColor); // Верх
        this.drawHorizontalLine(x, x + size - 1, y + size - 1, borderColor); // Низ
        this.drawVerticalLine(x, y, y + size - 1, borderColor); // Лево
        this.drawVerticalLine(x + size - 1, y, y + size - 1, borderColor); // Право
    }

    /// Обработка нажатий курсором
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Проверка клик левой кнопкой мыши
        if (mouseButton == 0) {
            IResearch cap = Minecraft.getMinecraft().player.getCapability(ResearchProvider.RESEARCH, null);
            if (cap != null) {
                int i = 0;
                for (String itemId : cap.getResearchMap().keySet()) {
                    int x = guiLeft + 10 + (i % 10) * 18;
                    int y = guiTop + 30 + (i / 10) * 18;

                    if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                        this.onItemClicked(itemId);
                        break;
                    }
                }
            }
        }
    }

    /// Метод вызываемый кликом курсора
    protected void onItemClicked(String itemId) {
        Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Ты выбрал: " + itemId));
    }


}
