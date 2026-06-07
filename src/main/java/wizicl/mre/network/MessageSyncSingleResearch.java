package wizicl.mre.network;

import io.netty.buffer.ByteBuf;
import wizicl.mre.MatterReplicationEngine;
import wizicl.mre.capabilities.ResearchKey;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

public class MessageSyncSingleResearch implements IMessage {

    public ResearchKey key;
    public int progress;

    // Пустой конструктор для сетевого движка Forge
    public MessageSyncSingleResearch() {}

    // Конструктор для отправки точечного обновления
    public MessageSyncSingleResearch(ResearchKey key, int progress) {
        this.key = key;
        this.progress = progress;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        var buffer = new PacketBuffer(buf);

        // Пишем данные ОДНОГО ключа через быстрые методы рекорда
        buffer.writeResourceLocation(key.registryName());
        buffer.writeInt(key.meta());

        boolean hasNBT = key.cleanedNbt() != null;
        buffer.writeBoolean(hasNBT);
        if (hasNBT) {
            buffer.writeCompoundTag(key.cleanedNbt());
        }

        // Пишем новое значение прогресса
        buffer.writeInt(this.progress);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        var buffer = new PacketBuffer(buf);

        var id = buffer.readResourceLocation();
        int meta = buffer.readInt();

        NBTTagCompound nbt = null;
        if (buffer.readBoolean()) {
            try {
                nbt = buffer.readCompoundTag();
            } catch (IOException e) {
                MatterReplicationEngine.logger.error("Ошибка точечного чтения NBT в пакете синхронизации!", e);
            }
        }

        this.key = new ResearchKey(id, meta, nbt);
        this.progress = buffer.readInt();
    }

    // Хендлер пакета
    public static class Handler implements IMessageHandler<MessageSyncSingleResearch, IMessage> {
        @Override
        public IMessage onMessage(MessageSyncSingleResearch message, MessageContext ctx) {
            MatterReplicationEngine.proxy.handleSyncSingleResearch(message);
            return null;
        }
    }
}
