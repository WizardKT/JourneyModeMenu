package me.wizicl.journeymode.network;

import io.netty.buffer.ByteBuf;
import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchKey;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import me.wizicl.journeymode.client.gui.GuiResearchContainer;
import me.wizicl.journeymode.client.gui.GuiState;
import me.wizicl.journeymode.proxy.CommonProxy;
import me.wizicl.journeymode.util.JourneyUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageGuiAction implements IMessage {

    // Перечисление всех типов кликов/действий в GUI
    public enum ActionType {
        SWITCH_TAB,
        DO_RESEARCH,
        GIVE_ITEM
    }

    private ActionType actionType;

    // Переменные для разных типов действий (заполняются по необходимости)
    private int tabId;
    private ItemStack requestedStack;
    private int clickMode;

    // Обязательный пустой конструктор для Netty
    public MessageGuiAction() {}

    // Конструктор для SWITCH_TAB
    public MessageGuiAction(int tabId) {
        this.actionType = ActionType.SWITCH_TAB;
        this.tabId = tabId;
    }

    // Конструктор для DO_RESEARCH
    public MessageGuiAction(ActionType actionType) {
        this.actionType = actionType; // Для действий без параметров
    }

    // Конструктор для GIVE_ITEM
    public MessageGuiAction(ItemStack requestedStack, int clickMode) {
        this.actionType = ActionType.GIVE_ITEM;
        this.requestedStack = requestedStack.copy();
        this.clickMode = clickMode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.actionType = ActionType.values()[buffer.readInt()];

        // В зависимости от типа действия дочитываем из буфера только то, что нужно!
        switch (this.actionType) {
            case SWITCH_TAB:
                this.tabId = buffer.readInt();
                break;

            case DO_RESEARCH:
                break;

            case GIVE_ITEM:
                this.requestedStack = ByteBufUtils.readItemStack(buffer);
                this.clickMode = buffer.readInt();
                break;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeInt(this.actionType.ordinal());

        // Записываем данные в буфер строго под конкретное действие
        switch (this.actionType) {
            case SWITCH_TAB:
                buffer.writeInt(this.tabId);
                break;

            case DO_RESEARCH:
                break;

            case GIVE_ITEM:
                ByteBufUtils.writeItemStack(buffer, this.requestedStack);
                buffer.writeInt(this.clickMode);
                break;
        }
    }

    public static class Handler implements IMessageHandler<MessageGuiAction, IMessage> {
        @Override
        public IMessage onMessage(MessageGuiAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(() -> {
                // Общая защита для всех действий внутри открытого GUI
                if (!(player.openContainer instanceof GuiResearchContainer)) return;
                GuiResearchContainer container = (GuiResearchContainer) player.openContainer;

                // Магия распределения логики на сервере
                switch (message.actionType) {

                    case SWITCH_TAB:
                        if (message.tabId >= 0 && message.tabId < GuiState.values().length) {
                            GuiState newState = GuiState.values()[message.tabId];
                            container.switchState(newState);
                        }
                        break;

                    case DO_RESEARCH:
                        ItemStack stack = container.getResearchTargetStack();
                        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);

                        if (cap != null && !stack.isEmpty()) {
                            int consumed = cap.addResearch(stack, stack.getCount());

                            if (consumed > 0) {
                                ResearchKey keyToSend = new ResearchKey(stack);
                                stack.shrink(consumed);
                                int realProgress = cap.getReadOnlyMap().getOrDefault(keyToSend, 0);
                                CommonProxy.NETWORK.sendTo(new MessageSyncSingleResearch(keyToSend, realProgress), player);
                                container.detectAndSendChanges();
                            }
                        }
                        break;

                    case GIVE_ITEM:
                        // Защита: Сервер проверяет, находится ли контейнер во вкладке выдачи
                        if (container.getCurrentState() != GuiState.GIVE) return;

                        IResearch giveCap = player.getCapability(ResearchProvider.RESEARCH, null);
                        if (giveCap == null || message.requestedStack.isEmpty()) return;

                        ResearchKey key = new ResearchKey(message.requestedStack);
                        ItemStack stackToGive = key.createItemStack();
                        if (stackToGive.isEmpty()) return;

                        int required = JourneyUtils.getRequiredAmount(stackToGive);
                        int currentProgress = giveCap.getResearch(stackToGive);

                        if (currentProgress < required) {
                            break;
                        }

                        if (message.clickMode == 0) { // ЛКМ - 1 штуку в курсор
                            ItemStack cursorStack = player.inventory.getItemStack();
                            int maxStackSize = stackToGive.getMaxStackSize();
                            stackToGive.setCount(1);

                            if (cursorStack.isEmpty()) {
                                player.inventory.setItemStack(stackToGive);
                                player.connection.sendPacket(new SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
                            }
                            else if (ItemStack.areItemsEqual(cursorStack, stackToGive) && ItemStack.areItemStackTagsEqual(cursorStack, stackToGive) && cursorStack.getCount() < maxStackSize) {
                                cursorStack.grow(1);
                                player.connection.sendPacket(new SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
                            }
                            else if (ItemStack.areItemsEqual(cursorStack, stackToGive) && ItemStack.areItemStackTagsEqual(cursorStack, stackToGive) && cursorStack.getCount() == maxStackSize ) {
                                player.inventory.addItemStackToInventory(stackToGive);
                            }
                            player.updateHeldItem();
                        }
                        else if (message.clickMode == 1) { // Shift + ЛКМ - Стак в инвентарь
                            stackToGive.setCount(stackToGive.getMaxStackSize());
                            if (!player.inventory.addItemStackToInventory(stackToGive)) {
                                player.dropItem(stackToGive, false);
                            }
                        }
                        else if (message.clickMode == 2) { // Колесико мыши - Стак в курсор (в руку)
                            ItemStack cursorStack = player.inventory.getItemStack();
                            int maxStackSize = stackToGive.getMaxStackSize();
                            if (cursorStack.isEmpty()) {
                                stackToGive.setCount(maxStackSize);
                                player.inventory.setItemStack(stackToGive);
                            }
                            else if (ItemStack.areItemsEqual(cursorStack, stackToGive) && ItemStack.areItemStackTagsEqual(cursorStack, stackToGive)) {
                                int spaceLeft = maxStackSize - cursorStack.getCount();
                                if (spaceLeft > 0) {
                                    cursorStack.grow(spaceLeft);
                                }
                            }
                        }

                        // Синхронизируем изменения инвентаря с клиентом
                        player.sendContainerToPlayer(container);
                        break;
                }
            });
            return null;
        }
    }
}
