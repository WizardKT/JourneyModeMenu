package wizicl.mre.autoresearch;

import wizicl.mre.Reference;
import wizicl.mre.capabilities.IResearch;
import wizicl.mre.capabilities.ResearchProvider;
import wizicl.mre.config.ConfigMain;
import wizicl.mre.network.MessageToggleAutoResearch;
import wizicl.mre.proxy.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/// Создаем класс для автоматического исследования результатов в верстаке или инвентаре.
// Эта кнопка будет активирована только в этих двух случаях.
// В остальных ситуациях она не будет отображаться.
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Side.CLIENT)
public class ResultAutoResearch {

    /// Этот метод будет вызван при инициализации любого GUI.
    // Мы проверяем если открыт верстак или инвентарь.
    // Если да, мы добавляем нашу кнопку.
    // Это событие вызывается после инициализации всех элементов GUI.
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();

        // Проверяем, если открыт GUI верстака или инвентарь.
        if (gui instanceof GuiCrafting || gui instanceof GuiInventory) {

            // Координаты можно изменить в зависимости от размера GUI и позиции кнопки.
            int guiLeft = (gui.width - 176) / 2;
            int guiTop = (gui.height - 166) / 2;

            // Создаем кнопку с безопасным ID, чтобы избежать ошибки
            // при попытке добавить кнопку с уже существующим ID.
            int safeId = event.getButtonList().size() + 100;

            /// Добавляем кнопку в список кнопок GUI.
            // Кнопка будет иметь текст "Auto Research" и вызывать метод `onActionPerformed` при нажатии.
            // В зависимости от открытого GUI, мы добавляем кнопку в соответствующее место.
            if (gui instanceof GuiCrafting && ConfigMain.autoResearch.enableInWorkbench) {
                // Координаты для верстака 3x3
                event.getButtonList().add(new AutoResearchButton(safeId, guiLeft + 125, guiTop + 68));
            } else if (gui instanceof GuiInventory && ConfigMain.autoResearch.enableInInventory) {
                // Координаты для инвентаря 2x2
                event.getButtonList().add(new AutoResearchButton(safeId, guiLeft + 155, guiTop + 46));
            }
        }
    }

    /// Этот метод обрабатывает события нажатия на кнопку в GUI.
    // Он проверяет, был ли нажат один из вариантов кнопок и обновляет состояние автоматической исследовательской системы.
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onGuiAction(GuiScreenEvent.ActionPerformedEvent.Post event) {

        // Проверяем, был ли нажат один из вариантов кнопок.
        // Если да, обновляем состояние автоматической исследовательской системы.
        if (event.getGui() instanceof GuiCrafting || event.getGui() instanceof GuiInventory) {
            if (event.getButton() instanceof AutoResearchButton) {
                Minecraft mc = Minecraft.getMinecraft();

                // Получаем текущего игрока. Если игрок существует, получаем его компонент исследований.
                // Если компонент исследований существует, переключаем состояние автоматической исследовательской системы.
                EntityPlayer player = mc.player;
                if (player != null) {
                    IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                    if (cap != null) {
                        cap.toggleAutoResearchState();
                    }
                }

                // Отправляем сообщение на сервер для обновления состояния автоматической исследовательской системы.
                CommonProxy.NETWORK.sendToServer(new MessageToggleAutoResearch());
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }
    }

    /// Создаем класс кнопки AutoResearchButton, которая будет отображаться в GUI-интерфейсе.
    // Этот класс наследуется от GuiButton и переопределяет метод drawButton, чтобы отобразить кастомную текстуру.
    @SideOnly(Side.CLIENT)
    public static class AutoResearchButton extends GuiButton {

        // Используем текстуру из пакета ресурсов
        private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/container/statebuttons.png");

        // Конструктор для инициализации кнопки с заданными координатами.
        public AutoResearchButton(int buttonId, int x, int y) {

            // Задаём координаты и размеры кнопки, а также пустую строку для текста.
            super(buttonId, x, y, 12, 12, "");
        }

        /// Метод drawButton отрисовывает кнопку в зависимости от её состояния.
        // Этот метод используется для отрисовки кнопок в GUI, а конкретнее
        // я переопределяю его, чтобы отобразить кастомную текстуру для кнопки.
        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {

                // Проверка на наведение мыши на кнопку.
                this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

                boolean isActive = false;
                EntityPlayer player = mc.player;

                // Проверяем, если игрок в игре и у него есть компонент исследования.
                // А так же состояние автои-сследования.
                if (player != null) {
                    IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                    if (cap != null) {
                        isActive = cap.getAutoResearchState();
                    }
                }

                int u = 0;
                int v = 0;

                /// Кастомная логика для выбора текстуры в зависимости от состояния кнопки
                // Текстура по умолчанию (u=0,v=0)
                // - Текстура при наведении мыши (u=0,v=20)
                // - Текстура при активном состоянии кнопки (u=20,v=0)
                if (isActive) {
                    u = 20;
                }
                if (this.hovered) {
                    v = 20;
                }

                /// Привязываем текстуру кнопки
                mc.getTextureManager().bindTexture(BUTTON_TEXTURE);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                // Включаем поддержку прозрачности
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

                /// Отрисовка текстуры
                // - x, y - координаты верхнего левого угла
                // - u, v - смещение текстуры в пикселях
                // - width, height - размеры текстуры на экране
                // - textureWidth, textureHeight - размеры текстуры в атласе
                Gui.drawModalRectWithCustomSizedTexture(this.x, this.y, u, v, this.width, this.height, 32.0F, 32.0F);

                /// Отрисовка тултипа
                if (this.hovered) {

                    // Создаем список строк для тултипа
                    List<String> tooltip = new ArrayList<>();

                    // Подтягиваем текст из lang файлов
                    String tooltipText = isActive ?
                            net.minecraft.client.resources.I18n.format("journeymode.tooltip.auto_research.on") :
                            net.minecraft.client.resources.I18n.format("journeymode.tooltip.auto_research.off");

                    tooltip.add(tooltipText);

                    // Если мы находимся в контейнере GUI, то отображаем тултип
                    GuiContainer currentScreen = (GuiContainer) mc.currentScreen;
                    if (currentScreen != null) {
                        currentScreen.drawHoveringText(tooltip, mouseX, mouseY);
                    }
                }
            }
        }
    }
}
