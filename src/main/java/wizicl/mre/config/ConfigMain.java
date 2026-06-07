package wizicl.mre.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wizicl.mre.Reference;

@Config(modid = Reference.MOD_ID, name = "MRE_Main")
public class ConfigMain {

    @Config.Name("Stack Multiplier")
    @Config.Comment("How many stacks of a block are needed to fully research it (default 10)")
    public static int costMultiplier = 10;

    // --- Авто-изучение ---
    @Config.Name("Auto Research Settings")
    @Config.Comment("Global settings for automatic item research")
    public static final AutoResearchSettings autoResearch = new AutoResearchSettings();

    public static class AutoResearchSettings {
        @Config.Name("Auto research shift click")
        @Config.Comment("When true make research menu auto research items when clicking shift click per item, useful for big amount of researches")
        public boolean easy_research_shift = false;

        @Config.Name("Enable in Vanilla Workbench")
        @Config.Comment("Should the auto-research button appear in the vanilla crafting table?")
        public boolean enableInWorkbench = true;

        @Config.Name("Enable in Vanilla Inventory")
        @Config.Comment("Should the auto-research button appear in the vanilla inventory table?")
        public boolean enableInInventory = true;

        @Config.Name("Consume Items on Auto-Craft")
        @Config.Comment("If true, items are consumed when auto-researched. If false, they are just scanned.")
        public boolean consumeItems = false;
    }

    // --- Синхронизация настройки в реал тайм ---

    @Mod.EventBusSubscriber(modid = Reference.MOD_ID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Reference.MOD_ID)) {
                ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }
}
