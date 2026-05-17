package me.wizicl.journeymode.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Config(modid = "journeymode", name = "journeymode_nbt")
public class ConfigNBT {
    @Config.Comment({"A list of NBT tags that will be ignored when researching items.",})
    @Config.Name("Ignored NBT Tags")
    public static String[] IGNORED_TAGS = new String[] {"display", "RepairCost"};

    private static Set<String> ignoredTagsSet = null;

    // Проверяет, занесен ли тег в черный список.
    public static boolean isTagIgnored(String tag) {
        if (ignoredTagsSet == null) {
            initTagSet();
        }
        return ignoredTagsSet.contains(tag);
    }

    /**
     * Динамически добавляет тег в массив, обновляет кэш и записывает файл на диск:
     * Возвращает true, если тег добавлен успешно;
     * Возвращает false, если такой тег уже был в списке.
     */

    public static boolean addTag(String tag) {

        // Создаем динамический список на основе текущего массива
        ArrayList<String> list = new ArrayList<>(Arrays.asList(IGNORED_TAGS));

        // Если такой тег уже есть — ничего не делаем
        if (list.contains(tag)) {
            return false;
        }

        // Добавляем новый тег в список
        list.add(tag);

        // Превращаем список обратно в массив фиксированной длины и сохраняем в конфиг
        String[] newArray = new String[list.size()];
        IGNORED_TAGS = list.toArray(newArray);

        // Обновляем кэш в оперативной памяти
        initTagSet();

        // Заставляем Forge перезаписать файл .cfg на жестком диске
        ConfigManager.sync("journeymode", Config.Type.INSTANCE);
        return true;
    }

    public static boolean removeTag(String tag) {
        ArrayList<String> list = new ArrayList<>(Arrays.asList(IGNORED_TAGS));

        if (list.contains(tag)) {
            return false;
        }

        list.remove(tag);

        String[] newArray = new String[list.size()];
        IGNORED_TAGS = list.toArray(newArray);

        initTagSet();

        ConfigManager.sync("journeymode", Config.Type.INSTANCE);
        return true;
    }

    public static Set <String> getIgnoredTagsSet() {
        if (ignoredTagsSet == null) {
            initTagSet();
        }
        return ignoredTagsSet;
    }

    // Инициализация кэша
    private static void initTagSet() {
        ignoredTagsSet = new HashSet<>(Arrays.asList(IGNORED_TAGS));
    }

    // Обновление и синхронизация конфига
    @Mod.EventBusSubscriber(modid = "journeymode")
    public static class RegistryHandler {
        @SubscribeEvent
        public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("journeymode")) {
                ConfigManager.sync("journeymode", Config.Type.INSTANCE);
                initTagSet();
            }
        }
    }


}