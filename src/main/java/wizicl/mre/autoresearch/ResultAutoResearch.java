package wizicl.mre.autoresearch;

import wizicl.mre.MRE;
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

@Mod.EventBusSubscriber(modid = MODID, value = Side.CLIENT)
public class ResultAutoResearch {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();

        // Проверяем, что открыт верстак ИЛИ инвентарь
        if (gui instanceof GuiCrafting || gui instanceof GuiInventory) {

            // Мини-подсказка: И GuiCrafting, и GuiInventory наследуют GuiContainer.
            // Приведя к нему, мы могли бы динамически достать guiLeft и guiTop (через Reflection),
            // но классический подсчет через ширину экрана (176x166) тоже отлично работает.
            int guiLeft = (gui.width - 176) / 2;
            int guiTop = (gui.height - 166) / 2;

            int safeId = event.getButtonList().size() + 100;

            // Добавляем кнопку по разным координатам в зависимости от открытого GUI
            if (gui instanceof GuiCrafting && ConfigMain.autoResearch.enableInWorkbench) {
                // Координаты для верстака 3x3 (немного сдвинул из-за нового размера)
                event.getButtonList().add(new AutoResearchButton(safeId, guiLeft + 125, guiTop + 58));
            } else if (gui instanceof GuiInventory && ConfigMain.autoResearch.enableInInventory) {
                // Координаты для инвентаря 2x2 (вводи любые, потом подгонишь!)
                event.getButtonList().add(new AutoResearchButton(safeId, guiLeft + 155, guiTop + 46));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onGuiAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        // Здесь тоже расширяем проверку
        if (event.getGui() instanceof GuiCrafting || event.getGui() instanceof GuiInventory) {
            if (event.getButton() instanceof AutoResearchButton) {
                Minecraft mc = Minecraft.getMinecraft();

                // Клиентское предсказание (моментально меняем визуал)
                EntityPlayer player = mc.player;
                if (player != null) {
                    IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                    if (cap != null) {
                        cap.toggleAutoResearchState();
                    }
                }

                // Отправляем пакет на сервер
                CommonProxy.NETWORK.sendToServer(new MessageToggleAutoResearch());
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }
    }

    // --- Кастомный класс кнопки (остается без изменений) ---
    @SideOnly(Side.CLIENT)
    public static class AutoResearchButton extends GuiButton {

        // Указываем путь к твоей текстуре
        private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation("journeymode", "textures/gui/container/statebuttons.png");

        public AutoResearchButton(int buttonId, int x, int y) {
            // Передаем размеры 12x12 и пустую строку (текст поверх больше не нужен)
            super(buttonId, x, y, 12, 12, "");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                // Проверяем наведение (стандартная логика ванилы)
                this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

                // Получаем статус капы для выбора правильной текстуры
                boolean isActive = false;
                EntityPlayer player = mc.player;
                if (player != null) {
                    IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                    if (cap != null) {
                        isActive = cap.getAutoResearchState();
                    }
                }

                // Математика координат текстуры (U, V) на основе твоих данных
                int u = 0;
                int v = 0;

                if (isActive) {
                    u = 20; // Правый столбец (Включено)
                }
                if (this.hovered) {
                    v = 20; // Нижняя строка (Наведение)
                }

                // Биндим текстуру и сбрасываем цвет, чтобы кнопка не окрасилась
                mc.getTextureManager().bindTexture(BUTTON_TEXTURE);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                // Включаем поддержку прозрачности (на случай, если у кнопки есть полупрозрачные пиксели)
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

                // Отрисовка самой текстуры

                Gui.drawModalRectWithCustomSizedTexture(this.x, this.y, u, v, this.width, this.height, 32.0F, 32.0F);

                // Отрисовка локализованного тултипа при наведении
                if (this.hovered) {
                    List<String> tooltip = new ArrayList<>();

                    // Подтягиваем текст из lang файлов
                    String tooltipText = isActive ?
                            net.minecraft.client.resources.I18n.format("journeymode.tooltip.auto_research.on") :
                            net.minecraft.client.resources.I18n.format("journeymode.tooltip.auto_research.off");

                    tooltip.add(tooltipText);

                    GuiContainer currentScreen = (GuiContainer) mc.currentScreen;
                    if (currentScreen != null) {
                        currentScreen.drawHoveringText(tooltip, mouseX, mouseY);
                    }
                }
            }
        }
    }
}
