package me.wizicl.journeymode.proxy;

import me.wizicl.journeymode.main.JourneyConfig;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy{

    // Добавляем переменную новой клавиши
    public static KeyBinding keyBindOpenGui;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // Регистрируем новую клавишу на J
        keyBindOpenGui = new KeyBinding("key.journeymode.open_menu", JourneyConfig.openGuiKey, "key.categories.journeymode");
        ClientRegistry.registerKeyBinding(keyBindOpenGui);
    }


}
