package me.wizicl.journeymode.client.gui;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchKey;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.network.MessageRequestResearch;
import me.wizicl.journeymode.proxy.CommonProxy;
import me.wizicl.journeymode.util.JourneyUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.opengl.GL11;

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

    private String lastResearchLog = "";
    private long logExpireTime = 0;
    private float animationStartTicks = 0;
    private float marqueeTimer = 0.0f;

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

        if (this.researchContainer.getCurrentState() == GuiState.RESEARCH) {
            if (this.researchContainer.isResearchSlotEmpty()) {
                String textToShow;

                // Проверяем таймер лога
                if (!this.lastResearchLog.isEmpty() && System.currentTimeMillis() < this.logExpireTime) {
                    textToShow = this.lastResearchLog;
                    this.marqueeTimer += partialTicks;
                } else {
                    textToShow = new TextComponentTranslation("gui.journeymode.tooltip").getFormattedText();
                    this.marqueeTimer = 0.0f;
                }

                int globalX = this.guiLeft + PROGRESS_X;
                int globalY = this.guiTop + PROGRESS_Y;

                // Передаем эти очищенные тики в твою функцию отрисовки строки
                this.drawMarqueeText(textToShow, globalX, globalY, PROGRESS_WIDTH, PROGRESS_HEIGHT, 4210752, this.marqueeTimer, 2.0f);
            }
        }
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

            // Если в слоте есть предмет, рисуем обычные статичные цифры прогресса
            if (!this.researchContainer.isResearchSlotEmpty()) {
                IResearch cap = Minecraft.getMinecraft().player.getCapability(ResearchProvider.RESEARCH, null);
                ItemStack stack = this.researchContainer.getResearchTargetStack();

                int currentProgress = cap.getResearch(stack);
                int requiredAmount = JourneyUtils.getRequiredAmount(stack);

                String infoText = currentProgress + "/" + requiredAmount;

                int textWidth = this.fontRenderer.getStringWidth(infoText);
                int textX = PROGRESS_X + (PROGRESS_WIDTH - textWidth) / 2;
                int textY = PROGRESS_Y + (PROGRESS_HEIGHT - 8) / 2;

                this.fontRenderer.drawString(infoText, textX, textY, 4210752);
            }
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

    public void drawMarqueeText(String text, int x, int y, int fieldWidth, int fieldHeight, int color, float animationTimer, float speed) {
        if (text == null || text.isEmpty()) return;

        int textWidth = this.fontRenderer.getStringWidth(text);
        int textY = y + (fieldHeight - this.fontRenderer.FONT_HEIGHT) / 2;

        // Если текст целиком помещается в поле, просто рисуем его статично по центру
        if (textWidth <= fieldWidth) {
            int centerX = x + (fieldWidth - textWidth) / 2;
            int centerY = y + (fieldHeight - 8) / 2;
            this.fontRenderer.drawString(text, centerX, centerY, color);
            return;
        }

        // Используем тики игрока, чтобы скорость не зависела от частоты кадров (FPS)
        float ticks = animationTimer;

        // Полный цикл анимации: от момента, когда текст только появляется справа, до полного ухода влево
        int totalDistance = fieldWidth + textWidth;

        // Вычисляем текущее смещение внутри этого цикла
        float offset = (ticks * speed) % totalDistance;

        // Начальная точка рисования (справа от поля) минус смещение в левую сторону
        float textX = (x + fieldWidth) - offset;

        /// === Обрезка лишнего текста ===

        // Перенос координат GUI в пиксели монитора с учетом масштаба интерфейса.
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution scaleRes = new ScaledResolution(mc);
        int scale = scaleRes.getScaleFactor();

        // Переводим координаты GUI в экранные пиксели (OpenGL считает снизу вверх, поэтому Y инвертируем)
        int scissorX = x * scale;
        int scissorY = (mc.displayHeight) - ((y + fieldHeight) * scale);
        int scissorW = fieldWidth * scale;
        int scissorH = fieldHeight * scale;

        // Включаем ножницы и режем всё, что выходит за рамки прямоугольника информационного поля
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);

        // Рисуем сдвинутый текст
        GlStateManager.pushMatrix();
        this.fontRenderer.drawString(text, (int) textX, textY, color);
        GlStateManager.popMatrix();

        // Выключаем ножницы, чтобы не сломать отрисовку остального GUI
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void updateResearchLog(ResearchKey key, int amount) {
        if (key == null) return;

        this.marqueeTimer = 0.0f;
        this.animationStartTicks = net.minecraft.client.Minecraft.getMinecraft().player.ticksExisted;

        net.minecraft.item.Item item = net.minecraft.item.Item.REGISTRY.getObject(key.getRegistryName());
        String displayName = (item != null) ? new net.minecraft.item.ItemStack(item, 1, key.getMeta()).getDisplayName() : key.getRegistryName().toString();


        String baseInfo = new TextComponentTranslation("gui.journeymode.marquee.success", displayName, key.getMeta(), amount).getFormattedText();
        StringBuilder builder = new StringBuilder(baseInfo);

        if (key.getCleanedNbt() != null && !key.getCleanedNbt().hasNoTags()) {
            String nbtInfo = new TextComponentTranslation("gui.journeymode.marquee.with_nbt", key.getCleanedNbt().toString()).getFormattedText();
            builder.append(nbtInfo);
        } else {
            String noNbtInfo = new TextComponentTranslation("gui.journeymode.marquee.no_nbt").getFormattedText();
            builder.append(noNbtInfo);
        }

        this.lastResearchLog = builder.toString();
        this.logExpireTime = System.currentTimeMillis() + 7000;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();

        // Полностью стираем старый текст лога и обнуляем таймер при закрытии экрана
        this.lastResearchLog = "";
        this.logExpireTime = 0;
        this.marqueeTimer = 0.0f; // Обнуляем и здесь
    }
}
