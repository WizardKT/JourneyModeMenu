package me.wizicl.journeymode.main;

import me.wizicl.journeymode.proxy.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

public class GuiResearch extends GuiContainer {

    /// --- Отрисовка меню ---

    private static final ResourceLocation BACKGROUND = new ResourceLocation("journeymode", "textures/gui/container/research.png");

    public GuiResearch(ResearchContainer container) {
        super(container);

        // Размеры картинки фона
        this.xSize = 176;
        this.ySize = 166;
    }

    /**
     * ЭТАП 1: Инициализация. Вызывается при открытии или изменении размера окна игры.
     */

    @Override
    public void initGui() {
        super.initGui();

        // guiLeft и guiTop — это координаты верхнего левого угла окна,
    }

    /**
     * ЭТАП 2: Главный метод отрисовки. Вызывается каждый кадр.
     */

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        // Серый полупрозрачный фон
        this.drawDefaultBackground();

        // Отрисовка слотов и фона
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Всплывающие подсказки над предметами
        this.renderHoveredToolTip(mouseX, mouseY);

    }

    /**
     * ЭТАП 3: Отрисовка заднего слоя (текстуры).
     */

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BACKGROUND);

        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    /**
     * ЭТАП 4: Отрисовка переднего слоя (текст, индикаторы).
     * Рисуется поверх текстуры и слотов, но ПОД мышкой.
     */

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString("Research", 8, 6, 4210752);
    }
}
