package wizicl.mre.command;

import wizicl.mre.capabilities.ResearchKey;
import wizicl.mre.config.ConfigNBT;
import wizicl.mre.util.JourneyUtils;
import wizicl.mre.capabilities.IResearch;
import wizicl.mre.capabilities.ResearchProvider;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class CommandJourney extends CommandBase {

    // Легкие статические списки (не выделяют память при каждом вызове)
    private static final List<String> COMMANDS = List.of("consume", "research", "give", "progress", "clear", "remove", "ignore");

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return true; }

    @Override
    public String getName() { return "jm"; }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/jm " + String.join("|", COMMANDS);
    }


    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        // Паттерн-матчинг: мгновенный каст в одну строку
        if (!(sender instanceof EntityPlayer player)) return;

        if (args.length == 0) {
            sendError(player, "chat.journeymode.commandError", getUsage(player));
            return;
        }

        var cap = player.getCapability(ResearchProvider.RESEARCH, null);
        var heldItem = player.getHeldItemMainhand();
        String subCommand = args[0].toLowerCase();

        // 1. Проверка прав Администратора (OP)
        if ((subCommand.equals("ignore") || subCommand.equals("research")) && !player.canUseCommand(2, this.getName())) {
            player.sendMessage(new TextComponentTranslation("chat.journeymode.noPermission"));
            return;
        }

        // 2. Маршрутизация через Enhanced Switch (Современная Java)
        switch (subCommand) {
            case "consume", "research" -> {
                if (heldItem.isEmpty()) {
                    sendError(player, "chat.journeymode.emptyHand", getUsage(player));
                    return;
                }
                if (subCommand.equals("consume")) handleConsume(player, heldItem, args);
                else handleResearch(player, heldItem, args);
            }
            case "give", "progress", "remove", "clear" -> {
                if (!subCommand.equals("clear") && cap.getReadOnlyMap().isEmpty()) {
                    player.sendMessage(new TextComponentTranslation("chat.journeymode.noResearch"));
                    return;
                }
                switch (subCommand) {
                    case "give" -> handleGive(player, cap, args);
                    case "progress" -> handleProgress(player, cap, args);
                    case "remove" -> handleRemove(player, heldItem, cap, args);
                    case "clear" -> handleClear(player, cap);
                }
            }
            case "ignore" -> handleIgnore(player, heldItem, args);
            default -> sendError(player, "chat.journeymode.commandError", getUsage(player));
        }
    }

    // === Обработчики ===

    private void handleConsume(EntityPlayer player, ItemStack stack, String[] args) {
        Integer amount = parseArgInt(player, args, 1, stack.getCount());
        if (amount == null) return;

        JourneyUtils.addResearchAndSync(player, stack, amount);
        player.sendMessage(new TextComponentTranslation("chat.journeymode.add", stack.getTextComponent(), amount));
        stack.shrink(amount);
    }

    private void handleResearch(EntityPlayer player, ItemStack stack, String[] args) {
        Integer amount = parseArgInt(player, args, 1, stack.getCount());
        if (amount == null) return;

        JourneyUtils.addResearchAndSync(player, stack, amount);
        player.sendMessage(new TextComponentTranslation("chat.journeymode.add", stack.getTextComponent(), amount));
    }

    private void handleGive(EntityPlayer player, IResearch cap, String[] args) {
        if (args.length < 2) {
            sendError(player, "chat.journeymode.commandError", getUsage(player));
            return;
        }

        var resultStack = parseItemStackFromString(args[1]);
        if (resultStack.isEmpty()) {
            sendError(player, "chat.journeymode.commandError", getUsage(player));
            return;
        }

        Integer amount = parseArgInt(player, args, 2, 1);
        if (amount == null) return;
        resultStack.setCount(amount);

        if (args.length >= 4) {
            var nbt = parseNbtFromArgs(args, 3);
            if (nbt != null) resultStack.setTagCompound(nbt);
        }

        if (cap.isResearched(resultStack)) {
            player.inventory.addItemStackToInventory(resultStack);
            resultStack.setCount(amount == null ? 1 : amount);
            player.sendMessage(new TextComponentTranslation("chat.journeymode.giveSuccess", resultStack.getTextComponent(), amount));
        } else {
            int need = JourneyUtils.getRequiredAmount(resultStack);
            int has = cap.getResearch(resultStack);
            player.sendMessage(new TextComponentTranslation("chat.journeymode.needMoreResearch", need - has, resultStack.getDisplayName()));
        }
    }

    private void handleProgress(EntityPlayer player, IResearch cap, String[] args) {
        var totalList = new ArrayList<>(cap.getReadOnlyMap().entrySet());
        final int ITEMS_PER_PAGE = 6;
        int maxPages = (int) Math.ceil((double) totalList.size() / ITEMS_PER_PAGE);

        Integer currentPage = parseArgInt(player, args, 1, 1);
        if (currentPage == null) {
            player.sendMessage(new TextComponentTranslation("chat.journeymode.wrongPage"));
            return;
        }

        currentPage = Math.max(1, Math.min(currentPage, maxPages));
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalList.size());

        player.sendMessage(new TextComponentTranslation("chat.journeymode.progress.header", currentPage, maxPages));

        for (int i = startIndex; i < endIndex; i++) {
            var entry = totalList.get(i);
            var key = entry.getKey();
            int currentProgress = entry.getValue();

            var displayStack = createDisplayStack(key);
            int requiredAmount = JourneyUtils.getRequiredAmount(displayStack);

            var color = (currentProgress >= requiredAmount) ? TextFormatting.GREEN : TextFormatting.GRAY;
            ITextComponent nameComp = new TextComponentString(displayStack.getDisplayName());
            nameComp.getStyle().setColor(color);

            boolean hasNbt = key.cleanedNbt() != null;
            ITextComponent nbtComp = new TextComponentString(hasNbt ? " [+NBT]" : "");
            if (hasNbt) {
                nbtComp.getStyle().setColor(TextFormatting.LIGHT_PURPLE);
                nbtComp.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(TextFormatting.ITALIC + key.cleanedNbt().toString())));
            }

            player.sendMessage(new TextComponentTranslation("chat.journeymode.progress.item", nameComp, nbtComp, currentProgress, requiredAmount));
        }

        if (currentPage < maxPages) {
            player.sendMessage(new TextComponentTranslation("chat.journeymode.progress.nextPage", currentPage + 1));
        }
    }

    private void handleClear(EntityPlayer player, IResearch cap) {
        cap.clear();
        player.sendMessage(new TextComponentTranslation("chat.journeymode.clear"));
    }

    private void handleRemove(EntityPlayer player, ItemStack handStack, IResearch cap, String[] args) {
        var targetStack = handStack;

        if (args.length >= 2) {
            var item = Item.getByNameOrId(args[1]);
            if (item != null) {
                targetStack = new ItemStack(item);
            } else {
                sendError(player, "chat.journeymode.commandError", getUsage(player));
                return;
            }
        }

        if (targetStack.isEmpty()) {
            sendError(player, "chat.journeymode.commandError", getUsage(player));
            return;
        }

        if (cap.getResearch(targetStack) > 0) {
            cap.remove(targetStack);
            player.sendMessage(new TextComponentTranslation("chat.journeymode.remove", targetStack.getDisplayName()));
        }
    }

    private void handleIgnore(EntityPlayer player, ItemStack stack, String[] args) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            sendError(player, "chat.journeymode.commandError", getUsage(player));
            return;
        }

        if (args.length == 1) {
            player.sendMessage(new TextComponentTranslation("chat.journeymode.ignoreHat"));
            for (String key : stack.getTagCompound().getKeySet()) {
                var color = ConfigNBT.isTagIgnored(key) ? TextFormatting.GREEN : TextFormatting.RED;
                var keyComponent = new TextComponentString(key);
                keyComponent.getStyle().setColor(color);
                player.sendMessage(new TextComponentTranslation("chat.journeymode.tagPrefix").appendSibling(keyComponent));
            }
            player.sendMessage(new TextComponentTranslation("chat.journeymode.ignore"));
            return;
        }

        if (args.length >= 3) {
            String action = args[1].toLowerCase();
            String targetTag = args[2];

            if (action.equals("add")) {
                if (ConfigNBT.addTag(targetTag)) {
                    player.sendMessage(new TextComponentTranslation("chat.journeymode.addIgnore"));
                } else {
                    var errorText = new TextComponentString("Тег '" + targetTag + "' уже находится в черном списке!");
                    errorText.getStyle().setColor(TextFormatting.YELLOW);
                    player.sendMessage(errorText);
                }
            } else if (action.equals("remove")) {
                if (ConfigNBT.removeTag(targetTag)) {
                    player.sendMessage(new TextComponentTranslation("chat.journeymode.removeIgnore"));
                } else {
                    var errorText = new TextComponentString("Тега '" + targetTag + "' нет в черном списке!");
                    errorText.getStyle().setColor(TextFormatting.RED);
                    player.sendMessage(errorText);
                }
            } else {
                sendError(player, "chat.journeymode.commandError", getUsage(player));
            }
        } else {
            sendError(player, "chat.journeymode.commandError", getUsage(player));
        }
    }

    /// === Автодополнение (Прямое и быстрое) ===

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (!(sender instanceof EntityPlayer player)) return Collections.emptyList();
        var cap = player.getCapability(ResearchProvider.RESEARCH, null);

        if (args.length == 1) return getListOfStringsMatchingLastWord(args, COMMANDS);

        String cmd = args[0].toLowerCase();

        return switch (args.length) {
            case 2 -> switch (cmd) {
                case "ignore" -> getListOfStringsMatchingLastWord(args, "add", "remove");
                case "remove" -> getListOfStringsMatchingLastWord(args, cap.getReadOnlyMap().keySet().stream().map(k -> k.registryName().toString()).toList());
                case "give" -> {
                    var suggestions = cap.getReadOnlyMap().entrySet().stream()
                            .filter(e -> e.getValue() >= JourneyUtils.getRequiredAmount(createDisplayStack(e.getKey())))
                            .map(e -> e.getKey().registryName().toString() + "@" + e.getKey().meta())
                            .toList();
                    yield getListOfStringsMatchingLastWord(args, suggestions);
                }
                case "research", "consume" -> getListOfStringsMatchingLastWord(args, String.valueOf(player.getHeldItemMainhand().getCount()));
                default -> Collections.emptyList();
            };
            case 3 -> switch (cmd) {
                case "ignore" -> args[1].equalsIgnoreCase("add")
                        ? (player.getHeldItemMainhand().hasTagCompound() ? getListOfStringsMatchingLastWord(args, player.getHeldItemMainhand().getTagCompound().getKeySet()) : Collections.emptyList())
                        : getListOfStringsMatchingLastWord(args, ConfigNBT.IGNORED_TAGS);
                case "give" -> getListOfStringsMatchingLastWord(args, "1", "64");
                default -> Collections.emptyList();
            };
            case 4 -> {
                if (!cmd.equals("give")) yield Collections.emptyList();
                var targetItem = args[1];
                var nbtSuggestions = cap.getReadOnlyMap().keySet().stream()
                        .filter(k -> k.cleanedNbt() != null)
                        .filter(k -> {
                            String formatWithMeta = k.registryName().toString() + "@" + k.meta();
                            String formatWithoutMeta = k.registryName().toString() + (k.meta() > 0 ? "@" + k.meta() : "");
                            return targetItem.equals(formatWithMeta) || targetItem.equals(formatWithoutMeta);
                        })
                        .map(k -> k.cleanedNbt().toString().replace(" ", ""))
                        .toList();
                yield getListOfStringsMatchingLastWord(args, nbtSuggestions);
            }
            default -> Collections.emptyList();
        };
    }

    /// === Утилиты ===

    @Nullable
    private Integer parseArgInt(EntityPlayer player, String[] args, int index, int defaultValue) {
        if (args.length <= index) return defaultValue;
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            sendError(player, "chat.journeymode.commandError", getUsage(player));
            return null;
        }
    }

    private ItemStack parseItemStackFromString(String input) {
        String[] parts = input.split("@");
        Item item = Item.getByNameOrId(parts[0]);
        if (item == null) return ItemStack.EMPTY;

        int meta = 0;
        if (parts.length > 1) {
            try { meta = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
        }
        return new ItemStack(item, 1, meta);
    }

    @Nullable
    private NBTTagCompound parseNbtFromArgs(String[] args, int startIndex) {
        if (args.length <= startIndex) return null;
        try {
            return JsonToNBT.getTagFromJson(CommandBase.buildString(args, startIndex));
        } catch (NBTException e) {
            return null;
        }
    }

    private ItemStack createDisplayStack(ResearchKey key) {
        var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(key.registryName()), 1, key.meta());
        if (key.cleanedNbt() != null) stack.setTagCompound(key.cleanedNbt());
        return stack;
    }

    private void sendError(EntityPlayer player, String key, String usage) {
        var text = new TextComponentTranslation(key, usage);
        text.getStyle().setColor(TextFormatting.RED);
        player.sendMessage(text);
    }
}