package wizicl.mre.gui.view;

import wizicl.mre.capabilities.IResearch;
import wizicl.mre.capabilities.ResearchKey;
import wizicl.mre.capabilities.ResearchProvider;
import wizicl.mre.gui.controller.GuiResearchContainer;
import wizicl.mre.gui.controller.JellyBarMath;
import wizicl.mre.gui.controller.JellyMesh2D;
import wizicl.mre.gui.model.GuiState;
import wizicl.mre.gui.model.SortType;
import wizicl.mre.metaprogress.JourneyLeveling;
import wizicl.mre.network.MessageGuiAction;
import wizicl.mre.proxy.ClientProxy;
import wizicl.mre.util.JourneyUtils;
import wizicl.mre.gui.controller.SearchHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static wizicl.mre.metaprogress.JourneyLeveling.calculateCurrentLevel;
import static wizicl.mre.proxy.CommonProxy.NETWORK;

public class GuiResearch extends GuiContainer {

    // [ПОДСКАЗКА]: Класс Layout — это "Конфиг визуальной части".
    // Больше никаких магических чисел, разбросанных по коду! Если нужно что-то подвинуть — меняем только здесь.
    private static final class Layout {
        // Желе-прогресс-бар
        static final int BAR_X = 8, BAR_Y = 9, BAR_H = 71 , WATER_X = 0, WATER_Y = 185;

        // Прогресс-бар исследований
        static final int PROGRESS_X = 78, PROGRESS_Y = 12, PROGRESS_W = 60, PROGRESS_H = 10;

        // Кнопки состояний (Справа)
        static final int DECOR_X = 176, DECOR_Y = 10, DECOR_W = 56, DECOR_H = 82;
        static final int STATE_BTN_X = 177, STATE_BTN_SIZE = 32;
        static final int BTN_GIVE_Y = 17, BTN_RESEARCH_Y = 57;

        // Кнопка "Изучить" (Внутри меню)
        static final int BTN_DO_RESEARCH_X = 83, BTN_DO_RESEARCH_Y = 53, BTN_DO_RESEARCH_W = 52, BTN_DO_RESEARCH_H = 12;

        // Скролл-бар
        static final int THUMB_X = 159, THUMB_Y_MIN = 39, THUMB_Y_MAX = 97, THUMB_W = 8, THUMB_H = 28;
        static final int THUMB_U = 0, THUMB_V = 168;
        static final int BG_TRACK_X = 176, BG_TRACK_Y = 93, BG_TRACK_W = 37, BG_TRACK_H = 71;
        static final int BG_TRACK_U = 176, BG_TRACK_V = 93;

        // Поиск и сортировка
        static final int SEARCH_X = 99, SEARCH_Y = 16;
        static final int SORT_BTN_Y = 14, SORT_BTN_SIZE = 12;
        static final int SORT_BTN_A_X = 13, SORT_BTN_ID_X = 30, SORT_BTN_MI_X = 47;

        // Сетка предметов
        static final int GRID_COLS = 8, GRID_ROWS = 6;
        static final int GRID_START_X = 8, GRID_START_Y = 30, SLOT_SIZE = 18;
    }

    private static final ResourceLocation TEXTURE_RESEARCH = new ResourceLocation("journeymode", "textures/gui/container/research.png");
    private static final ResourceLocation TEXTURE_GIVE = new ResourceLocation("journeymode", "textures/gui/container/give.png");

    // --- Переменные состояния GUI ---
    private final GuiResearchContainer researchContainer;
    private final List<ItemStack> guiItemCache = new ArrayList<>();

    private GuiState currentState = GuiState.RESEARCH;
    private SortType currentSortType = SortType.ALPHABETICAL;

    private GuiTextField searchField;
    private ItemStack hoveredCatalogStack = null;

    private float currentScroll = 0.0F;
    private boolean isScrolling = false;

    // --- Переменные бегущей строки ---
    private String lastResearchLog = "";
    private long logExpireTime = 0;
    private float animationStartTicks = 0;
    private float marqueeTimer = 0.0f;

    // --- Переменные прогресс бара ---
    private final JellyMesh2D fluidMesh = new JellyMesh2D(36, 71, 6, 10);
    private final JellyBarMath jellyBar = new JellyBarMath();
    private int previousLevel = -1;

    public GuiResearch(GuiResearchContainer container) {
        super(container);
        this.researchContainer = container;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        this.searchField = new GuiTextField(0, this.fontRenderer, this.guiLeft + Layout.SEARCH_X, this.guiTop + Layout.SEARCH_Y, 69, 12);
        this.searchField.setMaxStringLength(20);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setVisible(true);
        this.searchField.setTextColor(16777215);

        this.rebuildItemCache();
    }

    /// ===================================================================================
    /// РЕНДЕР
    /// ===================================================================================

    @Override
    public void updateScreen() {
        super.updateScreen();

        if (this.currentState == GuiState.RESEARCH) {

            IResearch cap = Minecraft.getMinecraft().player.getCapability(ResearchProvider.RESEARCH, null);
            int totalResearched = cap.getProgress();

            // 1. Узнаем текущий уровень
            int currentLevel = calculateCurrentLevel(totalResearched);

            // Если меню уже прогрузилось (не -1) и текущий уровень больше прошлого
            if (previousLevel != -1 && currentLevel > previousLevel) {
                // ТУТ СРАБАТЫВАЕТ ТРИГГЕР!
                // Этот код выполнится ровно ОДИН раз в момент левел-апа.
                System.out.println("БУЛЬК! Получен уровень: " + currentLevel);

                // Принудительно кидаем воду вниз, чтобы она не уползала плавно
                jellyBar.forceValue(0f);
                this.mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.
                        getMasterRecord(SoundEvents.ENTITY_FIREWORK_BLAST, 1.0F));

            }

            if (currentLevel < JourneyLeveling.MAX_LEVEL) {
                // 2. Считаем, сколько нужно для следующего апа
                int currentLevelReq = JourneyLeveling.getItemsRequiredForLevel(currentLevel);
                int nextLevelReq = JourneyLeveling.getItemsRequiredForLevel(currentLevel + 1);

                int itemsNeeded = nextLevelReq - currentLevelReq;
                int itemsGot = totalResearched - currentLevelReq;

                // 3. Высчитываем пиксели для желе (от 0 до Layout.WATER_Y)
                float waterTarget = ((float) itemsGot / itemsNeeded) * Layout.BAR_H;
                jellyBar.setTarget(waterTarget);
            } else {
                // 100-й уровень! Вода всегда на максимуме
                jellyBar.setTarget(Layout.BAR_H);
            }

            previousLevel = currentLevel;

            // 1. Двигатель обновляет математику плавности
            jellyBar.updateTick();

            // 2. Получаем текущую высоту и сплющиваем сетку
            int ticksExisted = this.mc.player.ticksExisted;
            float renderHeight = jellyBar.getRenderSize(ticksExisted, 0); // partialTicks в updateScreen не нужны

            // Защита от выхода за рамки
            if (renderHeight > Layout.BAR_H) renderHeight = Layout.BAR_H;
            if (renderHeight < 0) renderHeight = 0;

            fluidMesh.updateWaterLevel(renderHeight);

            // 3. Считаем физику волн от курсора
            int mouseX = Mouse.getX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
            fluidMesh.updatePhysics(mouseX, mouseY, guiLeft, guiTop, Layout.BAR_X, Layout.BAR_Y);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (this.currentState == GuiState.RESEARCH && this.researchContainer.isResearchSlotEmpty()) {
            this.drawMarqueeLogic(partialTicks);
        }

        if (this.currentState == GuiState.GIVE) {
            this.searchField.drawTextBox();

            GlStateManager.pushMatrix();
            GlStateManager.enableDepth();
            this.drawGiveMenuContent(this.guiLeft, this.guiTop, mouseX, mouseY);
            if (this.hoveredCatalogStack != null) {
                this.renderToolTip(this.hoveredCatalogStack, mouseX, mouseY);
            }
            GlStateManager.popMatrix();
        }

        // Рендер тултипа поверх всего остального
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
        }
        else if (this.currentState == GuiState.GIVE) this.mc.getTextureManager().bindTexture(TEXTURE_GIVE);

        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        // Отрисовка декоративной панели и кнопок состояний
        this.drawTexturedModalRect(guiLeft + Layout.DECOR_X, guiTop + Layout.DECOR_Y, Layout.DECOR_X, Layout.DECOR_Y, Layout.DECOR_W, Layout.DECOR_H);
        this.drawTexturedModalRect(guiLeft + Layout.STATE_BTN_X, guiTop + Layout.BTN_RESEARCH_Y, 179, 18, Layout.STATE_BTN_SIZE, Layout.STATE_BTN_SIZE);
        this.drawTexturedModalRect(guiLeft + Layout.STATE_BTN_X, guiTop + Layout.BTN_GIVE_Y, 179, 56, Layout.STATE_BTN_SIZE, Layout.STATE_BTN_SIZE);

        if (this.currentState == GuiState.RESEARCH) {

            // Включаем прозрачность (85%)
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            // [МАТЕМАТИКА ЦВЕТА]
            // 1. Достаем капу и прогресс (точно так же, как в updateScreen)
            IResearch cap = net.minecraft.client.Minecraft.getMinecraft().player.getCapability(
                    wizicl.mre.capabilities.ResearchProvider.RESEARCH, null);
            int totalResearched = cap.getProgress();

            // 2. Узнаем уровень
            int currentLevel = calculateCurrentLevel(totalResearched);

            // 3. Вычисляем RGB градиент (100 - это макс. уровень для цвета)
            float[] currentColor = calculateWaterColor(currentLevel, 100);

            // Устанавливаем динамический цвет + 85% прозрачности (0.85f)
            GlStateManager.color(currentColor[0], currentColor[1], currentColor[2], 0.85f);

            // Рисуем сетку
            drawDistortedFluid(guiLeft + Layout.BAR_X, guiTop + Layout.BAR_Y, Layout.WATER_X, Layout.WATER_Y);

            // Сбрасываем всё
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.disableBlend();

            // На всякий случай возвращаем бинд текстуры
            this.mc.getTextureManager().bindTexture(TEXTURE_RESEARCH);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (this.currentState == GuiState.RESEARCH && !this.researchContainer.isResearchSlotEmpty()) {
            IResearch cap = Minecraft.getMinecraft().player.getCapability(ResearchProvider.RESEARCH, null);
            if (cap == null) return;

            ItemStack stack = this.researchContainer.getResearchTargetStack();
            String infoText = cap.getResearch(stack) + "/" + JourneyUtils.getRequiredAmount(stack);

            int textWidth = this.fontRenderer.getStringWidth(infoText);
            int textX = Layout.PROGRESS_X + (Layout.PROGRESS_W - textWidth) / 2;
            int textY = Layout.PROGRESS_Y + (Layout.PROGRESS_H - 8) / 2;

            this.fontRenderer.drawString(infoText, textX, textY, 4210752);
        }
    }

    private void drawGiveMenuContent(int guiLeft, int guiTop, int mouseX, int mouseY) {
        int totalRows = (int) Math.ceil((double) this.guiItemCache.size() / Layout.GRID_COLS);
        int maxScrollableRows = Math.max(0, totalRows - Layout.GRID_ROWS);

        int startRow = (int) (this.currentScroll * maxScrollableRows);
        int startIndex = startRow * Layout.GRID_COLS;

        RenderHelper.enableGUIStandardItemLighting();
        this.hoveredCatalogStack = null;

        // [ПОДСКАЗКА]: Отрисовка сетки. Числа берем из Layout, логика чистая.
        for (int i = 0; i < (Layout.GRID_ROWS * Layout.GRID_COLS); i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= this.guiItemCache.size()) break;

            ItemStack stack = this.guiItemCache.get(itemIndex);
            int renderX = guiLeft + Layout.GRID_START_X + ((i % Layout.GRID_COLS) * Layout.SLOT_SIZE);
            int renderY = guiTop + Layout.GRID_START_Y + ((i / Layout.GRID_COLS) * Layout.SLOT_SIZE);

            this.itemRender.renderItemAndEffectIntoGUI(stack, renderX, renderY);
            this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, stack, renderX, renderY, null);

            if (mouseX >= renderX && mouseX < renderX + 16 && mouseY >= renderY && mouseY < renderY + 16) {
                this.hoveredCatalogStack = stack;
            }
        }
        RenderHelper.disableStandardItemLighting();

        // Отрисовка скролл-бара
        this.mc.getTextureManager().bindTexture(TEXTURE_GIVE);
        this.drawTexturedModalRect(guiLeft + Layout.BG_TRACK_X, guiTop + Layout.BG_TRACK_Y, Layout.BG_TRACK_U, Layout.BG_TRACK_V, Layout.BG_TRACK_W, Layout.BG_TRACK_H);

        int travelRange = Layout.THUMB_Y_MAX - Layout.THUMB_Y_MIN;
        int thumbRenderY = Layout.THUMB_Y_MIN + (int) (travelRange * this.currentScroll);

        this.drawTexturedModalRect(guiLeft + Layout.THUMB_X, guiTop + thumbRenderY, Layout.THUMB_U, Layout.THUMB_V, Layout.THUMB_W, Layout.THUMB_H);

        // Обновление логики перетаскивания скролла
        if (this.isScrolling) {
            float mousePosInTrack = (float) ((mouseY - guiTop) - Layout.THUMB_Y_MIN - (Layout.THUMB_H / 2)) / travelRange;
            this.currentScroll = Math.max(0.0F, Math.min(1.0F, mousePosInTrack));
        }
        if (!Mouse.isButtonDown(0)) this.isScrolling = false;
    }

    private void drawDistortedFluid(int x, int y, int texU, int texV) {
        // В 1.12.2 текстуры измеряются в нормализованных координатах (0.0 - 1.0).
        // Стандартная текстура меню обычно 256x256 пикселей.
        float textureScale = 1f / 256f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);

        Tessellator tessellator = Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(org.lwjgl.opengl.GL11.GL_QUADS, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);

        int cols = fluidMesh.getCols();
        int rows = fluidMesh.getRows();

        // Рисуем сетку по квадратам (квадам)
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols - 1; c++) {
                // Индексы 4 углов текущего квадратика
                int iTL = r * cols + c;          // Верх-Лево
                int iTR = iTL + 1;               // Верх-Право
                int iBL = (r + 1) * cols + c;    // Низ-Лево
                int iBR = iBL + 1;               // Низ-Право

                // Просчитываем UV координаты для этого конкретного квадрата.
                // Опираемся на идеальную сетку (baseX/baseY), чтобы текстура не "убегала"
                float u0 = (texU + (c * (fluidMesh.width / (cols - 1)))) * textureScale;
                float u1 = (texU + ((c + 1) * (fluidMesh.width / (cols - 1)))) * textureScale;
                float v0 = (texV + (r * (fluidMesh.height / (rows - 1)))) * textureScale;
                float v1 = (texV + ((r + 1) * (fluidMesh.height / (rows - 1)))) * textureScale;

                // Строим квад, используя искаженные физикой X и Y (curX, curY)
                // И рисуем против часовой стрелки (TL -> BL -> BR -> TR)
                buffer.pos(fluidMesh.curX[iTL], fluidMesh.curY[iTL], this.zLevel).tex(u0, v0).endVertex();
                buffer.pos(fluidMesh.curX[iBL], fluidMesh.curY[iBL], this.zLevel).tex(u0, v1).endVertex();
                buffer.pos(fluidMesh.curX[iBR], fluidMesh.curY[iBR], this.zLevel).tex(u1, v1).endVertex();
                buffer.pos(fluidMesh.curX[iTR], fluidMesh.curY[iTR], this.zLevel).tex(u1, v0).endVertex();
            }
        }

        tessellator.draw();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f); // Сбрасываем цвет обратно
        GlStateManager.popMatrix();

    }

    private float[] calculateWaterColor(int level, int maxLevel) {
        // Ограничиваем уровень, чтобы цвет не ушел в минус после капа
        float progress = Math.min((float) level / maxLevel, 1.0f);

        // Цвет 1: Стартовый (Почти белый/светло-сиреневый)
        float startR = 0.9f, startG = 0.9f, startB = 1.0f;

        // Цвет 2: Финальный (Сочный фиолетовый)
        float endR = 0.6f, endG = 0.1f, endB = 0.9f;

        // Математика смешивания (Lerp)
        float r = startR + (endR - startR) * progress;
        float g = startG + (endG - startG) * progress;
        float b = startB + (endB - startB) * progress;

        return new float[]{r, g, b};
    }

    /// ===================================================================================
    /// МАРШРУТИЗАТОР КЛИКОВ (CLICK ROUTER)
    /// ===================================================================================

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.currentState == GuiState.GIVE) {
            this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        // Обработка ЛКМ по элементам интерфейса
        if (mouseButton == 0) {
            int localX = mouseX - this.guiLeft;
            int localY = mouseY - this.guiTop;

            // [ПОДСКАЗКА]: Диспетчер проверяет компоненты по очереди.
            // Если метод вернул true, значит клик обработан, и мы выходим (return).
            if (this.handleSortClick(localX, localY)) return;
            if (this.handleStateSwitchClick(localX, localY)) return;
            if (this.handleScrollClick(localX, localY)) return;
            if (this.handleActionClick(localX, localY)) return;
        }

        // Обработка клика по предмету в сетке (ЛКМ, ПКМ, Колесико)
        if (this.handleItemClick(mouseButton)) return;

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean handleSortClick(int localX, int localY) {
        if (this.currentState != GuiState.GIVE) return false;
        if (localY < Layout.SORT_BTN_Y || localY >= Layout.SORT_BTN_Y + Layout.SORT_BTN_SIZE) return false;

        if (localX >= Layout.SORT_BTN_A_X && localX < Layout.SORT_BTN_A_X + Layout.SORT_BTN_SIZE) {
            this.setSortType(SortType.ALPHABETICAL);
            return true;
        } else if (localX >= Layout.SORT_BTN_ID_X && localX < Layout.SORT_BTN_ID_X + Layout.SORT_BTN_SIZE) {
            this.setSortType(SortType.ID);
            return true;
        } else if (localX >= Layout.SORT_BTN_MI_X && localX < Layout.SORT_BTN_MI_X + Layout.SORT_BTN_SIZE) {
            this.setSortType(SortType.SUPER_SORT);
            return true;
        }
        return false;
    }

    private boolean handleStateSwitchClick(int localX, int localY) {
        if (localX < Layout.STATE_BTN_X || localX >= Layout.STATE_BTN_X + Layout.STATE_BTN_SIZE) return false;

        if (localY >= Layout.BTN_GIVE_Y && localY < Layout.BTN_GIVE_Y + Layout.STATE_BTN_SIZE) {
            if (this.currentState != GuiState.GIVE) this.switchGuiState(GuiState.GIVE);
            return true;
        } else if (localY >= Layout.BTN_RESEARCH_Y && localY < Layout.BTN_RESEARCH_Y + Layout.STATE_BTN_SIZE) {
            if (this.currentState != GuiState.RESEARCH) this.switchGuiState(GuiState.RESEARCH);
            return true;
        }
        return false;
    }

    private boolean handleScrollClick(int localX, int localY) {
        if (this.currentState != GuiState.GIVE) return false;
        if (localX >= Layout.THUMB_X && localX < Layout.THUMB_X + Layout.THUMB_W &&
                localY >= Layout.THUMB_Y_MIN && localY < Layout.THUMB_Y_MAX) {
            this.isScrolling = true;
            return true;
        }
        return false;
    }

    private boolean handleActionClick(int localX, int localY) {
        if (this.currentState == GuiState.RESEARCH && !this.researchContainer.isResearchSlotEmpty()) {
            if (localX >= Layout.BTN_DO_RESEARCH_X && localX < Layout.BTN_DO_RESEARCH_X + Layout.BTN_DO_RESEARCH_W &&
                    localY >= Layout.BTN_DO_RESEARCH_Y && localY < Layout.BTN_DO_RESEARCH_Y + Layout.BTN_DO_RESEARCH_H) {
                this.playButtonPressSound();
                NETWORK.sendToServer(new MessageGuiAction(MessageGuiAction.ActionType.DO_RESEARCH));
                return true;
            }
        }
        return false;
    }

    private boolean handleItemClick(int mouseButton) {
        if (this.currentState != GuiState.GIVE || this.hoveredCatalogStack == null || this.hoveredCatalogStack.isEmpty()) return false;

        int clickMode = -1;
        if (mouseButton == 0) clickMode = GuiScreen.isShiftKeyDown() ? 1 : 0;
        else if (mouseButton == 2) clickMode = 2;

        if (clickMode != -1) {
            this.playButtonPressSound();
            // Оборачиваем стак в ResearchKey при создании пакета
            NETWORK.sendToServer(new MessageGuiAction(new ResearchKey(this.hoveredCatalogStack), clickMode));
            return true;
        }
        return false;
    }

    /// ===================================================================================
    /// ЛОГИКА ВВОДА И ОБНОВЛЕНИЯ ДАННЫХ
    /// ===================================================================================

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (this.currentState == GuiState.GIVE) {
            int dWheel = Mouse.getEventDWheel();
            if (dWheel != 0) {
                int totalRows = (int) Math.ceil((double) this.guiItemCache.size() / Layout.GRID_COLS);
                int hiddenRows = totalRows - Layout.GRID_ROWS;

                if (hiddenRows > 0) {
                    float scrollStep = 1.0F / (float) hiddenRows;
                    this.currentScroll += (dWheel > 0) ? -scrollStep : scrollStep;
                    this.currentScroll = Math.max(0.0F, Math.min(1.0F, this.currentScroll));
                }
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.currentState == GuiState.GIVE && this.searchField.isFocused()) {
            String oldText = this.searchField.getText();
            this.searchField.textboxKeyTyped(typedChar, keyCode);

            if (!oldText.equals(this.searchField.getText())) this.rebuildItemCache();
            return;
        }

        if (GuiScreen.isCtrlKeyDown() && keyCode == ClientProxy.keyBindDelItem.getKeyCode()) {
            if (this.currentState == GuiState.GIVE && hoveredCatalogStack != null && !hoveredCatalogStack.isEmpty()) {
                // Оборачиваем стак в ResearchKey при отправке пакета на удаление
                NETWORK.sendToServer(new MessageGuiAction(new ResearchKey(this.hoveredCatalogStack)));
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void setSortType(SortType type) {
        this.playButtonPressSound();
        this.currentSortType = type;
        this.rebuildItemCache();
    }

    private void switchGuiState(GuiState state) {
        this.playButtonPressSound();
        this.currentState = state;
        this.researchContainer.switchState(state);
        NETWORK.sendToServer(new MessageGuiAction(state.ordinal()));
    }

    private void playButtonPressSound() {
        this.mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void rebuildItemCache() {
        this.guiItemCache.clear();
        String currentSearch = this.searchField != null ? this.searchField.getText() : "";

        ClientProxy.CACHED_UNLOCKED_ITEMS.stream()
                .map(ResearchKey::createItemStack)
                .filter(stack -> !stack.isEmpty())
                .filter(stack -> SearchHelper.isFuzzyMatch(stack.getDisplayName(), currentSearch))
                .sorted(this.currentSortType.getComparator())
                .forEach(this.guiItemCache::add);
        this.currentScroll = 0.0F;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.lastResearchLog = "";
        this.logExpireTime = 0;
        this.marqueeTimer = 0.0f;
        Keyboard.enableRepeatEvents(false);
    }

    /// ===================================================================================
    /// УТИЛИТЫ БЕГУЩЕЙ СТРОКИ (В ИДЕАЛЕ ВЫНЕСТИ В RenderUtils)
    /// ===================================================================================

    private void drawMarqueeLogic(float partialTicks) {
        String textToShow = (!this.lastResearchLog.isEmpty() && System.currentTimeMillis() < this.logExpireTime)
                ? this.lastResearchLog : new TextComponentTranslation("gui.journeymode.tooltip").getFormattedText();

        this.marqueeTimer = (!this.lastResearchLog.isEmpty() && System.currentTimeMillis() < this.logExpireTime)
                ? this.marqueeTimer + partialTicks : 0.0f;

        this.drawMarqueeText(textToShow, this.guiLeft + Layout.PROGRESS_X, this.guiTop + Layout.PROGRESS_Y, Layout.PROGRESS_W, Layout.PROGRESS_H, 4210752, this.marqueeTimer, 2.0f);
    }

    private void drawMarqueeText(String text, int x, int y, int fieldWidth, int fieldHeight, int color, float animationTimer, float speed) {
        if (text == null || text.isEmpty()) return;

        int textWidth = this.fontRenderer.getStringWidth(text);
        int textY = y + (fieldHeight - this.fontRenderer.FONT_HEIGHT) / 2;

        if (textWidth <= fieldWidth) {
            this.fontRenderer.drawString(text, x + (fieldWidth - textWidth) / 2, y + (fieldHeight - 8) / 2, color);
            return;
        }

        float offset = (animationTimer * speed) % (fieldWidth + textWidth);
        float textX = (x + fieldWidth) - offset;

        ScaledResolution scaleRes = new ScaledResolution(Minecraft.getMinecraft());
        int scale = scaleRes.getScaleFactor();

        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(x * scale, Minecraft.getMinecraft().displayHeight - ((y + fieldHeight) * scale), fieldWidth * scale, fieldHeight * scale);

        GlStateManager.pushMatrix();
        this.fontRenderer.drawString(text, (int) textX, textY, color);
        GlStateManager.popMatrix();

        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }

    public void updateResearchLog(ResearchKey key, int amount) {
        // ... (Тут остался твой оригинальный код форматирования лога, я его не трогал)
        if (key == null) return;
        this.marqueeTimer = 0.0f;
        this.animationStartTicks = Minecraft.getMinecraft().player.ticksExisted;

        Item item = Item.REGISTRY.getObject(key.getRegistryName());
        String displayName = (item != null) ? new ItemStack(item, 1, key.getMeta()).getDisplayName() : key.getRegistryName().toString();

        StringBuilder builder = new StringBuilder(new TextComponentTranslation("gui.journeymode.marquee.success", displayName, key.getMeta(), amount).getFormattedText());
        builder.append(key.getCleanedNbt() != null && !key.getCleanedNbt().isEmpty()
                ? new TextComponentTranslation("gui.journeymode.marquee.with_nbt", key.getCleanedNbt().toString()).getFormattedText()
                : new TextComponentTranslation("gui.journeymode.marquee.no_nbt").getFormattedText());

        this.lastResearchLog = builder.toString();
        this.logExpireTime = System.currentTimeMillis() + 7000;
        this.initGui();
    }
}