package wizicl.mre.client.gui.controller;

public class JellyBarMath {
    private float currentValue = 0f;
    private float targetValue = 0f;

    // Настройки "желейности"
    private final float lerpSpeed = 0.15f; // Скорость догоняния
    private final float wobbleSpeed = 0.5f; // Скорость вибрации
    private final float wobbleAmplitude = 0.3f; // Сила вибрации

    // Установка нового значения
    public void setTarget(float target) {
        this.targetValue = target;
    }

    // Метод для обновления логики (вызывать в updateScreen())
    public void updateTick() {
        // Обычный Lerp для догоняния цели
        float diff = targetValue - currentValue;
        currentValue += diff * lerpSpeed;
    }

    // Вычисление итогового размера с учетом частичных тиков (для drawScreen)
    public float getRenderSize(int playerTicks, float partialTicks) {
        float diff = targetValue - currentValue;

        // Магия тригонометрии: вибрация работает только пока значения не сравнялись
        float time = (playerTicks + partialTicks) * wobbleSpeed;
        float wobble = (float) Math.sin(time) * diff * wobbleAmplitude;

        return currentValue + wobble;
    }

    public void forceValue(float val) {
        this.currentValue = val;
        this.targetValue = val;
    }
}