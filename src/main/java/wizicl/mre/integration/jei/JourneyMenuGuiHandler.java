package wizicl.mre.integration.jei;

import mezz.jei.api.gui.IAdvancedGuiHandler;
import wizicl.mre.client.gui.view.GuiResearch;
import wizicl.mre.client.gui.controller.GuiResearchContainer;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class JourneyMenuGuiHandler implements IAdvancedGuiHandler<GuiResearch> {

    @Override
    public Class<GuiResearch> getGuiContainerClass() {
        return GuiResearch.class;
    }

    @Override
    @Nullable
    public List<Rectangle> getGuiExtraAreas(GuiResearch guiContainer) {
        // Паттерн-матчинг: проверяем тип слотов инвентаря и сразу создаем переменную 'container'
        if (guiContainer.inventorySlots instanceof GuiResearchContainer container) {

            var x = guiContainer.getGuiLeft();
            var y = guiContainer.getGuiTop();
            int width = guiContainer.getXSize();
            int height = guiContainer.getYSize();

            return List.of(
                    new Rectangle(x, y, width, height),
                    new Rectangle(x + 155, y + 16, 56, 80)
            );
        }

        return null;
    }

    @Override
    @Nullable
    public Object getIngredientUnderMouse(GuiResearch guiContainer, int mouseX, int mouseY) {
        return null;
    }
}