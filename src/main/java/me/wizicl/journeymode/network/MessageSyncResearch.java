package me.wizicl.journeymode.network;

import io.netty.buffer.ByteBuf;
import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.HashMap;
import java.util.Map;

public class MessageSyncResearch implements IMessage {
    private Map<String, Integer> data;

    public MessageSyncResearch() {}

    public MessageSyncResearch(Map<String, Integer> data) {
        this.data = data;
    }


    /// Запись данных в байты
    @Override
    public void toBytes(ByteBuf buf) {
        // Пишем размер нашей карты
        buf.writeInt(data.size());

        // Проходимся по всей карте
        for (Map.Entry<String, Integer> entry : data.entrySet()) {

            // Пишем ID предмета
            ByteBufUtils.writeUTF8String(buf, entry.getKey());

            // Пишем количество предмета
            buf.writeInt(entry.getValue());
        }
    }


    ///Вытаскивание данных из байтов
    @Override
    public void fromBytes(ByteBuf buf) {

        // Вводим переменные карты и количества записей в карте
        data = new HashMap<>();
        int size = buf.readInt();

        // Проходимся по всей карте
        for  (int i = 0; i < size; i++) {

            // Читаем байты
            String key = ByteBufUtils.readUTF8String(buf);

            // Вытаскиваем данные
            int value = buf.readInt();
            data.put(key, value);
        }
    }


    /// Обработчик пакетов данных
    public static class Handler implements IMessageHandler<MessageSyncResearch, IMessage> {
        @Override
        public IMessage onMessage(MessageSyncResearch message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player != null) {
                    IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                    if (cap != null) {
                        cap.getResearchMap().clear();
                        cap.getResearchMap().putAll(message.data);

                        // Дебаг логики
                        System.out.println("CLIENT-SIDE: Получены данные исследований! Размер: " + message.data.size());
                    }
                }
            });
            return null;
        }
    }
}
