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
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GuiResearch extends GuiContainer {

    private static final ResourceLocation TEXTURE_RESEARCH = new ResourceLocation("journeymode", "textures/gui/container/research.png");
    private static final ResourceLocation TEXTURE_GIVE = new ResourceLocation("journeymode", "textures/gui/container/give.png");
    private final GuiResearchContainer researchContainer;

    private static final List<ItemStack> ALL_MINECRAFT_ITEMS = new ArrayList<>();
    private List<ItemStack> filteredItems = new ArrayList<>();

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

    @Override
    public void initGui() {
        super.initGui();

        // Заполняем кэш только один раз, если он пустой, чтобы не лагало
        if (ALL_MINECRAFT_ITEMS.isEmpty()) {
            for (Item item : Item.REGISTRY) {
                if (item == null) continue;

                // Получаем все подтипы предмета (шерсть, подблоки, доски и т.д.)
                NonNullList<ItemStack> subItems = NonNullList.create();
                item.getSubItems(CreativeTabs.SEARCH, subItems);

                for (ItemStack stack : subItems) {
                    if (!stack.isEmpty()) {
                        ALL_MINECRAFT_ITEMS.add(stack);
                    }
                }
            }
        }

        // При открытии меню отфильтрованный список равен полному
        this.filteredItems = new ArrayList<>(ALL_MINECRAFT_ITEMS);
    }

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

        if (this.currentState == GuiState.GIVE) {
            // Фикс Z-буфера: сбрасываем глубину, чтобы предметы рисовались в плоскости интерфейса
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.enableDepth();

            // Вызываем отрисовку нашей сетки
            this.drawGiveMenuContent(this.guiLeft, this.guiTop, mouseX, mouseY);

            net.minecraft.client.renderer.GlStateManager.disableDepth();
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }
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

    private void drawGiveMenuContent(int guiLeft, int guiTop, int mouseX, int mouseY) {
        // Достаём капу исследований игрока
        IResearch research = this.mc.player.getCapability(ResearchProvider.RESEARCH, null);
        if (research == null) return;

        // Фильтруем только ПОЛНОСТЬЮ изученные предметы (доступные для выдачи)
        List<ItemStack> unlockedItems = new ArrayList<>();

        // Перебираем карту прогресса
        for (Map.Entry<ResearchKey, Integer> entry : research.getReadOnlyMap().entrySet()) {
            ResearchKey key = entry.getKey();
            int currentProgress = entry.getValue();

            // Собираем ItemStack прямо из ключа!
            ItemStack stack = key.createItemStack();
            if (stack.isEmpty()) continue; // Пропускаем, если предмет почему-то не создался

            // Передаем созданный стек в твой лимитник
            int required = JourneyUtils.getRequiredAmount(stack);

            if (currentProgress >= required) {
                // Добавляем этот ItemStack в список доступных для отрисовки сетки
                unlockedItems.add(stack);
            }
        }

        /// === МАТЕМАТИКА СЕТКИ И СКРОЛЛБАРА ===
        int columns = 9; // 9 ячеек в один ряд интерфейса
        int visibleRows = 6; // Сколько рядов одновременно видно в окошке (подправь под свой GUI, например 6 или 8)

        // Всего рядов в нашем списке изученного
        int totalRows = (int) Math.ceil((double) unlockedItems.size() / columns);

        // Сколько рядов осталось "за кадром"
        int maxScrollableRows = totalRows - visibleRows;
        if (maxScrollableRows < 0) maxScrollableRows = 0;

        // Вычисляем индекс строки, с которой начнётся отрисовка предметов
        int startRow = (int) (this.currentScroll * maxScrollableRows);
        int startIndex = startRow * columns;

        // ОТРИСОВКА СЕТКИ ПРЕДМЕТОВ
        RenderHelper.enableGUIStandardItemLighting(); // Включаем ванильный свет для иконок предметов

        int gridStartX = 8;  // Относительный X левого верхнего угла сетки слотов
        int gridStartY = 30; // Относительный Y левого верхнего угла сетки слотов
        int slotSize = 18;   // Размер одной ячейки (16 пикселей предмет + 2 пикселя рамка)

        for (int i = 0; i < (visibleRows * columns); i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= unlockedItems.size()) break; // Если предметы закончились, выходим

            ItemStack stack = unlockedItems.get(itemIndex);

            // Считаем позицию слота на экране
            int row = i / columns;
            int col = i % columns;
            int renderX = guiLeft + gridStartX + (col * slotSize);
            int renderY = guiTop + gridStartY + (row * slotSize);

            // Рисуем сам предмет и количество (64 штуки или бесконечность — на твой выбор)
            this.itemRender.renderItemAndEffectIntoGUI(stack, renderX, renderY);
            this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, stack, renderX, renderY, null);

            // @papam: Если мышка наведена на этот предмет, можно сохранить его для отрисовки тултипа позже
            if (mouseX >= renderX && mouseX < renderX + 16 && mouseY >= renderY && mouseY < renderY + 16) {
                // Здесь в будущем можно вызвать renderToolTip(stack, mouseX, mouseY);
            }
        }

        RenderHelper.disableStandardItemLighting();

        /// === ОТРИСОВКА ПОЛЗУНКА СКРОЛЛБАРА ===

        this.mc.getTextureManager().bindTexture(TEXTURE_GIVE);
        this.drawTexturedModalRect(guiLeft + BG_TRACK_X, guiTop + BG_TRACK_Y, BG_TRACK_U, BG_TRACK_V, BG_TRACK_WIDTH, BG_TRACK_HEIGHT);

        // Высчитываем динамический Y для ползунка на экране
        int travelRange = THUMB_Y_MAX - THUMB_Y_MIN;
        int thumbRenderY = THUMB_Y_MIN + (int) (travelRange * this.currentScroll);

        // Рисуем ползунок поверх интерфейса
        this.drawTexturedModalRect(guiLeft + THUMB_X_POS, guiTop + thumbRenderY, THUMB_U, THUMB_V, THUMB_WIDTH, THUMB_HEIGHT);

        // Проверяем зажатие мыши для перетаскивания ползунка
        updateHoverAndScrolling(guiLeft, guiTop, mouseX, mouseY, THUMB_X_POS, THUMB_Y_MIN, travelRange, THUMB_HEIGHT, THUMB_WIDTH);
    }

    // Логика перетаскивания ползунка мышкой
    private void updateHoverAndScrolling(int guiLeft, int guiTop, int mouseX, int mouseY, int trackX, int trackYStart, int availableHeight, int thumbHeight, int thumbWidth) {
        int localY = mouseY - guiTop;

        // Если левая кнопка мыши отпущена — выключаем скроллинг
        if (!Mouse.isButtonDown(0)) {
            this.isScrolling = false;
        }

        // Если кнопка зажата, проверяем, тащим ли мы ползунок
        if (this.isScrolling) {
            float mousePosInTrack = (float) (localY - trackYStart - (thumbHeight / 2)) / (float) availableHeight;
            this.currentScroll = Math.max(0.0F, Math.min(1.0F, mousePosInTrack)); // Ограничиваем от 0.0 до 1.0
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            int localX = mouseX - this.guiLeft;
            int localY = mouseY - this.guiTop;

            if (this.currentState == GuiState.GIVE) {
                if (localX >= THUMB_X_POS && localX < THUMB_X_POS + THUMB_WIDTH && localY >= THUMB_Y_MIN && localY < THUMB_Y_MAX) {
                    this.isScrolling = true;
                }
            }

            // Логика клика по кнопке "Изучить" внутри интерфейса исследования
            if (this.researchContainer.getCurrentState() == GuiState.RESEARCH) {
                if (localX >= BTN_X && localX < BTN_X + BTN_WIDTH && localY >= BTN_Y && localY < BTN_Y + BTN_HEIGHT) {
                    if (!this.researchContainer.isResearchSlotEmpty()) {
                        this.playButtonPressSound();
                        CommonProxy.NETWORK.sendToServer(new MessageRequestResearch());
                    }
                    return;
                }
            }

            // Параметры боковых кнопок-вкладок для проверки кликов
            int btnX = 179;
            int giveBtnY = 18;
            int researchBtnY = 56;
            int btnSize = 32;

            // Клик по верхней вкладке (RESEARCH)
            if (localX >= btnX && localX < btnX + btnSize && localY >= giveBtnY && localY < giveBtnY + btnSize) {
                if (this.currentState != GuiState.GIVE) {
                    this.playButtonPressSound();
                    this.currentState = GuiState.GIVE;
                    this.researchContainer.switchState(GuiState.GIVE);

                    // @papam: Сейчас состояние меняется только на клиенте!
                    // @TODO: Создай и отправь пакет (например, MessageChangeGuiState), чтобы серверный контейнер тоже узнал о переключении!
                }
                return;
            }

            // Клик по нижней вкладке (GIVE)
            else if (localX >= btnX && localX < btnX + btnSize && localY >= researchBtnY && localY < researchBtnY + btnSize) {
                if (this.currentState != GuiState.RESEARCH) {
                    this.playButtonPressSound();
                    this.currentState = GuiState.RESEARCH;
                    this.researchContainer.switchState(GuiState.RESEARCH);

                    // @papam: Сервер должен знать актуальное состояние, иначе игрок сможет вынимать предметы через "невидимые" слоты читами.
                }
                return;
            }
        }

        // Если клик не попал по кастомным кнопкам, отдаем обработку ванильному GuiContainer (для кликов по слотам)
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        // Работает только в меню выдачи предмета
        if (this.currentState == GuiState.GIVE) {
            int dWheel = Mouse.getEventDWheel();
            if (dWheel != 0) {
                int columns = 9;
                int rows = 6;
                int totalRows = (int) Math.ceil((double) this.filteredItems.size() / columns);
                int hiddenRows = totalRows - rows;

                if (hiddenRows > 0) {
                    // Изменяем скролл в зависимости от направления колесика
                    float scrollStep = 1.0F / (float) hiddenRows;
                    if (dWheel > 0) {
                        this.currentScroll -= scrollStep; // Прокрутка вверх
                    } else {
                        this.currentScroll += scrollStep; // Прокрутка вниз
                    }
                    this.currentScroll = Math.max(0.0F, Math.min(1.0F, this.currentScroll));
                }
            }
        }
    }

    private void playButtonPressSound() {
        this.mc.getSoundHandler().playSound(
                net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(
                        net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F
                )
        );
    }

    private void drawStateButtons(int guiLeft, int guiTop, int mouseX, int mouseY) {
        int localX = mouseX - guiLeft;
        int localY = mouseY - guiTop;

        /// === ОТРИСОВКА ДЕКОРАТИВНОЙ ОБВОДКИ ===

        int decorX = 176;
        int decorY = 10;
        int decorWidth = 56;
        int decorHeight = 82;

        this.drawTexturedModalRect(guiLeft + decorX, guiTop + decorY, decorX, decorY, decorWidth, decorHeight);


        /// === ИНТЕРАКТИВНЫЕ КНОПКИ (ПОВЕРХ ОБВОДКИ) ===

        int btnX = 179;
        int researchBtnY = 18;
        int giveBtnY = 56;
        int btnSize = 32;

        // Проверка наведения мыши для кнопки Research
        boolean isHoveredResearch = localX >= btnX && localX < btnX + btnSize
                && localY >= researchBtnY && localY < researchBtnY + btnSize;

        int researchU = 179;
        int researchV = 18;

        if (this.currentState != GuiState.RESEARCH) {
            if (isHoveredResearch) {
            }
        }

        // Рисуем кнопку Research поверх декорации
        this.drawTexturedModalRect(guiLeft + btnX, guiTop + researchBtnY, researchU, researchV, btnSize, btnSize);


        // Проверка наведения мыши для кнопки Give
        boolean isHoveredGive = localX >= btnX && localX < btnX + btnSize
                && localY >= giveBtnY && localY < giveBtnY + btnSize;

        int giveU = 179;
        int giveV = 56;

        if (this.currentState != GuiState.GIVE) {
            if (isHoveredGive) {
            }
        }

        // Рисуем кнопку Give поверх декорации
        this.drawTexturedModalRect(guiLeft + btnX, guiTop + giveBtnY, giveU, giveV, btnSize, btnSize);
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
