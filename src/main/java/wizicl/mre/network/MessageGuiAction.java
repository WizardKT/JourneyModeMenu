package wizicl.mre.network;

import io.netty.buffer.ByteBuf;
import wizicl.mre.capabilities.IResearch;
import wizicl.mre.capabilities.ResearchKey;
import wizicl.mre.capabilities.ResearchProvider;
import wizicl.mre.client.gui.controller.GuiResearchContainer;
import wizicl.mre.client.gui.model.GuiState;
import wizicl.mre.proxy.CommonProxy;
import wizicl.mre.util.JourneyUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageGuiAction implements IMessage {

    public enum ActionType {
        SWITCH_TAB,
        DO_RESEARCH,
        GIVE_ITEM,
        DEL_ITEM
    }

    private ActionType actionType;
    private int tabId;
    private int clickMode;

    // Пуленепробиваемые поля
    private String registryName;
    private int meta;
    private NBTTagCompound cleanedNbt;

    public MessageGuiAction() {}

    public MessageGuiAction(int tabId) {
        this.actionType = ActionType.SWITCH_TAB;
        this.tabId = tabId;
    }

    public MessageGuiAction(ActionType actionType) {
        this.actionType = actionType;
    }

    public MessageGuiAction(ResearchKey key, int clickMode) {
        this.actionType = ActionType.GIVE_ITEM;
        this.registryName = key.getRegistryName().toString();
        this.meta = key.getMeta();
        this.cleanedNbt = key.getCleanedNbt();
        this.clickMode = clickMode;
    }

    public MessageGuiAction(ResearchKey key) {
        this.actionType = ActionType.DEL_ITEM;
        this.registryName = key.getRegistryName().toString();
        this.meta = key.getMeta();
        this.cleanedNbt = key.getCleanedNbt();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.actionType = ActionType.values()[buffer.readInt()];

        switch (this.actionType) {
            case SWITCH_TAB:
                this.tabId = buffer.readInt();
                break;

            case DO_RESEARCH:
                break;

            case GIVE_ITEM:
            case DEL_ITEM:
                this.registryName = ByteBufUtils.readUTF8String(buffer);
                this.meta = buffer.readInt();
                if (buffer.readBoolean()) {
                    this.cleanedNbt = ByteBufUtils.readTag(buffer);
                } else {
                    this.cleanedNbt = null;
                }
                if (this.actionType == ActionType.GIVE_ITEM) {
                    this.clickMode = buffer.readInt();
                }
                break;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeInt(this.actionType.ordinal());

        switch (this.actionType) {
            case SWITCH_TAB:
                buffer.writeInt(this.tabId);
                break;

            case DO_RESEARCH:
                break;

            case GIVE_ITEM:
            case DEL_ITEM:
                ByteBufUtils.writeUTF8String(buffer, this.registryName);
                buffer.writeInt(this.meta);

                boolean hasNbt = this.cleanedNbt != null;
                buffer.writeBoolean(hasNbt);
                if (hasNbt) {
                    ByteBufUtils.writeTag(buffer, this.cleanedNbt);
                }

                if (this.actionType == ActionType.GIVE_ITEM) {
                    buffer.writeInt(this.clickMode);
                }
                break;
        }
    }

    public static class Handler implements IMessageHandler<MessageGuiAction, IMessage> {
        @Override
        public IMessage onMessage(MessageGuiAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof GuiResearchContainer)) return;
                GuiResearchContainer container = (GuiResearchContainer) player.openContainer;

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
                        if (container.getCurrentState() != GuiState.GIVE) return;

                        IResearch giveCap = player.getCapability(ResearchProvider.RESEARCH, null);
                        if (giveCap == null) return;

                        ResourceLocation regName = new ResourceLocation(message.registryName);
                        ResearchKey key = new ResearchKey(regName, message.meta, message.cleanedNbt);

                        ItemStack stackToGive = key.createItemStack();
                        if (stackToGive.isEmpty()) return;

                        int required = JourneyUtils.getRequiredAmount(stackToGive);
                        int currentProgress = giveCap.getResearch(key); // Используем безопасный метод

                        if (currentProgress < required) {
                            break;
                        }

                        if (message.clickMode == 0) {
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
                        else if (message.clickMode == 1) {
                            stackToGive.setCount(stackToGive.getMaxStackSize());
                            if (!player.inventory.addItemStackToInventory(stackToGive)) {
                                player.dropItem(stackToGive, false);
                            }
                        }
                        else if (message.clickMode == 2) {
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

                        player.sendContainerToPlayer(container);
                        break;

                    case DEL_ITEM:
                        if (container.getCurrentState() != GuiState.GIVE) return;

                        IResearch delCap = player.getCapability(ResearchProvider.RESEARCH, null);
                        if (delCap != null) {
                            ResourceLocation delRegName = new ResourceLocation(message.registryName);
                            ResearchKey keyToDel = new ResearchKey(delRegName, message.meta, message.cleanedNbt);

                            // Создаем стак только для того, чтобы передать его в старый метод remove
                            ItemStack stackToDel = keyToDel.createItemStack();
                            if (stackToDel.isEmpty()) return;

                            if (delCap.remove(stackToDel)) {
                                CommonProxy.NETWORK.sendTo(new MessageSyncSingleResearch(keyToDel, 0), player);
                                container.detectAndSendChanges();
                            }
                        }
                        break;
                }
            });
            return null;
        }
    }
}
