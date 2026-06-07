package wizicl.mre.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wizicl.mre.Reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Config(modid = Reference.MOD_ID, name = "MRE_NBT")
public class ConfigNBT {

    @Config.Comment("A list of NBT tags that will be ignored when researching items.")
    @Config.Name("Ignored NBT Tags")
    public static String[] IGNORED_TAGS = new String[] {"display", "RepairCost"};

    private static Set<String> ignoredTagsSet = null;

    public static boolean isTagIgnored(String tag) {
        if (ignoredTagsSet == null) {
            initTagSet();
        }
        return ignoredTagsSet.contains(tag);
    }

    /**
     * Динамически добавляет тег в массив, обновляет кэш и пишет на диск.
     */

    public static boolean addTag(String tag) {
        var list = new ArrayList<>(Arrays.asList(IGNORED_TAGS));

        if (list.contains(tag)) {
            return false; // Тег уже есть, выходим
        }

        list.add(tag);

        // Ультра-быстрый перевод списка в массив в Java 25! Без ручного выделения памяти
        IGNORED_TAGS = list.toArray(String[]::new);

        initTagSet(); // Обновляем кэш памяти
        ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE); // Сохраняем на диск mre.cfg
        return true;
    }

    /**
     * Динамически удаляет тег из массива и обновляет файл конфига.
     */

    public static boolean removeTag(String tag) {
        var list = new ArrayList<>(Arrays.asList(IGNORED_TAGS));

        // ИСПРАВИЛИ БАГ: если тега НЕТ в списке, то и удалять нечего — возвращаем false
        if (!list.contains(tag)) {
            return false;
        }

        list.remove(tag);
        IGNORED_TAGS = list.toArray(String[]::new);

        initTagSet();
        ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
        return true;
    }

    public static Set<String> getIgnoredTagsSet() {
        if (ignoredTagsSet == null) {
            initTagSet();
        }
        return ignoredTagsSet;
    }

    // Инициализация кэша
    private static void initTagSet() {
        ignoredTagsSet = new HashSet<>(Arrays.asList(IGNORED_TAGS));
    }

    // Автоматическое обновление конфига при изменении через внутриигровое меню Forge
    @Mod.EventBusSubscriber(modid = Reference.MOD_ID)
    public static class RegistryHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Reference.MOD_ID)) {
                ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
                initTagSet();
            }
        }
    }
}