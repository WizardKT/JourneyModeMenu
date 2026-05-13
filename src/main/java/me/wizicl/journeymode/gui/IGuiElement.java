package me.wizicl.journeymode.gui;

public interface IGuiElement {
    void draw(int mouseX, int mouseY, float partialTicks);
    void mouseClicked(int mouseX, int mouseY, int mouseButton);
    void keyTyped(char typedChar, int keyCode);
    boolean isHovered(int mouseX, int mouseY);
}
