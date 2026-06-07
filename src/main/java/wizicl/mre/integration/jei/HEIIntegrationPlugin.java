package wizicl.mre.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import wizicl.mre.MatterReplicationEngine;

@JEIPlugin
public class HEIIntegrationPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        // Передача обработчика в систему HEI/JEI
        registry.addAdvancedGuiHandlers(new JourneyMenuGuiHandler());

        // Заменили System.out на логгер
        MatterReplicationEngine.logger.info("Успешно зарегистрирован обработчик областей для HEI/JEI!");
    }
}
