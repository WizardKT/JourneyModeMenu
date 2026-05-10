package me.wizicl.journeymode.init;

import me.wizicl.journeymode.JourneyUtils;
import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommandJourney extends CommandBase {

    /// Универсальное сообщение ошибки
    private void sendError(ICommandSender sender, String key, Object args) {
        sender.sendMessage(new TextComponentTranslation("chat.journeymode.commandError" + key, args));
    }

    /// Универсальное сообщение об отсутствии изучений
    private void sendNoResearch(ICommandSender sender, String key, Object args) {
        sender.sendMessage(new TextComponentTranslation("chat.journeymode.noResearch" + key, args));
    }


    // Название команды
    @Override
    public String getName() {
        return "jm";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/jm consume|research|give|progress|clear|remove";
    }


    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender instanceof EntityPlayer) {

        /// Все используемые в коде переменные
        EntityPlayer player = (EntityPlayer) sender; // Получаем переменную игрока
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null); // Получаем переменную капы
        ItemStack stack = player.getHeldItemMainhand(); // Получаем переменную предмета в руке
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null); // Получаем переменную капы
        String cmd = args[0].toLowerCase();


        /// Вводим список команд, которым нужен предмет в руке
        List<String> requireHand = Arrays.asList("consume", "research");

        // Проверка на наличие предмета в руке
        if (requireHand.contains(cmd) && stack.isEmpty()) {
            sendError(sender, "commandError", getUsage(sender));
        }


        /// Вводим список команд, которым нужны исследования
        List<String> requireResearch = Arrays.asList("give", "progress", "remove");

        // Проверка на наличие исследований у игрока
        if (requireResearch.contains(cmd) && stack.isEmpty()) {
            sendNoResearch(sender, "commandError", getUsage(sender));
        }


        /// Проверка на то что команду отправил именно игрок
        if (!(sender instanceof EntityPlayer)) return;

        /// Проверяем на то что игрок ввёл хоть что-то
        if (args.length == 0) {
            player.sendMessage(new TextComponentTranslation("chat.journeymode.commandError", getUsage(sender)));
            return;
        }

        /// Объединяем все команды в 1 поле
        String subCommand = args[0];
        switch (subCommand) {
            case "consume": handleConsume(player, cap, stack, args); break;
            case "research": handleResearch(player, stack, cap, args); break;
            case "give": handleGive(player, cap, args); break;
            case "progress": handleProgress(player, cap); break;
            case "clear": handleClear(player, cap); break;
            case "remove": handleRemove(player, cap); break;
        }
    }

            // Команда изучения предмета
            if (args[0].equals("consume")) {
                ItemStack stack = player.getHeldItemMainhand();
                if (stack.isEmpty()) {
                    player.sendMessage(new TextComponentTranslation("chat.journeymode.commandError", getUsage(sender)));
                    return;
                }

                String hand = player.getHeldItemMainhand().getItem().getRegistryName().toString();
                int amount = stack.getCount();

                if (args.length < 2) {
                } else {
                    try {
                        amount = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(new TextComponentTranslation("chat.journeymode.commandError", getUsage(sender)));
                        return;
                    }
                }

                if (amount > stack.getCount()) amount = stack.getCount();

                cap.addResearch(hand, amount);
                stack.shrink(amount);

                player.sendMessage(new TextComponentTranslation("chat.journeymode.add", hand, amount));
            }


            // Админская команда изучения предмета
            if (args[0].equalsIgnoreCase("research")) {
                ItemStack stack = player.getHeldItemMainhand();
                if (stack.isEmpty()) {
                    player.sendMessage(new TextComponentTranslation("chat.journeymode.commandError", getUsage(sender)));
                    return;
                }

                String hand = player.getHeldItemMainhand().getItem().getRegistryName().toString();
                int amount = stack.getCount();

                if (args.length < 2) {
                } else {
                    try {
                        amount = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(new TextComponentTranslation("chat.journeymode.commandError", getUsage(sender)));
                        return;
                    }
                }

                if (amount > stack.getCount()) amount = stack.getCount();

                cap.addResearch(hand, amount);

                player.sendMessage(new TextComponentTranslation("chat.journeymode.add", hand, amount));
            }


            if (args[0].equalsIgnoreCase("give")) {
                if (cap.getResearchMap().isEmpty()) {
                    player.sendMessage(new TextComponentTranslation("chat.journeymode.noResearch"));
                } else {
                    Item item = Item.getByNameOrId(args[1]);
                    if (item == null) {
                        player.sendMessage(new TextComponentTranslation("chat.journeymode.commandError", getUsage(sender)));
                    }

                    int has = cap.getResearchMap().getOrDefault(args[1], 0);
                    int need = JourneyUtils.getRequiredAmount(new ItemStack(item));
                    if (has >= need) {
                        player.inventory.addItemStackToInventory(new ItemStack(item, item.getItemStackLimit()));
                    } else {
                        player.sendMessage(new TextComponentTranslation("chat.journeymode.needMoreResearch", need - has, item));
                    }
                }
            }


            // Команда progress
            if (args[0].equalsIgnoreCase("progress")) {
                player.sendMessage(new TextComponentTranslation("chat.journeymode.progress"));

                if (cap.getResearchMap().isEmpty()) {
                    player.sendMessage(new TextComponentTranslation("chat.journeymode.noResearch"));
                } else {
                    for (Map.Entry<String, Integer> entry : cap.getResearchMap().entrySet()) {
                        Item item = Item.getByNameOrId(entry.getKey());
                        if (item == null) {
                            continue;
                        }
                        int need = JourneyUtils.getRequiredAmount(new ItemStack(item));
                        player.sendMessage(new TextComponentString(entry.getKey() + ":" + entry.getValue() + "/" + need));
                    }
                }
            }


            // Команда clear
            if (args[0].equalsIgnoreCase("clear")) {
                cap.getResearchMap().clear();
                player.sendMessage(new TextComponentTranslation("chat.journeymode.clear"));
            }

            // Команда remove для убирания определённого предмета
            if (args[0].equalsIgnoreCase("remove")) {
                if (cap.getResearchMap().isEmpty()) {
                    player.sendMessage(new TextComponentTranslation("chat.journeymode.noResearch"));
                }
                else {
                    String target =args[1];
                    if (cap.getResearchMap().containsKey(target)) {
                        cap.getResearchMap().remove(target);
                        player.sendMessage(new TextComponentTranslation("chat.journeymode.remove", target));
                    }
                }
            }
        }
    }


    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (sender instanceof EntityPlayer) {}

        EntityPlayer player = (EntityPlayer) sender;
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);

        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "consume", "research", "give", "progress", "clear", "remove");
        }

        if (args.length == 2 && "give".equals(args[0]) || "remove".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, cap.getResearchMap().keySet().toArray(new String[cap.getResearchMap().keySet().size()]));
        }

        if (args.length == 2 && "research".equals(args[0]) || "consume".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, String.valueOf(player.getHeldItemMainhand().getCount()));
        }

        if (args.length == 3 && "give".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, String.valueOf(player.getHeldItemMainhand().getCount()));
        }

        return Collections.emptyList();
    }
}