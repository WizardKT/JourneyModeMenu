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
        List<Rectangle> areas = new ArrayList<>();

        if (guiContainer.inventorySlots instanceof GuiResearchContainer) {
            GuiResearchContainer container = (GuiResearchContainer) guiContainer.inventorySlots;

            // Получаем текущие базовые координаты и размеры GuiContainer
            int x = guiContainer.getGuiLeft();
            int y = guiContainer.getGuiTop();
            int width = guiContainer.getXSize();
            int height = guiContainer.getYSize();
            areas.add(new Rectangle(x, y, width, height));
            areas.add(new Rectangle(x + 155, y + 16, 56, 80));
        }
        return areas.isEmpty() ? null : areas;
    }

    @Override
    @Nullable
    public Object getIngredientUnderMouse(GuiResearch guiContainer, int mouseX, int mouseY) {
        return null;
    }
}