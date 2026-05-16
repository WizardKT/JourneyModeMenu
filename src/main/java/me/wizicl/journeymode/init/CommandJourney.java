package me.wizicl.journeymode.init;

import me.wizicl.journeymode.main.JourneyUtils;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
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


    // Подсказка к использованию команды
    @Override
    public String getUsage(ICommandSender sender) {
        return "/jm consume|research|give|progress|clear|remove";
    }


    // Команды
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, @Nonnull String[] args) throws CommandException {

        /// Все используемые в коде переменные
        EntityPlayer player = (EntityPlayer) sender; // Получаем переменную игрока
        ItemStack stack = player.getHeldItemMainhand(); // Получаем переменную предмета в руке
        IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null); // Получаем переменную капы
        String cmd = args[0].toLowerCase();


        /// Вводим список команд, которым нужен предмет в руке
        List<String> requireHand = Arrays.asList("consume", "research");

        // Проверка на наличие предмета в руке
        if (requireHand.contains(cmd) && stack.isEmpty()) {
            sendError(sender, "EmptyHand", getUsage(sender));
            return;
        }


        /// Вводим список команд, которым нужны исследования
        List<String> requireResearch = Arrays.asList("give", "progress", "remove");

        // Проверка на наличие исследований у игрока
        if (requireResearch.contains(cmd) && cap.getReadResearchMap().isEmpty()) {
            sendNoResearch(sender, "", "");
            return;
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
            case "remove": handleRemove(player, stack, cap, args); break;
        }
    }

    /// --- Команда на уничтожение предмета в целях науки ---

    private void handleConsume(EntityPlayer player, IResearch cap, ItemStack stack, String[] args) {
        // Вводим переменную количества предметов и сам предмет
        int amount;
        String item = player.getHeldItemMainhand().getItem().getRegistryName().toString();

        // Проверка, если пользователь ничего не ввёл
        if (args.length < 2) {

            // Если в руке пусто
            if (stack.isEmpty()) {
                sendError(player, "commandError", getUsage(player));
                return;
            }

            // Если пользователь ничего не ввёл регистрируем максимально возможное число
            amount = stack.getCount();
        }

        // Если пользователь что-то ввёл, получаем желаемое игроком количество потребляемых предметов, с проверкой на число
        else {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sendError(player, "commandError", getUsage(player));
                return;
            }
        }

        // Проверяем на мухлёж, больше ли запрос чем то что в руке игрока
        if (amount > stack.getCount()) amount = stack.getCount();

        // Добавляем предметы в изучения и удаляем из инвентаря
        cap.addResearch(item, amount);
        stack.shrink(amount);

        // Синхронизируем данные с сервером
        JourneyUtils.addResearchAndSync(player, item, amount);

        // Отправляем игроку сколько он изучил
        player.sendMessage(new TextComponentTranslation("chat.journeymode.add", item, amount));
    }


    /// --- Команда на бесплатное изучение предмета в целях науки ---

    private void handleResearch(EntityPlayer player,ItemStack stack, IResearch cap, String[] args) {

        // Вводим переменную количества предметов и сам предмет
        int amount;
        String item = player.getHeldItemMainhand().getItem().getRegistryName().toString();

        // Проверка, если пользователь ничего не ввёл
        if (args.length < 2) {

            // Если в руке пусто
            if (stack.isEmpty()) {
                sendError(player, "commandError", getUsage(player));
                return;
            }

            // Если пользователь ничего не ввёл регистрируем максимально возможное число
            amount = stack.getCount();
        }

        // Если пользователь что-то ввёл, получаем желаемое игроком количество потребляемых предметов, с проверкой на число
        else {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sendError(player, "commandError", getUsage(player));
                return;
            }
        }

        // Добавляем предметы в капу
        cap.addResearch(item, amount);

        // Синхронизируем данные с сервером
        JourneyUtils.addResearchAndSync(player, item, amount);

        // Отправляем игроку сколько он изучил
        player.sendMessage(new TextComponentTranslation("chat.journeymode.add", item, amount));

    }


    /// --- Команда на бесплатную выдачу изученного предмета ---

    private void handleGive(EntityPlayer player, IResearch cap, String[] args) {

        // Вводим переменную айди предмета
        Item item = Item.getByNameOrId(args[1]);

        // Если предмет отсутствует - ошибка
        if (item == null) {
            sendError(player, "commandError", getUsage(player));
        }

        // Вводим переменную количества предметов, равную размеру стака
        int amount = item.getItemStackLimit();

        // Проверка на то сколько есть предметов, сколько надо и если предмет изучен, игрок может получить предметы
        int has = cap.getReadResearchMap().getOrDefault(args[1], 0);
        int need = JourneyUtils.getRequiredAmount(new ItemStack(item));
        if (has >= need) {

            // Проверка, хочет ли получить игрок определённое количество предметов.
            if (args.length == 3) {

                // Получаем желаемое игроком количество получаемых предметов, с проверкой на число
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sendError(player, "commandError", getUsage(player));
                    return;
                }
            }

            // Выдаём игроку предметы
            player.inventory.addItemStackToInventory(new ItemStack(item, amount));
        } else {
            // Не выдаём если предмет не изучен
            String localizedName = new ItemStack(item).getDisplayName();
            player.sendMessage(new TextComponentTranslation("chat.journeymode.needMoreResearch", need - has, localizedName));
        }
    }


    /// --- Команда на бесплатную выдачу изученного предмета ---

    private void handleProgress(EntityPlayer player, IResearch cap) {

        // Высылаем игроку заголовок команды
        player.sendMessage(new TextComponentTranslation("chat.journeymode.progress"));

        // Вытаскиваем из карты изучений все предметы, которые изучает игрок
        for (Map.Entry<String, Integer> entry : cap.getReadResearchMap().entrySet()) {
            Item item = Item.getByNameOrId(entry.getKey()); // Переменная предмета, означающая его айди

            // Проверка на пустой предмет во избежание ошибок
            if (item == null) continue;

            // Используем метод проверки требуемого количества предметов
            int has = entry.getValue();
            int need = JourneyUtils.getRequiredAmount(new ItemStack(item));
            String localizedName = new ItemStack(item).getDisplayName();

            // Выводим игроку прогресс исследования для каждого предмета отдельными сообщениями
            // благодаря циклу for
            player.sendMessage(new TextComponentString(localizedName + ":" + has + "/" + need));
        }
    }


    /// --- Команда на бесплатную выдачу изученного предмета ---

    private void handleClear(EntityPlayer player, IResearch cap) {

        // Вызываем метод очистки карты исследований
        cap.getReadResearchMap().clear();

        // Выводим игроку сообщение об успешной очистке исследований
        player.sendMessage(new TextComponentTranslation("chat.journeymode.clear"));
    }


    /// --- Команда на бесплатную выдачу изученного предмета ---

    private void handleRemove(EntityPlayer player,ItemStack stack ,IResearch cap, String[] args) {
        // Вводим переменную предмета для удаления и предмета в руке
        String item;

        // Проверка, если пользователь ничего не ввёл
        if (args.length < 2) {

            // Если в руке пусто
            if (stack.isEmpty()) {
                sendError(player, "commandError", getUsage(player));
                return;
            }
            item = stack.getItem().getRegistryName().toString();
        }

        // Если пользовать что-то ввёл, регистрируем
        else {
            item = args[1];
        }

        // Если карта исследований содержит этот предмет - сносим нафиг.
        if (cap.getReadResearchMap().containsKey(item)) {
            cap.getReadResearchMap().remove(item);

            // Выводим сообщение об успешном снесении предмета из исследований, хехе
            player.sendMessage(new TextComponentTranslation("chat.journeymode.remove", item));
        }
    }

    // Авто-дописывание команд и аргументов на tab
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {

        // Проверка на игрока, является ли отправитель игроком
        if (sender instanceof EntityPlayer) {

            // Вводим переменные игрока и NBT игрока
            EntityPlayer player = (EntityPlayer) sender;
            IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);

            // Проверка первого слова
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, "consume", "research", "give", "progress", "clear", "remove");
            }

            // Проверка для слова give и remove на ввод предмета
            if (args.length == 2 && "give".equals(args[0]) || "remove".equals(args[0])) {
                return getListOfStringsMatchingLastWord(args, cap.getReadResearchMap().keySet().toArray(new String[cap.getReadResearchMap().keySet().size()]));
            }

            // Проверка для слова research и consume на ввод числа
            if (args.length == 2 && "research".equals(args[0]) || "consume".equals(args[0])) {
                return getListOfStringsMatchingLastWord(args, String.valueOf(player.getHeldItemMainhand().getCount()));
            }

            // Дополнительно для give ещё 1 проверка, чтобы вводить и число тоже
            if (args.length == 3 && "give".equals(args[0])) {
                return getListOfStringsMatchingLastWord(args, String.valueOf(player.getHeldItemMainhand().getCount()));
            }
        }

        // Вывод пустого листа
        return Collections.emptyList();
    }
}