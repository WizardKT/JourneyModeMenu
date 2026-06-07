package wizicl.mre.network;

import io.netty.buffer.ByteBuf;
import wizicl.mre.MatterReplicationEngine;
import wizicl.mre.capabilities.ResearchKey;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MessageSyncResearch implements IMessage {
    public Map<ResearchKey, Integer> data;
    private boolean autoResearchState;

    // Пустой конструктор для сетевого движка Forge
    public MessageSyncResearch() {}

    // Основной конструктор для отправки с сервера
    public MessageSyncResearch(Map<ResearchKey, Integer> data, boolean autoResearchState) {
        this.data = data;
        this.autoResearchState = autoResearchState;
    }

    public boolean getAutoResearchState() {
        return this.autoResearchState;
    }

    /// Запись данных в байты (Отправка)
    @Override
    public void toBytes(ByteBuf buf) {
        var buffer = new PacketBuffer(buf);
        buffer.writeBoolean(this.autoResearchState);
        buffer.writeInt(data.size());

        // var превращает обход мапы в сказку
        for (var entry : data.entrySet()) {
            var key = entry.getKey();

            buffer.writeResourceLocation(key.registryName());
            buffer.writeInt(key.meta());

            boolean hasNBT = key.cleanedNbt() != null;
            buffer.writeBoolean(hasNBT);
            if (hasNBT) {
                buffer.writeCompoundTag(key.cleanedNbt());
            }

            buffer.writeInt(entry.getValue());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        var buffer = new PacketBuffer(buf);
        this.autoResearchState = buffer.readBoolean();
        this.data = new HashMap<>();

        int size = buffer.readInt();

        for (int i = 0; i < size; i++) {
            var id = buffer.readResourceLocation();
            int meta = buffer.readInt();

            NBTTagCompound nbt = null;
            if (buffer.readBoolean()) {
                try {
                    nbt = buffer.readCompoundTag();
                } catch (IOException e) {
                    MatterReplicationEngine.logger.error("Ошибка чтения NBT пакета исследований!", e);
                }
            }

            int value = buffer.readInt();

            this.data.put(new ResearchKey(id, meta, nbt), value);
        }
    }
    public static class Handler implements IMessageHandler<MessageSyncResearch, IMessage> {
        @Override
        public IMessage onMessage(MessageSyncResearch message, MessageContext ctx) {
            MatterReplicationEngine.proxy.handleSyncResearch(message);
            return null;
        }
    }
}
