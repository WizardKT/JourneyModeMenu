package me.wizicl.journeymode.network;

import io.netty.buffer.ByteBuf;
import me.wizicl.journeymode.JourneyMode;
import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
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
                System.out.println("1. СЕРВЕР: Пакет получен. Контейнер: " + player.openContainer.getClass().getSimpleName());

                if (player.openContainer instanceof GuiResearchContainer) {
                    GuiResearchContainer container = (GuiResearchContainer) player.openContainer;

                    // Берем предмет напрямую из инвентаря капы через метод контейнера
                    ItemStack stack = container.getResearchTargetStack();
                    System.out.println("2. СЕРВЕР: Предмет в кастомном методе = " + (stack.isEmpty() ? "ПУСТО" : stack.getDisplayName() + " x" + stack.getCount()));

                    // На всякий случай проверяем, что лежит в слоте по индексу 36 через ванильный метод
                    net.minecraft.inventory.Slot slot36 = container.getSlot(36);
                    ItemStack stackInSlot36 = slot36.getStack();
                    System.out.println("3. СЕРВЕР: Предмет в Slot(36) = " + (stackInSlot36.isEmpty() ? "ПУСТО" : stackInSlot36.getDisplayName() + " x" + stackInSlot36.getCount()));

                    IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                    System.out.println("4. СЕРВЕР: Капа игрока найдена? = " + (cap != null));

                    if (cap != null && !stack.isEmpty()) {
                        int consumed = cap.addResearch(stack, stack.getCount());
                        System.out.println("5. ВАХТЁР: Сказал поглотить = " + consumed);

                        if (consumed > 0) {
                            stack.shrink(consumed);
                            container.detectAndSendChanges();
                            JourneyMode.NETWORK.sendTo(new MessageSyncResearch(cap.getReadOnlyMap()), player);
                            System.out.println("6. СЕРВЕР: Успешно уменьшено!");
                        }
                    }
                }
            });

            return null;
        }
    }
}
