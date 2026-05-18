package me.wizicl.journeymode.network;

import io.netty.buffer.ByteBuf;
import me.wizicl.journeymode.JourneyMode;
import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.client.gui.GuiResearchContainer;
import me.wizicl.journeymode.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageRequestResearch implements IMessage {

    public MessageRequestResearch() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<MessageRequestResearch, IMessage> {

        @Override
        public IMessage onMessage(MessageRequestResearch message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(() -> {

                if (player.openContainer instanceof GuiResearchContainer) {
                    GuiResearchContainer container = (GuiResearchContainer) player.openContainer;

                    // Берем предмет напрямую из инвентаря капы через метод контейнера
                    ItemStack stack = container.getResearchTargetStack();

                    // На всякий случай проверяем, что лежит в слоте по индексу 36 через ванильный метод
                    net.minecraft.inventory.Slot slot36 = container.getSlot(36);
                    ItemStack stackInSlot36 = slot36.getStack();

                    IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);

                    if (cap != null && !stack.isEmpty()) {
                        int consumed = cap.addResearch(stack, stack.getCount());

                        if (consumed > 0) {
                            stack.shrink(consumed);
                            container.detectAndSendChanges();
                            CommonProxy.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap()), player);
                        }
                    }
                }
            });

            return null;
        }
    }
}
