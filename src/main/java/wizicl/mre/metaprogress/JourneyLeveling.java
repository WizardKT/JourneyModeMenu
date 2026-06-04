package wizicl.mre.metaprogress;

import wizicl.mre.config.ConfigMain;

public class JourneyLeveling {
    public static final int MAX_LEVEL = 100;
    private static final int BASE_ITEMS = 64;

    /**
     * Динамическая базовая стоимость.
     * Защищена от математического коллапса (если кто-то впишет 0 или минус в конфиг).
     */
    public static int getActualBaseItems() {
        int multiplier = Math.max(1, ConfigMain.costMultiplier);
        return BASE_ITEMS * multiplier;
    }

    /**
     * Возвращает, сколько ВСЕГО предметов нужно изучить,
     * чтобы достичь указанного уровня.
     */
    public static int getItemsRequiredForLevel(int targetLevel) {
        if (targetLevel <= 0) return 0;
        // Экспоненциальная Роблокс-математика
        return (int) (getActualBaseItems() * Math.pow(targetLevel, 1.5));
    }

    /**
     * Вычисляет текущий уровень на основе изученных предметов.
     */
    public static int calculateCurrentLevel(int totalResearched) {
        int level = 0;

        // Цикл: пока количества изученного хватает, чтобы покрыть
        // требования СЛЕДУЮЩЕГО уровня — повышаем текущий уровень.
        while (level < MAX_LEVEL && totalResearched >= getItemsRequiredForLevel(level + 1)) {
            level++;
        }

        return level;
    }

    /**
     * Метод для GUI: сколько предметов осталось изучить для получения следующего уровня.
     */
    public static int getItemsToNextLevel(int totalResearched) {
        int currentLevel = calculateCurrentLevel(totalResearched);
        if (currentLevel >= MAX_LEVEL) return 0; // Кап достигнут

        int currentLevelReq = getItemsRequiredForLevel(currentLevel);
        int nextLevelReq = getItemsRequiredForLevel(currentLevel + 1);

        int progressInCurrentLevel = totalResearched - currentLevelReq;
        int itemsNeededForNext = nextLevelReq - currentLevelReq;

        return itemsNeededForNext - progressInCurrentLevel;
    }
}
