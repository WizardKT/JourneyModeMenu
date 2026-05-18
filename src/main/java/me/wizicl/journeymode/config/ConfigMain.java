package me.wizicl.journeymode.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

@Config(modid = "journeymode", name = "journeymode_config")
public class ConfigMain {
    @Config.Name("Stack Multiplier")
    @Config.Comment("How many stacks of a block are needed to fully research it (default 10)")
    public static int stackMultiplier = 10;

    @Config.Name("Auto research shift click")
    @Config.Comment("When true make research menu auto research items when clicking shift click per item, useful for big amount of researches")
    public static boolean easy_research_shift = false;

    @Mod.EventBusSubscriber(modid = "journeymode")
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("journeymode")) {
                ConfigManager.sync("journeymode", Config.Type.INSTANCE);
            }
        }
    }
}
