package me.wizicl.journeymode.network;

import io.netty.buffer.ByteBuf;
import me.wizicl.journeymode.JourneyMode;
import me.wizicl.journeymode.capabilities.ResearchKey;
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

    public MessageSyncResearch() {
    }

    public MessageSyncResearch(Map<ResearchKey, Integer> data) {
        this.data = data;
    }


    /// Запись данных в байты
    @Override
    public void toBytes(ByteBuf buf) {

        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeInt(data.size());

        // Проходимся по всей карте
        for (Map.Entry<ResearchKey, Integer> entry : data.entrySet()) {
            ResearchKey key = entry.getKey();

            // ID предмета
            buffer.writeResourceLocation(key.getRegistryName());

            // Meta предемета
            buffer.writeInt(key.getMeta());

            // NBT предмета
            boolean hasNBT = key.getCleanedNbt() != null;
            buffer.writeBoolean(hasNBT);
            if (hasNBT) {
                buffer.writeCompoundTag(key.getCleanedNbt());
            }

            // Amount предмета
            buffer.writeInt(entry.getValue());
        }
    }


    /// Вытаскивание данных из байтов
    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        data = new HashMap<>();

        int size = buffer.readInt();

        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            int meta = buffer.readInt();

            NBTTagCompound nbt = null;
            if (buffer.readBoolean()) {
                try {
                    nbt = buffer.readCompoundTag();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            int value = buffer.readInt();

            ResearchKey key = new ResearchKey(id, meta, nbt);
            data.put(key, value);
        }
    }


    /// Обработчик пакетов данных
    public static class Handler implements IMessageHandler<MessageSyncResearch, IMessage> {
        @Override
        public IMessage onMessage(MessageSyncResearch message, MessageContext ctx) {
            JourneyMode.proxy.handleSyncResearch(message);
            return null;
        }
    }
}
