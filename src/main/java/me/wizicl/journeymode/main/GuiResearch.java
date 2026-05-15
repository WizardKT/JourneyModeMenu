package me.wizicl.journeymode.main;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import static me.wizicl.journeymode.proxy.ClientProxy.keyBindOpenGui;

public class GuiResearch extends GuiContainer {

    /// --- Отрисовка меню ---

    private static final ResourceLocation BACKGROUND = new ResourceLocation("journeymode", "textures/gui/container/research.png");

    public GuiResearch(InventoryPlayer playerInv) {
        super(new ResearchContainer(playerInv));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BACKGROUND);

        int x = (width - this.xSize) / 2;
        int y = (height - this.ySize) / 2;

        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keyBindOpenGui.isPressed()) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            Minecraft.getMinecraft().displayGuiScreen(new GuiResearch(player.inventory));
        }
    }
}
