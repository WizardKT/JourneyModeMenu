package me.wizicl.journeymode.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import scala.swing.TextComponent;

public class GuiResearch extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation("journeymode", "textures/gui/container/research.png");

    private final GuiResearchContainer researchContainer;

    public GuiResearch(GuiResearchContainer container) {
        super(container);
        this.researchContainer = container;

        // Размеры картинки фона
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BACKGROUND);

        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        switch (this.researchContainer.getCurrentState()) {
            case RESEARCH:
                this.drawTexturedModalRect(guiLeft, guiTop, 176, 0, 16, 16);
                break;
            case CATEGORIES:
                break;
            case SEARCH:
                break;
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (this.researchContainer.getCurrentState() == GuiState.RESEARCH) {
            Slot researchSlot = this.researchContainer.inventorySlots.get(36);

            if (researchSlot != null && researchSlot.getHasStack()) {
                ItemStack currentItem = researchSlot.getStack();

                int currentProgress = 12;
                int requiredAmount = 50;

                String progressText = currentProgress + " / " + requiredAmount;

                int textWidth = this.fontRenderer.getStringWidth(progressText);
                this.fontRenderer.drawString(progressText, 88 - (textWidth / 2), 22, 4210752);
            }
        }

    }
    private void drawResearchBackgroundElements() {}
}
