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

public class MessageSyncSingleResearch implements IMessage {
    public ResearchKey key;
    public int progress;

    public MessageSyncSingleResearch() {};

    public MessageSyncSingleResearch(ResearchKey key, int progress) {
        this.key = key;
        this.progress = progress;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);

        // Пишем данные ОДНОГО ключа
        buffer.writeResourceLocation(key.getRegistryName());
        buffer.writeInt(key.getMeta());

        boolean hasNBT = key.getCleanedNbt() != null;
        buffer.writeBoolean(hasNBT);
        if (hasNBT) {
            buffer.writeCompoundTag(key.getCleanedNbt());
        }

        // Пишем новое значение прогресса
        buffer.writeInt(this.progress);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);

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

        this.key = new ResearchKey(id, meta, nbt);
        this.progress = buffer.readInt();
    }

    public static class Handler implements IMessageHandler<MessageSyncSingleResearch, IMessage> {
        @Override
        public IMessage onMessage(MessageSyncSingleResearch message, MessageContext ctx) {
            JourneyMode.proxy.handleSyncSingleResearch(message);
            return null;
        }
    }
}
