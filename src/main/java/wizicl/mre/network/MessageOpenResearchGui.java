package wizicl.mre.network;

import io.netty.buffer.ByteBuf;
import wizicl.mre.MRE;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageOpenResearchGui implements IMessage {
    public MessageOpenResearchGui() {}

    @Override public void fromBytes(ByteBuf buf) {}
    @Override public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<MessageOpenResearchGui, IMessage> {
        @Override
        public IMessage onMessage(MessageOpenResearchGui message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // Перенаправляем в основной поток сервера
            player.getServerWorld().addScheduledTask(() -> {
                // Вызываем openGui НА СЕРВЕРЕ!
                player.openGui(MRE.instance, 0, player.world, (int)player.posX, (int)player.posY, (int)player.posZ);
            });
            return null;
        }
    }
}
