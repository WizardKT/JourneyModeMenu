package me.wizicl.journeymode.network;

import io.netty.buffer.ByteBuf;
import me.wizicl.journeymode.client.gui.GuiResearchContainer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageRequestResearch implements IMessage {

    public MessageRequestResearch() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<MessageRequestResearch, IMessage> {

        @Override
        public IMessage onMessage(MessageRequestResearch message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            IThreadListener mainThread = (WorldServer) player.world;
            mainThread.addScheduledTask(() -> {

                if (player.openContainer instanceof GuiResearchContainer) {
                    GuiResearchContainer container = (GuiResearchContainer) player.openContainer;

                    if (!container.isResearchSlotEmpty()) {
                        ItemStack stackInSlot = container.getResearchTargetStack();

                        System.out.println("SERVER-SIDE: Deleting item " + stackInSlot.getDisplayName() + " x" + stackInSlot.getCount());

                        container.getSlot(36).putStack(ItemStack.EMPTY);
                        container.detectAndSendChanges();
                    }
                }
            });

            return null;
        }
    }
}
