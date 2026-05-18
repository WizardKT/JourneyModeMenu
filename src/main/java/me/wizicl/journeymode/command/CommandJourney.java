package me.wizicl.journeymode.command;

import me.wizicl.journeymode.capabilities.ResearchKey;
import me.wizicl.journeymode.config.ConfigNBT;
import me.wizicl.journeymode.util.JourneyUtils;
import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
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

    @Override
    public String getName() {
        return "jm";
    }


    @Override
    public String getUsage(ICommandSender sender) {
        return "/jm consume|research|give|progress|clear|remove|ignore";
    }


    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) sender;
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
        ItemStack heldItem = player.getHeldItemMainhand();

        if (args.length == 0) {
            sendError(player, "chat.journeymode.commandError", getUsage(player));
            return;
        }

        String subCommand = args[0].toLowerCase();

        // 1. Проверка на обязательный предмет в руке
        List<String> requireHand = Arrays.asList("consume", "research");
        if (requireHand.contains(subCommand) && heldItem.isEmpty()) {
            sendError(player, "chat.journeymode.emptyHand", getUsage(player));
            return;
        }

        // 2. Проверка на наличие хоть какого-то прогресса в вашей базе данных
        List<String> requireResearch = Arrays.asList("give", "progress", "remove");
        if (requireResearch.contains(subCommand) && cap.getReadOnlyMap().isEmpty()) {
            player.sendMessage(new TextComponentTranslation("chat.journeymode.noResearch"));
            return;
        }

        // 3. Проверка прав Администратора (OP уровень 2)
        List<String> requireOp = Arrays.asList("ignore", "research");
        if (requireOp.contains(subCommand) && !player.canUseCommand(2, this.getName())) {
            player.sendMessage(new TextComponentTranslation("chat.journeymode.noPermission"));
            return; // КРИТИЧЕСКИ ВАЖНО: останавливаем выполнение!
        }

        switch (subCommand) {
            case "consume":
                handleConsume(player, heldItem, args);
                break;
            case "research":
                handleResearch(player, heldItem, args);
                break;
            case "give":
                handleGive(player, cap, args);
                break;
            case "progress":
                handleProgress(player, cap, args);
                break;
            case "clear":
                handleClear(player, cap);
                break;
            case "remove":
                handleRemove(player, heldItem, cap, args);
                break;
            case "ignore":
                handleIgnore(player, heldItem, args);
                break;
            default:
                sendError(player, "chat.journeymode.commandError", getUsage(player));
                break;
        }
    }


    /// === Обработчики конкретных команд ===

    private void handleConsume(EntityPlayer player, ItemStack stack, String[] args) {
        Integer amount = parseArgInt(player, args, 1, stack.getCount());
        if (amount == null) return;
        int amountToConsume = Math.max(1, Math.min(amount, stack.getCount()));

        JourneyUtils.addResearchAndSync(player, stack, amountToConsume);
        player.sendMessage(new TextComponentTranslation("chat.journeymode.add", stack.getTextComponent(), amountToConsume));
        stack.shrink(amountToConsume);
    }

    private void handleResearch(EntityPlayer player, ItemStack stack, String[] args) {
        Integer amount = parseArgInt(player, args, 1, stack.getCount());
        if (amount == null) return;

        player.sendMessage(new TextComponentTranslation("chat.journeymode.add", stack.getTextComponent(), amount));
        JourneyUtils.addResearchAndSync(player, stack, amount);
    }


    private void handleGive(EntityPlayer player, IResearch cap, String[] args) {
        if (args.length < 2) {
            sendError(player, "chat.journeymode.commandError", getUsage(player));
            return;
        }

        ItemStack resultStack = parseItemStackFromString(args[1]);
        if (resultStack.isEmpty()) {
            sendError(player, "chat.journeymode.commandError", getUsage(player));
            return;
        }

        Integer amount = parseArgInt(player, args, 2, 1);
        if (amount == null) return;
        resultStack.setCount(amount);

        if (args.length >= 4) {
            NBTTagCompound nbt = parseNbtFromArgs(args, 3);
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
        List<Map.Entry<ResearchKey, Integer>> totalList = new ArrayList<>(cap.getReadOnlyMap().entrySet());

        if (totalList.isEmpty()) {
            player.sendMessage(new TextComponentTranslation("chat.journeymode.noResearch"));
            return;
        }

        final int ITEMS_PER_PAGE = 6;
        int maxPages = (int) Math.ceil((double) totalList.size() / ITEMS_PER_PAGE);

        Integer currentPage = parseArgInt(player, args, 1, 1);
        if (currentPage == null) {
            player.sendMessage(new TextComponentTranslation("chat.journeymode.wrongPage"));
            return;
        }

        currentPage = Math.max(1, Math.min(currentPage, maxPages)); // Защита от выхода за пределы

        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalList.size());

        player.sendMessage(new TextComponentTranslation("chat.journeymode.progress.header", currentPage, maxPages));

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<ResearchKey, Integer> entry = totalList.get(i);
            ResearchKey key = entry.getKey();
            int currentProgress = entry.getValue();

            ItemStack displayStack = createDisplayStack(key);
            int requiredAmount = JourneyUtils.getRequiredAmount(displayStack);

            TextFormatting color = (currentProgress >= requiredAmount) ? TextFormatting.GREEN : TextFormatting.GRAY;

            ITextComponent nameComponent = new TextComponentString(displayStack.getDisplayName());
            nameComponent.getStyle().setColor(color);

            boolean hasNbt = key.getCleanedNbt() != null;
            ITextComponent nbtComponent = new TextComponentString(hasNbt ? " [+NBT]" : "");
            if (hasNbt) {
                nbtComponent.getStyle().setColor(TextFormatting.LIGHT_PURPLE);
                String nbtJson = key.getCleanedNbt().toString();

                nbtComponent.getStyle().setHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(
                                TextFormatting.ITALIC + nbtJson)
                        ));
            }

            player.sendMessage(new TextComponentTranslation(
                    "chat.journeymode.progress.item",
                    nameComponent, nbtComponent, currentProgress, requiredAmount
            ));
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
        ItemStack targetStack = handStack;

        // Если указан аргумент - пытаемся получить предмет по ID
        if (args.length >= 2) {
            Item item = Item.getByNameOrId(args[1]);
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

        // Сценарий: просто /jm ignore (вывод списка тегов предмета)
        if (args.length == 1) {
            player.sendMessage(new TextComponentTranslation("chat.journeymode.ignoreHat"));
            for (String key : stack.getTagCompound().getKeySet()) {
                boolean isIgnored = ConfigNBT.isTagIgnored(key);
                TextFormatting color = isIgnored ? TextFormatting.GREEN : TextFormatting.RED;

                ITextComponent keyComponent = new TextComponentString(key);
                keyComponent.getStyle().setColor(color);

                player.sendMessage(new TextComponentTranslation("chat.journeymode.tagPrefix").appendSibling(keyComponent));
            }
            player.sendMessage(new TextComponentTranslation("chat.journeymode.ignore"));
            return;
        }

        // Сценарий: /jm ignore add/remove <tag>
        String action = args[1].toLowerCase();

        if (args.length >= 3) {
            String targetTag = args[2];

            if (action.equals("add")) {
                boolean success = ConfigNBT.addTag(targetTag);
                if (success) {
                    player.sendMessage(new TextComponentTranslation("chat.journeymode.addIgnore"));
                } else {
                    ITextComponent errorText = new TextComponentString("Тег '" + targetTag + "' уже находится в черном списке!");
                    errorText.getStyle().setColor(TextFormatting.YELLOW);
                    player.sendMessage(errorText);
                }
            }
            else if (action.equals("remove")) {
                boolean success = ConfigNBT.removeTag(targetTag); // Твой новый метод
                if (success) {
                    // Создай этот ключ перевода в .lang файле
                    player.sendMessage(new TextComponentTranslation("chat.journeymode.removeIgnore"));
                } else {
                    ITextComponent errorText = new TextComponentString("Тега '" + targetTag + "' нет в черном списке!");
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


    /// === Утилиты и хелперы ===

    /**
     * Безопасно достает число из аргументов. Если числа нет - возвращает стандартное значение.
     * Если введена абракадабра - отправляет ошибку игроку и возвращает null.
     */

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
        if (item == null) {
            return ItemStack.EMPTY;
        }
        int meta = 0;
        if (parts.length > 1) {
            try {
                meta = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return ItemStack.EMPTY;
            }
        }
        return new ItemStack(item,1, meta);
    }

    @Nullable
    private NBTTagCompound parseNbtFromArgs(String[] args, int startIndex) {
        if (args.length <= startIndex) return null;
        try {
            String nbtString = CommandBase.buildString(args, startIndex);
            return JsonToNBT.getTagFromJson(nbtString);
        } catch (NBTException e) {
            return null;
        }
    }


    /**
     * Превращает ResearchKey обратно в полноценный ItemStack для отображения
     */

    private ItemStack createDisplayStack(ResearchKey key) {
        ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(key.getRegistryName()), 1, key.getMeta());
        if (key.getCleanedNbt() != null) {
            stack.setTagCompound(key.getCleanedNbt());
        }
        return stack;
    }


    /// === Автодополнение ===

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (!(sender instanceof EntityPlayer)) return Collections.emptyList();

        EntityPlayer player = (EntityPlayer) sender;
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);

        // Уровень 1: /jm [подкоманда]
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "consume", "research", "give", "progress", "clear", "remove", "ignore");
        }

        String cmd = args[0].toLowerCase();

        // Уровень 2: /jm ignore [add/remove]
        if (args.length == 2 && cmd.equals("ignore")) {
            return getListOfStringsMatchingLastWord(args, "add", "remove");
        }

        // Уровень 2: /jm remove [item]
        if (args.length == 2 && cmd.equals("remove")) {
            List<String> researchedItems = new ArrayList<>();
            for (ResearchKey key : cap.getReadOnlyMap().keySet()) {
                researchedItems.add(key.getRegistryName().toString());
            }
            return getListOfStringsMatchingLastWord(args, researchedItems);
        }

        // Уровень 2: /jm give [item@meta]
        if (args.length == 2 && cmd.equals("give")) {
            List<String> suggestions = new ArrayList<>();
            for (Map.Entry<ResearchKey, Integer> entry : cap.getReadOnlyMap().entrySet()) {
                ResearchKey key = entry.getKey();
                int currentProgress = entry.getValue();

                ItemStack checkStack = createDisplayStack(key);
                int requiredAmount = JourneyUtils.getRequiredAmount(checkStack);
                if (currentProgress >= requiredAmount) {
                    String registryName = key.getRegistryName().toString();
                    int meta = key.getMeta();

                    String suggestionString = registryName + "@" + meta;
                    suggestions.add(suggestionString);
                }
            }
            return getListOfStringsMatchingLastWord(args, suggestions);
        }

        // Уровень 3: /jm ignore add/remove [tag]
        if (args.length == 3 && cmd.equals("ignore")) {
            String action = args[1].toLowerCase();

            if (action.equals("add")) {
                // Подсказываем теги, которые есть на предмете в руке
                ItemStack heldItem = player.getHeldItemMainhand();
                if (!heldItem.isEmpty() && heldItem.hasTagCompound()) {
                    return getListOfStringsMatchingLastWord(args, heldItem.getTagCompound().getKeySet());
                }
            }
            else if (action.equals("remove")) {
                // Подсказываем теги, которые уже занесены в конфиг
                return getListOfStringsMatchingLastWord(args, ConfigNBT.IGNORED_TAGS);
            }
        }

        // Уровень 3: /jm give pid@meta] [количество]
        if (args.length == 3 && cmd.equals("give")) {
            return getListOfStringsMatchingLastWord(args, "1", "64");
        }

        // Уровень 4: /jm give [id@meta] [количество] [NBT]
        if (args.length == 4 && cmd.equals("give")) {
            List<String> nbtSuggestions = new ArrayList<>();
            String targetItem = args[1]; // Например: "minecraft:spawn_egg@0" или "minecraft:spawn_egg"

            for (ResearchKey key : cap.getReadOnlyMap().keySet()) {
                if (key.getCleanedNbt() == null) continue; // Если у предмета нет NBT, он нам не интересен

                String registryName = key.getRegistryName().toString();
                int meta = key.getMeta();

                String formatWithMeta = registryName + "@" + meta;
                String formatWithoutMeta = registryName + (meta > 0 ? "@" + meta : "");

                if (targetItem.equals(formatWithMeta) || targetItem.equals(formatWithoutMeta)) {
                    String nbtString = key.getCleanedNbt().toString().replace(" ", "");
                    nbtSuggestions.add(nbtString);
                }
            }
            return getListOfStringsMatchingLastWord(args, nbtSuggestions);
        }

        // Уровень 3/4: Подсказка количества предметов из руки
        if ((args.length == 2 && (cmd.equals("research") || cmd.equals("consume")))) {
            return getListOfStringsMatchingLastWord(args, String.valueOf(player.getHeldItemMainhand().getCount()));
        }

        return Collections.emptyList();
    }

    // Заглушка
    private void sendError(EntityPlayer player, String key, String usage) {
        TextComponentTranslation text = new TextComponentTranslation(key, usage);
        text.getStyle().setColor(TextFormatting.RED);
        player.sendMessage(text);
    }
}