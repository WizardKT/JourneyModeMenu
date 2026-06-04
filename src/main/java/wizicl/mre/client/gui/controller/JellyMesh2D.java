package wizicl.mre.client.gui.controller;

public class JellyMesh2D {
    private final int cols;
    private final int rows;
    private final int numNodes;
    private final JellyBarMath jellyBar = new JellyBarMath();

    // Массивы координат для максимальной оптимизации (никаких объектов)
    private final float[] baseX, baseY; // Базовая (домашняя) сетка
    public final float[] curX, curY;  // Текущие искаженные координаты
    private final float[] velX, velY;   // Скорости узлов

    public final float width, height;

    public JellyMesh2D(int width, int height, int cols, int rows) {
        this.width = width;
        this.height = height;
        this.cols = cols;
        this.rows = rows;
        this.numNodes = cols * rows;

        baseX = new float[numNodes]; baseY = new float[numNodes];
        curX = new float[numNodes]; curY = new float[numNodes];
        velX = new float[numNodes]; velY = new float[numNodes];

        initGrid();
    }

    private void initGrid() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int i = r * cols + c;
                // Равномерно распределяем узлы по площади колбы
                baseX[i] = curX[i] = (width / (cols - 1)) * c;
                baseY[i] = curY[i] = (height / (rows - 1)) * r;
            }
        }
    }

    // Вызывать каждый тик (updateScreen)
    public void updatePhysics(int mouseX, int mouseY, int guiLeft, int guiTop, int barX, int barY) {
        float stiffness = 0.15f;  // Жесткость пружины (как быстро возвращается на место)
        float dampening = 0.75f;  // Вязкость воды (чем меньше, тем быстрее затухают волны)
        float mouseRadius = 18f;  // Радиус взаимодействия курсора
        float mouseForce = 4.0f;  // Сила "булька" от мышки

        // Переводим глобальные координаты мыши в локальные координаты внутри колбы
        float localMouseX = mouseX - (guiLeft + barX);
        float localMouseY = mouseY - (guiTop + barY);

        for (int i = 0; i < numNodes; i++) {
            // Сила возврата к базовой позиции (закон Гука)
            float forceX = (baseX[i] - curX[i]) * stiffness;
            float forceY = (baseY[i] - curY[i]) * stiffness;

            // [МИНИ-ПОДСКАЗКА]: Создаем подводное течение!
            // Если узел внутренний (не крайний), добавляем ему едва заметное автономное шевеление
            if (c(i) != 0 && c(i) != cols - 1 && r(i) != 0 && r(i) != rows - 1) {
                float idleTicks = net.minecraft.client.Minecraft.getMinecraft().player.ticksExisted;
                forceX += (float) Math.sin(idleTicks * 0.04f + r(i)) * 0.015f;
                forceY += (float) Math.cos(idleTicks * 0.04f + c(i)) * 0.015f;
            }

            // Взаимодействие с курсором (если мышка внутри радиуса)
            float dx = curX[i] - localMouseX;
            float dy = curY[i] - localMouseY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < mouseRadius && dist > 0.1f) {
                float push = (mouseRadius - dist) / mouseRadius * mouseForce;
                forceX += (dx / dist) * push;
                forceY += (dy / dist) * push;
            }

            // Интегрируем скорость и позицию
            velX[i] = (velX[i] + forceX) * dampening;
            velY[i] = (velY[i] + forceY) * dampening;

            curX[i] += velX[i];
            curY[i] += velY[i];

            // Жесткие рамки (Clamp), чтобы полигоны не вывернулись наизнанку
            float maxDist = 6f; // Максимальное смещение узла в пикселях
            curX[i] = Math.max(baseX[i] - maxDist, Math.min(baseX[i] + maxDist, curX[i]));
            curY[i] = Math.max(baseY[i] - maxDist, Math.min(baseY[i] + maxDist, curY[i]));

            // Кэшируем высоту сетки, чтобы под действием сил мышки узлы физически не могли упасть ниже дна колбы
            if (curY[i] > this.height) {
                curY[i] = this.height;
            }

            // Жестко фиксируем края колбы, чтобы вода не вытекала за рамки интерфейса
            if (c(i) == 0 || c(i) == cols - 1 || r(i) == 0 || r(i) == rows - 1) {
                curX[i] = baseX[i];
                curY[i] = baseY[i];
            }
        }
    }

    // Вызывать каждый тик перед updatePhysics
    public void updateWaterLevel(float currentWaterHeight) {
        // Где находится поверхность (0 - это верх колбы, height - это дно)
        float surfaceY = this.height - currentWaterHeight;

        // [МИНИ-ПОДСКАЗКА]: Используем ванильные тики игрока как генератор времени для волн
        float waveTicks = net.minecraft.client.Minecraft.getMinecraft().player.ticksExisted;

        for (int r = 0; r < rows; r++) {
            // Считаем пропорцию r=0 это 0.0 (поверхность), самый нижний ряд - это 1.0 (дно)
            float fraction = (float) r / (rows - 1);

            // Вычисляем новую Y-координату для всего ряда
            float targetY = surfaceY + (currentWaterHeight * fraction);

            for (int c = 0; c < cols; c++) {
                int i = r * cols + c;

                if (r == 0 && currentWaterHeight > 1.0f) {
                    // Смешивание синуса и косинуса с разной скоростью создает эффект хаотичного покачивания воды
                    float wave = (float) Math.sin(waveTicks * 0.12f + c * 0.7f) * 1.3f
                            + (float) Math.cos(waveTicks * 0.07f + c * 0.3f) * 0.5f;
                    baseY[i] = targetY + wave;
                } else {
                    baseY[i] = targetY;
                }

                // [МИНИ-ПОДСКАЗКА]: Если вода резко упала, а точка осталась высоко,
                // мы принудительно тянем её вниз, чтобы текстура не порвалась
                if (curY[i] < surfaceY) {
                    curY[i] = surfaceY;
                }
            }
        }
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }
    private int c(int i) { return i % cols; }
    private int r(int i) { return i / cols; }
}