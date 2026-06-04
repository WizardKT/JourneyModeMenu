package wizicl.mre.network;

import io.netty.buffer.ByteBuf;
import wizicl.mre.capabilities.IResearch;
import wizicl.mre.capabilities.ResearchProvider;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageToggleAutoResearch implements IMessage {
    public MessageToggleAutoResearch() {}

    @Override
    public void fromBytes(ByteBuf buf) { }

    @Override
    public void toBytes(ByteBuf buf) { }

    public static class Handler implements IMessageHandler<MessageToggleAutoResearch, IMessage> {
        @Override
        public IMessage onMessage(MessageToggleAutoResearch message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(() -> {
                // Достаем капу игрока
                IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                if (cap != null) {
                    // Переключаем статус (предполагаю, что у тебя в капе будут такие методы)
                    cap.toggleAutoResearchState();
                }
            });
            return null;
        }
    }
}
