package me.wizicl.journeymode.client.gui;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchKey;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.network.MessageGuiAction;
import me.wizicl.journeymode.network.MessageOpenResearchGui;
import me.wizicl.journeymode.proxy.ClientProxy;
import me.wizicl.journeymode.util.JourneyUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Mouse;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static me.wizicl.journeymode.proxy.CommonProxy.NETWORK;

public class GuiResearch extends GuiContainer {

    private static final ResourceLocation TEXTURE_RESEARCH = new ResourceLocation("journeymode", "textures/gui/container/research.png");
    private static final ResourceLocation TEXTURE_GIVE = new ResourceLocation("journeymode", "textures/gui/container/give.png");
    private final GuiResearchContainer researchContainer;
    private ItemStack hoveredCatalogStack = null;
    private final List<ItemStack> guiItemCache = new ArrayList<>();

    private static final int PROGRESS_X = 74;
    private static final int PROGRESS_Y = 10;
    private static final int PROGRESS_WIDTH = 64;
    private static final int PROGRESS_HEIGHT = 16;

    private static final int BTN_X = 83;
    private static final int BTN_Y = 53;
    private static final int BTN_WIDTH = 52;
    private static final int BTN_HEIGHT = 12;

    private static final int THUMB_X_POS = 180;
    private static final int THUMB_Y_MIN = 100;
    private static final int THUMB_Y_MAX = 136;

    private static final int THUMB_WIDTH = 14;
    private static final int THUMB_HEIGHT = 14;
    private static final int THUMB_U = 0;
    private static final int THUMB_V = 166;

    private static final int BG_TRACK_U = 176;
    private static final int BG_TRACK_V = 93;
    private static final int BG_TRACK_WIDTH = 37;
    private static final int BG_TRACK_HEIGHT = 71;
    private static final int BG_TRACK_X = 176;
    private static final int BG_TRACK_Y = 93;

    private float currentScroll = 0.0F;
    private boolean isScrolling = false;
    private String lastResearchLog = "";
    private long logExpireTime = 0;
    private float animationStartTicks = 0;
    private float marqueeTimer = 0.0f;

    private GuiState currentState = GuiState.RESEARCH;

    public GuiResearch(GuiResearchContainer container) {
        super(container);
        this.researchContainer = container;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiItemCache.clear();
        for (ResearchKey key : ClientProxy.CACHED_UNLOCKED_ITEMS) {
            ItemStack stack = key.createItemStack();
            if (!stack.isEmpty()) {
                this.guiItemCache.add(stack);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Логика бегущей строки (RESEARCH)
        if (this.researchContainer.getCurrentState() == GuiState.RESEARCH) {
            if (this.researchContainer.isResearchSlotEmpty()) {
                String textToShow;

                if (!this.lastResearchLog.isEmpty() && System.currentTimeMillis() < this.logExpireTime) {
                    textToShow = this.lastResearchLog;
                    this.marqueeTimer += partialTicks;
                } else {
                    textToShow = new TextComponentTranslation("gui.journeymode.tooltip").getFormattedText();
                    this.marqueeTimer = 0.0f;
                }

                int globalX = this.guiLeft + PROGRESS_X;
                int globalY = this.guiTop + PROGRESS_Y;
                this.drawMarqueeText(textToShow, globalX, globalY, PROGRESS_WIDTH, PROGRESS_HEIGHT, 4210752, this.marqueeTimer, 2.0f);
            }
        }

        // Отрисовка предметов в меню (GIVE)
        if (this.currentState == GuiState.GIVE) {
            GlStateManager.pushMatrix();
            GlStateManager.enableDepth();

            this.drawGiveMenuContent(this.guiLeft, this.guiTop, mouseX, mouseY);
            if (hoveredCatalogStack != null) {
                this.renderToolTip(hoveredCatalogStack, mouseX, mouseY);
            }
            GlStateManager.popMatrix();
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 500.0F);
        GlStateManager.enableDepth();
        GlStateManager.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
        this.renderHoveredToolTip(mouseX, mouseY);
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();
    }


    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.currentState == GuiState.RESEARCH) {
            this.mc.getTextureManager().bindTexture(TEXTURE_RESEARCH);
        } else if (this.currentState == GuiState.GIVE) {
            this.mc.getTextureManager().bindTexture(TEXTURE_GIVE);
        }

        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        this.drawStateButtons(guiLeft, guiTop, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (this.researchContainer.getCurrentState() == GuiState.RESEARCH) {
            if (!this.researchContainer.isResearchSlotEmpty()) {
                IResearch cap = Minecraft.getMinecraft().player.getCapability(ResearchProvider.RESEARCH, null);
                if (cap == null) return;

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

    private void drawGiveMenuContent(int guiLeft, int guiTop, int mouseX, int mouseY) {
        int columns = 9;
        int visibleRows = 6;
        int totalRows = (int) Math.ceil((double) ClientProxy.CACHED_UNLOCKED_ITEMS.size() / columns);

        int maxScrollableRows = totalRows - visibleRows;
        if (maxScrollableRows < 0) maxScrollableRows = 0;

        int startRow = (int) (this.currentScroll * maxScrollableRows);
        int startIndex = startRow * columns;

        RenderHelper.enableGUIStandardItemLighting();

        int gridStartX = 8;
        int gridStartY = 30;
        int slotSize = 18;

        hoveredCatalogStack = null;
        for (int i = 0; i < (visibleRows * columns); i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= this.guiItemCache.size()) break;

            ItemStack stack = this.guiItemCache.get(itemIndex);

            int row = i / columns;
            int col = i % columns;
            int renderX = guiLeft + gridStartX + (col * slotSize);
            int renderY = guiTop + gridStartY + (row * slotSize);

            this.itemRender.renderItemAndEffectIntoGUI(stack, renderX, renderY);
            this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, stack, renderX, renderY, null);

            if (mouseX >= renderX && mouseX < renderX + 16 && mouseY >= renderY && mouseY < renderY + 16) {
                hoveredCatalogStack = stack;
            }
        }

        RenderHelper.disableStandardItemLighting();

        this.mc.getTextureManager().bindTexture(TEXTURE_GIVE);
        this.drawTexturedModalRect(guiLeft + BG_TRACK_X, guiTop + BG_TRACK_Y, BG_TRACK_U, BG_TRACK_V, BG_TRACK_WIDTH, BG_TRACK_HEIGHT);

        int travelRange = THUMB_Y_MAX - THUMB_Y_MIN;
        int thumbRenderY = THUMB_Y_MIN + (int) (travelRange * this.currentScroll);

        this.drawTexturedModalRect(guiLeft + THUMB_X_POS, guiTop + thumbRenderY, THUMB_U, THUMB_V, THUMB_WIDTH, THUMB_HEIGHT);
        this.updateHoverAndScrolling(guiLeft, guiTop, mouseX, mouseY, THUMB_X_POS, THUMB_Y_MIN, travelRange, THUMB_HEIGHT, THUMB_WIDTH);
    }

    private void updateHoverAndScrolling(int guiLeft, int guiTop, int mouseX, int mouseY, int trackX, int trackYStart, int availableHeight, int thumbHeight, int thumbWidth) {
        int localY = mouseY - guiTop;

        if (!Mouse.isButtonDown(0)) {
            this.isScrolling = false;
        }

        if (this.isScrolling) {
            float mousePosInTrack = (float) (localY - trackYStart - (thumbHeight / 2)) / (float) availableHeight;
            this.currentScroll = Math.max(0.0F, Math.min(1.0F, mousePosInTrack));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.currentState == GuiState.GIVE && this.hoveredCatalogStack != null && !this.hoveredCatalogStack.isEmpty()) {
            int clickMode = -1;

            if (mouseButton == 0) {
                if (GuiScreen.isShiftKeyDown()) {
                    clickMode = 1;
                } else {
                    clickMode = 0;
                }
            } else if (mouseButton == 2) {
                clickMode = 2;
            }

            if (clickMode != -1) {
                this.playButtonPressSound();
                NETWORK.sendToServer(new MessageGuiAction(this.hoveredCatalogStack, clickMode));
                return;
            }
        }

        if (mouseButton == 0) {
            int localX = mouseX - this.guiLeft;
            int localY = mouseY - this.guiTop;

            if (this.currentState == GuiState.GIVE) {
                if (localX >= THUMB_X_POS && localX < THUMB_X_POS + THUMB_WIDTH && localY >= THUMB_Y_MIN && localY < THUMB_Y_MAX) {
                    this.isScrolling = true;
                }
            }

            if (this.currentState == GuiState.RESEARCH) {
                if (localX >= BTN_X && localX < BTN_X + BTN_WIDTH && localY >= BTN_Y && localY < BTN_Y + BTN_HEIGHT) {
                    if (!this.researchContainer.isResearchSlotEmpty()) {
                        this.playButtonPressSound();
                        NETWORK.sendToServer(new MessageGuiAction(MessageGuiAction.ActionType.DO_RESEARCH));
                    }
                    return;
                }
            }

            int btnX = 179;
            int giveBtnY = 18;
            int researchBtnY = 56;
            int btnSize = 32;

            if (localX >= btnX && localX < btnX + btnSize && localY >= giveBtnY && localY < giveBtnY + btnSize) {
                if (this.currentState != GuiState.GIVE) {
                    this.playButtonPressSound();
                    this.currentState = GuiState.GIVE;
                    this.researchContainer.switchState(GuiState.GIVE);
                    NETWORK.sendToServer(new MessageGuiAction(GuiState.GIVE.ordinal()));
                }
                return;
            }
            else if (localX >= btnX && localX < btnX + btnSize && localY >= researchBtnY && localY < researchBtnY + btnSize) {
                if (this.currentState != GuiState.RESEARCH) {
                    this.playButtonPressSound();
                    this.currentState = GuiState.RESEARCH;
                    this.researchContainer.switchState(GuiState.RESEARCH);
                    NETWORK.sendToServer(new MessageGuiAction(GuiState.RESEARCH.ordinal()));
                }
                return;
            }
        }
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }


    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        if (this.currentState == GuiState.GIVE) {
            int dWheel = Mouse.getEventDWheel();
            if (dWheel != 0) {
                int columns = 9;
                int rows = 6;
                int totalRows = (int) Math.ceil((double) ClientProxy.CACHED_UNLOCKED_ITEMS.size() / columns);
                int hiddenRows = totalRows - rows;

                if (hiddenRows > 0) {
                    float scrollStep = 1.0F / (float) hiddenRows;
                    if (dWheel > 0) {
                        this.currentScroll -= scrollStep;
                    } else {
                        this.currentScroll += scrollStep;
                    }
                    this.currentScroll = Math.max(0.0F, Math.min(1.0F, this.currentScroll));
                }
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (GuiScreen.isCtrlKeyDown() && keyCode == ClientProxy.keyBindDelItem.getKeyCode()) {
            if (this.currentState == GuiState.GIVE && hoveredCatalogStack != null) {
                NETWORK.sendToServer(new MessageGuiAction(hoveredCatalogStack));
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void playButtonPressSound() {
        this.mc.getSoundHandler().playSound(
                net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(
                        net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F
                )
        );
    }

    private void drawStateButtons(int guiLeft, int guiTop, int mouseX, int mouseY) {
        int decorX = 176;
        int decorY = 10;
        int decorWidth = 56;
        int decorHeight = 82;

        this.drawTexturedModalRect(guiLeft + decorX, guiTop + decorY, decorX, decorY, decorWidth, decorHeight);

        int btnX = 179;
        int researchBtnY = 18;
        int giveBtnY = 56;
        int btnSize = 32;

        int researchU = 179;
        int researchV = 18;
        this.drawTexturedModalRect(guiLeft + btnX, guiTop + researchBtnY, researchU, researchV, btnSize, btnSize);

        int giveU = 179;
        int giveV = 56;
        this.drawTexturedModalRect(guiLeft + btnX, guiTop + giveBtnY, giveU, giveV, btnSize, btnSize);

        // Опционально: здесь можно добавить отрисовку ховера (подсветки кнопки при наведении),
        // сдвигая U или V координату текстуры, если isHovered == true.
    }

    public void drawMarqueeText(String text, int x, int y, int fieldWidth, int fieldHeight, int color, float animationTimer, float speed) {
        if (text == null || text.isEmpty()) return;

        int textWidth = this.fontRenderer.getStringWidth(text);
        int textY = y + (fieldHeight - this.fontRenderer.FONT_HEIGHT) / 2;

        if (textWidth <= fieldWidth) {
            int centerX = x + (fieldWidth - textWidth) / 2;
            int centerY = y + (fieldHeight - 8) / 2;
            this.fontRenderer.drawString(text, centerX, centerY, color);
            return;
        }

        float ticks = animationTimer;
        int totalDistance = fieldWidth + textWidth;
        float offset = (ticks * speed) % totalDistance;
        float textX = (x + fieldWidth) - offset;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution scaleRes = new ScaledResolution(mc);
        int scale = scaleRes.getScaleFactor();

        int scissorX = x * scale;
        int scissorY = (mc.displayHeight) - ((y + fieldHeight) * scale);
        int scissorW = fieldWidth * scale;
        int scissorH = fieldHeight * scale;

        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(scissorX, scissorY, scissorW, scissorH);

        GlStateManager.pushMatrix();
        this.fontRenderer.drawString(text, (int) textX, textY, color);
        GlStateManager.popMatrix();

        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }

    public void updateResearchLog(ResearchKey key, int amount) {
        if (key == null) return;

        this.marqueeTimer = 0.0f;
        this.animationStartTicks = Minecraft.getMinecraft().player.ticksExisted;

        Item item = Item.REGISTRY.getObject(key.getRegistryName());
        String displayName = (item != null) ? new ItemStack(item, 1, key.getMeta()).getDisplayName() : key.getRegistryName().toString();

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
        this.initGui();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.lastResearchLog = "";
        this.logExpireTime = 0;
        this.marqueeTimer = 0.0f;
    }
}
