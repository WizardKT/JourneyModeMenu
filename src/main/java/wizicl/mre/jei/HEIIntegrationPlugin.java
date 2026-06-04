package wizicl.mre.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;

@JEIPlugin
public class HEIIntegrationPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {

        // Передача обработчика в систему HEI
        registry.addAdvancedGuiHandlers(new JourneyMenuGuiHandler());

        System.out.println("[JourneyMode] Успешно зарегистрирован обработчик огибания для HEI/JEI!");
    }
}
