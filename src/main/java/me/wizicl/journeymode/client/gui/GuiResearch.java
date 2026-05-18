package me.wizicl.journeymode.client.gui;

import me.wizicl.journeymode.JourneyMode;
import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.network.MessageRequestResearch;
import me.wizicl.journeymode.proxy.CommonProxy;
import me.wizicl.journeymode.util.JourneyUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

import java.io.IOException;

public class GuiResearch extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation("journeymode", "textures/gui/container/research.png");
    private final GuiResearchContainer researchContainer;

    private static final int PROGRESS_X = 74;
    private static final int PROGRESS_Y = 10;
    private static final int PROGRESS_WIDTH = 64;
    private static final int PROGRESS_HEIGHT = 16;

    private static final int BTN_X = 83;
    private static final int BTN_Y = 53;
    private static final int BTN_WIDTH = 52;
    private static final int BTN_HEIGHT = 12;

    public GuiResearch(GuiResearchContainer container) {
        super(container);
        this.researchContainer = container;
        this.xSize = 176;
        this.ySize = 166;
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
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (this.researchContainer.getCurrentState() == GuiState.RESEARCH) {
            String infoText;

            if (!this.researchContainer.isResearchSlotEmpty()) {
                IResearch cap = Minecraft.getMinecraft().player.getCapability(ResearchProvider.RESEARCH, null);
                ItemStack stack = this.researchContainer.getResearchTargetStack();

                int currentProgress = cap.getResearch(stack);
                int requiredAmount = JourneyUtils.getRequiredAmount(stack);

                infoText = currentProgress + "/" + requiredAmount;
            } else {
                infoText = new TextComponentTranslation("gui.journeymode.tooltip").getFormattedText();
            }

            int textWidth = this.fontRenderer.getStringWidth(infoText);
            int textX = PROGRESS_X + (PROGRESS_WIDTH - textWidth) / 2;
            int textY = PROGRESS_Y + (PROGRESS_HEIGHT - 8) / 2;

            this.fontRenderer.drawString(infoText, textX, textY, 4210752);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && this.researchContainer.getCurrentState() == GuiState.RESEARCH) {
            int localX = mouseX - this.guiLeft;
            int localY = mouseY - this.guiTop;

            if (localX >= BTN_X && localX < BTN_X + BTN_WIDTH && localY >= BTN_Y && localY < BTN_Y + BTN_HEIGHT) {

                if (!this.researchContainer.isResearchSlotEmpty()) {
                    this.playButtonPressSound();

                    CommonProxy.NETWORK.sendToServer(new MessageRequestResearch());
                }
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void playButtonPressSound() {
        this.mc.getSoundHandler().playSound(
                net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(
                        net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F
                )
        );
    }
}
