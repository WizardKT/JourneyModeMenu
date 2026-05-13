package me.wizicl.journeymode.gui.base;

import net.minecraft.item.ItemStack;

public class GuiItemSlot implements IGuiElement {
    private final GuiBase parent;
    private int x, y, size;
    private ItemStack stack;

    public GuiItemSlot(GuiBase parent ,int x, int y, int size, ItemStack stack) {
        this.parent = parent;
        this.x = x; this.y = y; this.size = size; this.stack = stack;
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        int renderY = this.y - parent.scrollAmount;

        if (renderY + size >  parent.guiTop + 30 && renderY< parent.guiTop + parent.ySize - 10 ) {
            parent.drawSlot(this.x, renderY, this.size, mouseX, mouseY);
            parent.drawItemStack(this.stack, this.x + 1, renderY + 1);
        }

    }

    public ItemStack getStack() { return stack; }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY)) {

        }
    }

    @Override
    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    @Override public void keyTyped(char typedChar, int keyCode) {}
}
