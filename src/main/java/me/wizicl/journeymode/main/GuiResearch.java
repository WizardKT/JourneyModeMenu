package me.wizicl.journeymode.main;

import net.minecraft.client.gui.GuiScreen;

public class GuiResearch extends GuiScreen {

    /// --- Механика смены меню ---

    private GuiState currentState = GuiState.Research; // Состояние меню при его открытии

    public enum GuiState {
        Research,     // Меню исследования
        Progress,     // Меню прогресса исследования
        ItemList      // Меню предметов + их получения
    }

    public void setState(GuiState newState) { // Смена состояния меню
        this.currentState = newState;
        initGui();
    }

    /// --- Отрисовка меню ---

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        switch (currentState) {
            case Research:
                break;
            case Progress:
                break;
            case ItemList:
                break;
        }
    }
}
