package me.wizicl.journeymode.init;

import me.wizicl.journeymode.capabilities.ResearchKey;
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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class CommandJourney extends CommandBase {

    /// Общие переменные
    int amount;

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
        String cmd = args[0].toLowerCase(); // Первая команда jm


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
        if (requireResearch.contains(cmd) && cap.getReadOnlyMap().isEmpty()) {
            sendNoResearch(sender, "", "");
            return;
        }


        /// Вводим список команд, которым нужны админ права
        List<String> requireOp = Arrays.asList("ignore", "research");

        if (requireOp.contains(cmd) && !sender.canUseCommand(2, cmd)) {
            sender.sendMessage(new TextComponentTranslation("chat.journeymode.noPermission"));
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
            case "progress": handleProgress(player, stack, cap); break;
            case "clear": handleClear(player, cap); break;
            case "remove": handleRemove(player, stack, cap, args); break;
            case "ignore": handleIgnore(player, stack, args);  break;
        }
    }

    /// --- Команда на уничтожение предмета в целях науки ---

    private void handleConsume(EntityPlayer player, IResearch cap, ItemStack stack, String[] args) {

        // Проверка, если пользователь ничего не ввёл
        if (args.length < 2) {

            // Если в руке пусто
            if (stack.isEmpty()) {
                sendError(player, "commandError", getUsage(player));
                return;
            }

            // Если пользователь ничего не ввёл регистрируем максимально возможное число
            this.amount = stack.getCount();
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
        stack.shrink(amount);

        // Синхронизируем данные с сервером
        JourneyUtils.addResearchAndSync(player, stack, amount);

        // Переводим название предмета с эльфийского
        ITextComponent name = stack.getTextComponent();

        // Отправляем игроку сколько он изучил
        player.sendMessage(new TextComponentTranslation("chat.journeymode.add", name, amount));
    }


    /// --- Команда на бесплатное изучение предмета в целях науки ---

    private void handleResearch(EntityPlayer player, ItemStack stack, IResearch cap, String[] args) {

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

        // Синхронизируем данные с сервером
        JourneyUtils.addResearchAndSync(player, stack, amount);

        // Переводим название предмета с эльфийского
        ITextComponent name = stack.getTextComponent();

        // Отправляем игроку сколько он изучил
        player.sendMessage(new TextComponentTranslation("chat.journeymode.add", name, amount));
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
        int has = cap.getResearch(new ItemStack(item));
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

    private void handleProgress(EntityPlayer player, IResearch cap, String[] args) {
        List<Map.Entry<ResearchKey, Integer>> totalList = new ArrayList<>(cap.getReadOnlyMap().entrySet());

        if (totalList.isEmpty()) {
            player.sendMessage(new TextComponentTranslation(TextFormatting.RED + "chat.journeymode.noResearch"));
            return;
        }

        final int ITEMS_PER_PAGE = 6; // Сколько строк поместится в чат за раз
        int totalItems = totalList.size();
        int maxPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        int currentPage = 1;

        if (args.length >= 2) {
            try {
                currentPage = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentTranslation(TextFormatting.RED + "chat.journeymode.wrongPage"));
                return;
            }
        }

        if (currentPage < 1) currentPage = 1;
        if (currentPage > maxPages) currentPage = maxPages;

        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        player.sendMessage(new TextComponentTranslation(
                TextFormatting.GOLD + "=== " +
                        TextFormatting.YELLOW + "Прогресс исследований (Стр. " + currentPage + " из " + maxPages + ")" +
                        TextFormatting.GOLD + " ==="
        ));

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<ResearchKey, Integer> entry = totalList.get(i);
            ResearchKey key = entry.getKey();
            int currentProgress = entry.getValue();

            ItemStack displayStack = new ItemStack(
                    ForgeRegistries.ITEMS.getValue(key.getRegistryName()),
                    1,
                    key.getMeta()
            );
            if (key.getCleanedNbt() != null) {
                displayStack.setTagCompound(key.getCleanedNbt());
            }

            int requiredAmount = JourneyUtils.getRequiredAmount(displayStack);

            TextFormatting color = (currentProgress >= requiredAmount) ? TextFormatting.GREEN : TextFormatting.GRAY;

            String itemDisplayName = displayStack.getDisplayName();

            String nbtMarker = (key.getCleanedNbt() != null) ? TextFormatting.LIGHT_PURPLE + " [+NBT]" : "";

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_GRAY + " - " +
                            color + itemDisplayName + nbtMarker +
                            TextFormatting.DARK_AQUA + " [" + currentProgress + "/" + requiredAmount + "]"
            ));
        }

        if (currentPage < maxPages) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "Используйте " +
                            TextFormatting.AQUA + "/jm progress " + (currentPage + 1) +
                            TextFormatting.GRAY + ", чтобы открыть следующую страницу."
            ));
        }
    }


    /// --- Команда на бесплатную выдачу изученного предмета ---

    private void handleClear(EntityPlayer player, IResearch cap) {

        // Вызываем метод очистки карты исследований
        cap.clear();

        // Выводим игроку сообщение об успешной очистке исследований
        player.sendMessage(new TextComponentTranslation("chat.journeymode.clear"));
    }


    /// --- Команда на бесплатную выдачу изученного предмета ---

    private void handleRemove(EntityPlayer player, ItemStack stack, IResearch cap, String[] args) {
        // Вводим переменную предмета для удаления и предмета в руке
        String item;

        // Проверка, если пользователь ничего не ввёл
        if (args.length < 2) {

            // Если в руке пусто
            if (stack.isEmpty()) {
                sendError(player, "commandError", getUsage(player));
                return;
            }
        }

        // Если пользовать что-то ввёл, регистрируем
        else {
            item = args[1];
        }

        // Если карта исследований содержит этот предмет - сносим нафиг.
        if (cap.getResearch(stack) > 0) {
            cap.remove(stack);

            String localizedName = new ItemStack(stack.getItem()).getDisplayName();

            // Выводим сообщение об успешном снесении предмета из исследований, хе-хе
            player.sendMessage(new TextComponentTranslation("chat.journeymode.remove", localizedName));
        }
    }

    private void handleIgnore(EntityPlayer player, ItemStack stack, IResearch cap, String[] args) {
        if (stack.isEmpty()) {
            sendError(player, "commandError", getUsage(player));
        }

        if (!stack.hasTagCompound()) {
            sendError(player, "commandError", getUsage(player));
        }

        if (args.length == 1) {
            player.sendMessage(new TextComponentTranslation("chat.journeymode.ignoreHat"));
            for (String key : stack.getTagCompound().getKeySet()) {
                boolean isIgnored = ConfigHandler.IGNORED_TAGS.contains(key);
                String color = isIgnored ? TextComponentTranslation() : TextComponentTranslation();
                player.sendMessage(new TextComponentTranslation(color + key));
            }
            player.sendMessage(new TextComponentTranslation("chat.journeymode.ignore"));
            return;
        }

        String action = args[1].toLowerCase();

        if (args.length >= 3 && action.equals("add")) {
            String tagToIngore = args[2];

            ConfigHandler.IGNORED_TAGS.add(tagToIgnore);
            player.sendMessage(new TextComponentTranslation("chat.journeymode.addIgnore"));
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
            if (args.length == 2 && ("give".equals(args[0]) || "remove".equals(args[0]))) {
                List<String> researchedItems = new ArrayList<>();
                for (ResearchKey key : cap.getReadOnlyMap().keySet()) {
                    researchedItems.add(key.getRegistryName().toString());
                }
                return getListOfStringsMatchingLastWord(args, researchedItems);
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