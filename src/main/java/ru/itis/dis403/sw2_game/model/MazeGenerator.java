package ru.itis.dis403.sw2_game.model;

import java.io.Serializable;
import java.util.*;

public class MazeGenerator implements Serializable {
    private int width;
    private int height;
    private int level;
    private Random random;

    public MazeGenerator(int width, int height, int level) {
        this.width = width;
        this.height = height;
        this.level = level;
        this.random = new Random(level * 1000 + width * height);
    }

    public int[][] generateMaze() {
        int[][] grid = new int[height][width];

        // Инициализация всех клеток как стен
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = 1;
            }
        }

        // Выбор алгоритма в зависимости от уровня
        switch (level) {
            case 1:
                return generateSpiralMaze(grid);
            case 2:
                return generateCrossMaze(grid);
            case 3:
                return generateDiamondMaze(grid);
            case 4:
                return generateCircularMaze(grid);
            case 5:
                return generateMazeWithTeleports(grid);
            case 6:
                return generateMazeWithMultipleExits(grid);
            case 7:
                return generateRandomWalkMaze(grid);
            case 8:
                return generateRecursiveDivisionMaze(grid);
            case 9:
                return generateKeyAndDoorMaze(grid);
            case 10:
                return generateFinalBossMaze(grid);
            default:
                return generatePrimMaze(grid);
        }
    }

    // 1. СПИРАЛЬНЫЙ ЛАБИРИНТ (уровень 1) - ДВЕРИ С КЛЮЧАМИ
    private int[][] generateSpiralMaze(int[][] grid) {
        System.out.println("=== ГЕНЕРАЦИЯ СПИРАЛЬНОГО ЛАБИРИНТА (УРОВЕНЬ 1) ===");

        int centerX = width / 2;
        int centerY = height / 2;
        int maxRadius = Math.min(width, height) / 2 - 2;

        // Создаем спираль с центральной площадкой
        for (int y = centerY - 2; y <= centerY + 2; y++) {
            for (int x = centerX - 2; x <= centerX + 2; x++) {
                if (isValid(x, y)) grid[y][x] = 0;
            }
        }

        // Спиральные кольца
        for (int r = 1; r <= maxRadius; r++) {
            int ringSize = r * 8;
            for (int i = 0; i < ringSize; i++) {
                double angle = 2 * Math.PI * i / ringSize;
                int x = (int)(centerX + r * Math.cos(angle));
                int y = (int)(centerY + r * Math.sin(angle));

                if (isValid(x, y)) {
                    grid[y][x] = 0;
                    // Соединяем с внутренним кольцом
                    if (r > 1 && i % (r*2) == 0) {
                        int innerX = (int)(centerX + (r-1) * Math.cos(angle));
                        int innerY = (int)(centerY + (r-1) * Math.sin(angle));
                        createPath(grid, innerX, innerY, x, y);
                    }
                }
            }
        }

        // Выходы из спирали
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            for (int r = 1; r <= maxRadius; r++) {
                int x = (int)(centerX + r * Math.cos(angle));
                int y = (int)(centerY + r * Math.sin(angle));
                if (isValid(x, y)) grid[y][x] = 0;
            }
        }

        // ДОБАВЛЯЕМ ДВЕРИ С КЛЮЧАМИ ДЛЯ УРОВНЯ 1
        addDoorsAndKeysToLevel(grid, 2, 3);

        System.out.println("=== СПИРАЛЬНЫЙ ЛАБИРИНТ СОЗДАН ===");
        return grid;
    }

    // 2. КРЕСТООБРАЗНЫЙ ЛАБИРИНТ (уровень 2)
    private int[][] generateCrossMaze(int[][] grid) {
        System.out.println("=== ГЕНЕРАЦИЯ КРЕСТООБРАЗНОГО ЛАБИРИНТА (УРОВЕНЬ 2) ===");

        // Центральный крест
        for (int y = 1; y < height - 1; y++) {
            grid[y][width / 2] = 0;
            if (y % 3 == 0) {
                for (int dx = -2; dx <= 2; dx++) {
                    int x = width / 2 + dx;
                    if (isValid(x, y)) grid[y][x] = 0;
                }
            }
        }

        for (int x = 1; x < width - 1; x++) {
            grid[height / 2][x] = 0;
            if (x % 3 == 0) {
                for (int dy = -2; dy <= 2; dy++) {
                    int y = height / 2 + dy;
                    if (isValid(x, y)) grid[y][x] = 0;
                }
            }
        }

        // Угловые комнаты
        createRoom(grid, 2, 2, 4, 4);
        createRoom(grid, width - 6, 2, 4, 4);
        createRoom(grid, 2, height - 6, 4, 4);
        createRoom(grid, width - 6, height - 6, 4, 4);

        // Соединяем комнаты с центром
        createPath(grid, 4, 4, width / 2, 4);
        createPath(grid, width - 6, 4, width / 2, 4);
        createPath(grid, 4, height - 6, width / 2, height - 6);
        createPath(grid, width - 6, height - 6, width / 2, height - 6);

        // ДОБАВЛЯЕМ ДВЕРИ С КЛЮЧАМИ ДЛЯ УРОВНЯ 2
        addDoorsAndKeysToLevel(grid, 3, 4);

        System.out.println("=== КРЕСТООБРАЗНЫЙ ЛАБИРИНТ СОЗДАН ===");
        return grid;
    }

    // 3. АЛМАЗНЫЙ ЛАБИРИНТ (уровень 3)
    private int[][] generateDiamondMaze(int[][] grid) {
        System.out.println("=== ГЕНЕРАЦИЯ АЛМАЗНОГО ЛАБИРИНТА (УРОВЕНЬ 3) ===");

        int centerX = width / 2;
        int centerY = height / 2;
        int size = Math.min(width, height) / 2 - 2;

        // Ромб
        for (int i = -size; i <= size; i++) {
            int rowWidth = size - Math.abs(i);
            for (int j = -rowWidth; j <= rowWidth; j++) {
                int x = centerX + j;
                int y = centerY + i;
                if (isValid(x, y)) grid[y][x] = 0;
            }
        }

        // Диагональные перегородки
        for (int d = 1; d < size; d += 2) {
            for (int i = -d; i <= d; i++) {
                int x1 = centerX - d + Math.abs(i);
                int y1 = centerY + i;
                if (isValid(x1, y1) && random.nextDouble() > 0.3) grid[y1][x1] = 1;

                int x2 = centerX + d - Math.abs(i);
                int y2 = centerY + i;
                if (isValid(x2, y2) && random.nextDouble() > 0.3) grid[y2][x2] = 1;
            }
        }

        // Проходы в перегородках
        for (int d = 2; d < size; d += 3) {
            int breakPos = random.nextInt(d * 2 + 1) - d;
            int x = centerX - d + Math.abs(breakPos);
            int y = centerY + breakPos;
            if (isValid(x, y)) grid[y][x] = 0;

            breakPos = random.nextInt(d * 2 + 1) - d;
            x = centerX + d - Math.abs(breakPos);
            y = centerY + breakPos;
            if (isValid(x, y)) grid[y][x] = 0;
        }

        // ДОБАВЛЯЕМ ДВЕРИ С КЛЮЧАМИ ДЛЯ УРОВНЯ 3
        addDoorsAndKeysToLevel(grid, 2, 3);

        // Добавляем телепорты
        int numTeleports = 2;
        for (int i = 0; i < numTeleports; i++) {
            Position pos = findEmptySpace(grid, 4);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 2; // Телепорт
                System.out.println("Добавлен телепорт на позиции: " + pos);
            }
        }

        System.out.println("=== АЛМАЗНЫЙ ЛАБИРИНТ СОЗДАН ===");
        return grid;
    }

    // 4. КРУГОВОЙ ЛАБИРИНТ (уровень 4)
    private int[][] generateCircularMaze(int[][] grid) {
        System.out.println("=== ГЕНЕРАЦИЯ КРУГОВОГО ЛАБИРИНТА (УРОВЕНЬ 4) ===");

        int centerX = width / 2;
        int centerY = height / 2;
        int maxRadius = Math.min(width, height) / 2 - 2;

        // Центральная комната
        createRoom(grid, centerX - 3, centerY - 3, 6, 6);

        // Концентрические круги
        for (int r = 1; r <= maxRadius; r++) {
            int circumference = (int)(2 * Math.PI * r);
            for (int p = 0; p < circumference; p++) {
                double angle = 2 * Math.PI * p / circumference;
                int x = (int)(centerX + r * Math.cos(angle));
                int y = (int)(centerY + r * Math.sin(angle));
                if (isValid(x, y)) grid[y][x] = 0;
            }

            // Прерывания в кругах
            if (r % 2 == 0) {
                int breaks = 3 + random.nextInt(3);
                for (int b = 0; b < breaks; b++) {
                    double breakAngle = b * (2 * Math.PI / breaks) + random.nextDouble() * 0.5;
                    for (int dr = -1; dr <= 1; dr++) {
                        int x = (int)(centerX + (r + dr) * Math.cos(breakAngle));
                        int y = (int)(centerY + (r + dr) * Math.sin(breakAngle));
                        if (isValid(x, y)) grid[y][x] = 1;
                    }
                }
            }
        }

        // Радиальные проходы
        int numRadial = 4 + random.nextInt(4);
        for (int i = 0; i < numRadial; i++) {
            double angle = i * (2 * Math.PI / numRadial);
            for (int r = 1; r <= maxRadius; r++) {
                int x = (int)(centerX + r * Math.cos(angle));
                int y = (int)(centerY + r * Math.sin(angle));
                if (isValid(x, y)) grid[y][x] = 0;
            }
        }

        // ДОБАВЛЯЕМ ДВЕРИ С КЛЮЧАМИ ДЛЯ УРОВНЯ 4
        addDoorsAndKeysToLevel(grid, 3, 4);

        // Добавляем ловушки
        int numTraps = 3;
        for (int i = 0; i < numTraps; i++) {
            Position pos = findEmptySpace(grid, 3);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 6; // Ловушка
                System.out.println("Добавлена ловушка на позиции: " + pos);
            }
        }

        System.out.println("=== КРУГОВОЙ ЛАБИРИНТ СОЗДАН ===");
        return grid;
    }

    // 5. ЛАБИРИНТ С ТЕЛЕПОРТАМИ (уровень 5)
    private int[][] generateMazeWithTeleports(int[][] grid) {
        System.out.println("=== ГЕНЕРАЦИЯ ЛАБИРИНТА С ТЕЛЕПОРТАМИ (УРОВЕНЬ 5) ===");

        generatePrimMaze(grid);

        // ДОБАВЛЯЕМ ДВЕРИ С КЛЮЧАМИ ДЛЯ УРОВНЯ 5
        addDoorsAndKeysToLevel(grid, 3, 4);

        // Добавляем телепорты (пары)
        int numTeleportPairs = 4;
        List<Position> teleportPositions = new ArrayList<>();

        for (int i = 0; i < numTeleportPairs * 2; i++) {
            Position pos = findEmptySpace(grid, 3);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 2;
                teleportPositions.add(pos);
                System.out.println("Добавлен телепорт на позиции: " + pos);
            }
        }

        // Соединяем телепорты коридорами
        for (int i = 0; i < teleportPositions.size(); i += 2) {
            if (i + 1 < teleportPositions.size()) {
                Position t1 = teleportPositions.get(i);
                Position t2 = teleportPositions.get(i + 1);
                createPath(grid, t1.getX(), t1.getY(), t2.getX(), t2.getY());
            }
        }

        // Добавляем несколько ловушек
        int numTraps = 3;
        for (int i = 0; i < numTraps; i++) {
            Position pos = findEmptySpace(grid, 4);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 6;
                System.out.println("Добавлена ловушка на позиции: " + pos);
            }
        }

        System.out.println("=== ЛАБИРИНТ С ТЕЛЕПОРТАМИ СОЗДАН ===");
        return grid;
    }

    // 6. ЛАБИРИНТ С НЕСКОЛЬКИМИ ВЫХОДАМИ (уровень 6)
    private int[][] generateMazeWithMultipleExits(int[][] grid) {
        System.out.println("=== ГЕНЕРАЦИЯ ЛАБИРИНТА С НЕСКОЛЬКИМИ ВЫХОДАМИ (УРОВЕНЬ 6) ===");

        generatePrimMaze(grid);

        // ДОБАВЛЯЕМ ДВЕРИ С КЛЮЧАМИ ДЛЯ УРОВНЯ 6
        addDoorsAndKeysToLevel(grid, 4, 5);

        // Добавляем ложные выходы
        int numFakeExits = 6;
        for (int i = 0; i < numFakeExits; i++) {
            Position pos = findEmptySpace(grid, 4);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 3;
                System.out.println("Добавлен ложный выход на позиции: " + pos);
                // Создаем тупиковый путь к ложному выходу
                createDeadEnd(grid, pos.getX(), pos.getY(), 2);
            }
        }

        // Добавляем несколько ловушек
        int numTraps = 4;
        for (int i = 0; i < numTraps; i++) {
            Position pos = findEmptySpace(grid, 3);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 6;
                System.out.println("Добавлена ловушка на позиции: " + pos);
            }
        }

        System.out.println("=== ЛАБИРИНТ С НЕСКОЛЬКИМИ ВЫХОДАМИ СОЗДАН ===");
        return grid;
    }

    // 7. СЛУЧАЙНОЕ БЛУЖДАНИЕ (уровень 7)
    private int[][] generateRandomWalkMaze(int[][] grid) {
        System.out.println("=== ГЕНЕРАЦИЯ ЛАБИРИНТА СЛУЧАЙНОГО БЛУЖДАНИЯ (УРОВЕНЬ 7) ===");

        int numWalkers = 5;
        int stepsPerWalker = (width * height) / 15;

        for (int w = 0; w < numWalkers; w++) {
            int x = 2 + random.nextInt(width - 4);
            int y = 2 + random.nextInt(height - 4);

            for (int s = 0; s < stepsPerWalker; s++) {
                // Очищаем текущую клетку и вокруг
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (isValid(nx, ny) && random.nextDouble() > 0.3) {
                            grid[ny][nx] = 0;
                        }
                    }
                }

                // Двигаемся в случайном направлении
                int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                int dir = random.nextInt(4);
                int attempts = 0;
                while (attempts < 10) {
                    int nx = x + directions[dir][0];
                    int ny = y + directions[dir][1];
                    if (isValid(nx, ny)) {
                        x = nx;
                        y = ny;
                        break;
                    }
                    dir = random.nextInt(4);
                    attempts++;
                }
            }
        }

        // Заполняем мелкие пустоты
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (grid[y][x] == 1) {
                    int neighbors = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (isValid(x+dx, y+dy) && grid[y+dy][x+dx] == 0) {
                                neighbors++;
                            }
                        }
                    }
                    if (neighbors >= 5) {
                        grid[y][x] = 0;
                    }
                }
            }
        }

        // ДОБАВЛЯЕМ ДВЕРИ С КЛЮЧАМИ ДЛЯ УРОВНЯ 7
        addDoorsAndKeysToLevel(grid, 3, 4);

        // Добавляем телепорты, ловушки и дополнительные ключи
        int numSpecial = 6;
        for (int i = 0; i < numSpecial; i++) {
            Position pos = findEmptySpace(grid, 3);
            if (pos != null) {
                int type = random.nextInt(4);
                switch (type) {
                    case 0:
                        grid[pos.getY()][pos.getX()] = 2; // Телепорт
                        System.out.println("Добавлен телепорт на позиции: " + pos);
                        break;
                    case 1:
                        grid[pos.getY()][pos.getX()] = 5; // Ключ
                        System.out.println("Добавлен ключ на позиции: " + pos);
                        break;
                    case 2:
                        grid[pos.getY()][pos.getX()] = 6; // Ловушка
                        System.out.println("Добавлена ловушка на позиции: " + pos);
                        break;
                    case 3:
                        grid[pos.getY()][pos.getX()] = 4; // Дополнительная дверь
                        System.out.println("Добавлена дополнительная дверь на позиции: " + pos);
                        break;
                }
            }
        }

        System.out.println("=== ЛАБИРИНТ СЛУЧАЙНОГО БЛУЖДАНИЯ СОЗДАН ===");
        return grid;
    }

    // 8. РЕКУРСИВНОЕ ДЕЛЕНИЕ (уровень 8)
    private int[][] generateRecursiveDivisionMaze(int[][] grid) {
        System.out.println("=== ГЕНЕРАЦИЯ РЕКУРСИВНОГО ЛАБИРИНТА ДЛЯ УРОВНЯ 8 ===");

        // Сначала делаем все проходимым
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                grid[y][x] = 0;
            }
        }

        // Внешние стены
        for (int x = 0; x < width; x++) {
            grid[0][x] = 1;
            grid[height-1][x] = 1;
        }
        for (int y = 0; y < height; y++) {
            grid[y][0] = 1;
            grid[y][width-1] = 1;
        }

        // Упрощенное рекурсивное деление
        recursiveDivideSimple(grid, 2, width - 3, 2, height - 3);

        // ГАРАНТИРУЕМ связность
        ensureConnectivity(grid);

        // ДОБАВЛЯЕМ ДВЕРИ С КЛЮЧАМИ ДЛЯ УРОВНЯ 8
        addDoorsAndKeysToLevel(grid, 4, 5);

        // Добавляем специальные элементы на уровень 8
        addSpecialElementsForLevel8(grid);

        System.out.println("=== РЕКУРСИВНЫЙ ЛАБИРИНТ СОЗДАН ===");
        return grid;
    }

    private void addSpecialElementsForLevel8(int[][] grid) {
        // Добавляем ловушки
        int numTraps = 4;
        for (int i = 0; i < numTraps; i++) {
            Position pos = findEmptySpace(grid, 4);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 6; // Ловушка
                System.out.println("Добавлена ловушка на позиции: " + pos);
            }
        }

        // Добавляем телепорты
        int numTeleports = 3;
        for (int i = 0; i < numTeleports; i++) {
            Position pos = findEmptySpace(grid, 5);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 2; // Телепорт
                System.out.println("Добавлен телепорт на позиции: " + pos);
            }
        }
    }

    private void recursiveDivideSimple(int[][] grid, int left, int right, int top, int bottom) {
        // Если область слишком мала, прекращаем
        if (right - left < 4 || bottom - top < 4) {
            return;
        }

        // Выбираем ориентацию стены
        boolean horizontal = (bottom - top) > (right - left) ? true :
                (right - left) > (bottom - top) ? false :
                        random.nextBoolean();

        if (horizontal) {
            // Горизонтальная стена
            int wallY = top + 2 + random.nextInt((bottom - top - 3) / 2) * 2;

            // Проход в стене
            int passageX = left + 1 + random.nextInt((right - left - 1) / 2) * 2;

            // Строим стену
            for (int x = left; x <= right; x++) {
                if (x != passageX) {
                    if (wallY >= top && wallY <= bottom) {
                        grid[wallY][x] = 1;
                    }
                }
            }

            // Рекурсивно делим
            recursiveDivideSimple(grid, left, right, top, wallY - 1);
            recursiveDivideSimple(grid, left, right, wallY + 1, bottom);

        } else {
            // Вертикальная стена
            int wallX = left + 2 + random.nextInt((right - left - 3) / 2) * 2;

            // Проход в стене
            int passageY = top + 1 + random.nextInt((bottom - top - 1) / 2) * 2;

            // Строим стену
            for (int y = top; y <= bottom; y++) {
                if (y != passageY) {
                    if (wallX >= left && wallX <= right) {
                        grid[y][wallX] = 1;
                    }
                }
            }

            // Рекурсивно делим
            recursiveDivideSimple(grid, left, wallX - 1, top, bottom);
            recursiveDivideSimple(grid, wallX + 1, right, top, bottom);
        }
    }

    // 9. ЛАБИРИНТ С КЛЮЧАМИ И ДВЕРЯМИ (уровень 9) - ОСНОВНОЙ УРОВЕНЬ ДЛЯ ДВЕРЕЙ
    private int[][] generateKeyAndDoorMaze(int[][] grid) {
        System.out.println("=== ГЕНЕРАЦИЯ ЛАБИРИНТА С КЛЮЧАМИ И ДВЕРЯМИ (УРОВЕНЬ 9) ===");

        generatePrimMaze(grid);

        // ГАРАНТИРУЕМ добавление 5 дверей с ключами
        int numDoors = 5;

        for (int i = 0; i < numDoors; i++) {
            // Сначала ищем позицию для двери
            Position doorPos = findEmptySpace(grid, 5);
            if (doorPos != null) {
                // Размещаем дверь
                grid[doorPos.getY()][doorPos.getX()] = 4; // Дверь
                System.out.println("Добавлена дверь " + (i+1) + " на позиции: " + doorPos);

                // Теперь ищем позицию для ключа (подальше от двери)
                Position keyPos = findEmptySpace(grid, 8);
                if (keyPos != null) {
                    // Размещаем ключ
                    grid[keyPos.getY()][keyPos.getX()] = 5; // Ключ
                    System.out.println("Добавлен ключ для двери " + (i+1) + " на позиции: " + keyPos);

                    // ГАРАНТИРУЕМ путь от ключа к двери
                    createGuaranteedPath(grid, keyPos.getX(), keyPos.getY(), doorPos.getX(), doorPos.getY());
                    System.out.println("Создан гарантированный путь от ключа к двери " + (i+1));
                }
            }
        }

        // Добавляем дополнительные двери без гарантированных ключей (для сложности)
        int numExtraDoors = 2;
        for (int i = 0; i < numExtraDoors; i++) {
            Position doorPos = findEmptySpace(grid, 6);
            if (doorPos != null) {
                grid[doorPos.getY()][doorPos.getX()] = 4; // Дверь
                System.out.println("Добавлена дополнительная дверь на позиции: " + doorPos);
                // Создаем тупиковый путь к этой двери
                createDeadEnd(grid, doorPos.getX(), doorPos.getY(), 3);
            }
        }

        // Добавляем ловушки
        int numTraps = 6;
        for (int i = 0; i < numTraps; i++) {
            Position pos = findEmptySpace(grid, 4);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 6; // Ловушка
                System.out.println("Добавлена ловушка на позиции: " + pos);
            }
        }

        // Добавляем несколько телепортов
        int numTeleports = 3;
        for (int i = 0; i < numTeleports; i++) {
            Position pos = findEmptySpace(grid, 5);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 2; // Телепорт
                System.out.println("Добавлен телепорт на позиции: " + pos);
            }
        }

        System.out.println("=== ЛАБИРИНТ С КЛЮЧАМИ И ДВЕРЯМИ СОЗДАН ===");
        return grid;
    }

    // 10. ФИНАЛЬНЫЙ БОСС-ЛАБИРИНТ (уровень 10)
    private int[][] generateFinalBossMaze(int[][] grid) {
        System.out.println("=== ГЕНЕРАЦИЯ ФИНАЛЬНОГО БОСС-ЛАБИРИНТА (УРОВЕНЬ 10) ===");

        generatePrimMaze(grid);

        // ГАРАНТИРУЕМ добавление 6 дверей с ключами
        int numDoors = 6;

        for (int i = 0; i < numDoors; i++) {
            Position doorPos = findEmptySpace(grid, 6);
            if (doorPos != null) {
                grid[doorPos.getY()][doorPos.getX()] = 4; // Дверь
                System.out.println("Добавлена дверь " + (i+1) + " на позиции: " + doorPos);

                Position keyPos = findEmptySpace(grid, 8);
                if (keyPos != null) {
                    grid[keyPos.getY()][keyPos.getX()] = 5; // Ключ
                    System.out.println("Добавлен ключ для двери " + (i+1) + " на позиции: " + keyPos);

                    // ГАРАНТИРУЕМ путь от ключа к двери
                    createGuaranteedPath(grid, keyPos.getX(), keyPos.getY(), doorPos.getX(), doorPos.getY());
                    System.out.println("Создан гарантированный путь от ключа к двери " + (i+1));
                }
            }
        }

        // Добавляем телепорты
        int numTeleports = 8;
        for (int i = 0; i < numTeleports; i++) {
            Position pos = findEmptySpace(grid, 3);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 2; // Телепорт
                System.out.println("Добавлен телепорт на позиции: " + pos);
            }
        }

        // Добавляем ловушки
        int numTraps = 10;
        for (int i = 0; i < numTraps; i++) {
            Position pos = findEmptySpace(grid, 3);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 6; // Ловушка
                System.out.println("Добавлена ловушка на позиции: " + pos);
            }
        }

        // Добавляем ложные выходы
        int numFakeExits = 4;
        for (int i = 0; i < numFakeExits; i++) {
            Position pos = findEmptySpace(grid, 4);
            if (pos != null) {
                grid[pos.getY()][pos.getX()] = 3; // Ложный выход
                System.out.println("Добавлен ложный выход на позиции: " + pos);
                createDeadEnd(grid, pos.getX(), pos.getY(), 3);
            }
        }

        System.out.println("=== ФИНАЛЬНЫЙ БОСС-ЛАБИРИНТ СОЗДАН ===");
        return grid;
    }

    // НОВЫЙ МЕТОД: ДОБАВЛЕНИЕ ДВЕРЕЙ И КЛЮЧЕЙ ДЛЯ УРОВНЯ
    private void addDoorsAndKeysToLevel(int[][] grid, int numDoors, int numKeys) {
        System.out.println("Добавляю двери и ключи для уровня: " + level);

        // Добавляем двери
        for (int i = 0; i < numDoors; i++) {
            Position doorPos = findEmptySpace(grid, 5);
            if (doorPos != null) {
                grid[doorPos.getY()][doorPos.getX()] = 4; // Дверь
                System.out.println("Добавлена дверь " + (i+1) + " на позиции: " + doorPos);
            }
        }

        // Добавляем ключи (больше чем дверей, чтобы было проще)
        for (int i = 0; i < numKeys; i++) {
            Position keyPos = findEmptySpace(grid, 4);
            if (keyPos != null) {
                grid[keyPos.getY()][keyPos.getX()] = 5; // Ключ
                System.out.println("Добавлен ключ " + (i+1) + " на позиции: " + keyPos);
            }
        }

        System.out.println("Добавлено " + numDoors + " дверей и " + numKeys + " ключей");
    }

    // =========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===========

    private int[][] generatePrimMaze(int[][] grid) {
        boolean[][] inMaze = new boolean[height][width];
        PriorityQueue<Edge> edges = new PriorityQueue<>();

        int startX = 1 + random.nextInt((width - 3) / 2) * 2;
        int startY = 1 + random.nextInt((height - 3) / 2) * 2;

        grid[startY][startX] = 0;
        inMaze[startY][startX] = true;
        addEdges(startX, startY, edges, grid, inMaze);

        while (!edges.isEmpty()) {
            Edge edge = edges.poll();

            if (!inMaze[edge.y][edge.x]) {
                grid[edge.y][edge.x] = 0;
                grid[edge.wallY][edge.wallX] = 0;
                inMaze[edge.y][edge.x] = true;
                addEdges(edge.x, edge.y, edges, grid, inMaze);
            }
        }

        return grid;
    }

    private void addEdges(int x, int y, PriorityQueue<Edge> edges, int[][] grid, boolean[][] inMaze) {
        int[][] directions = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};

        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];

            if (nx > 0 && nx < width - 1 && ny > 0 && ny < height - 1 && !inMaze[ny][nx]) {
                int wallX = x + dir[0] / 2;
                int wallY = y + dir[1] / 2;
                edges.add(new Edge(nx, ny, wallX, wallY, random.nextInt(100)));
            }
        }
    }

    private void ensureConnectivity(int[][] grid) {
        boolean[][] visited = new boolean[height][width];
        List<List<Position>> regions = new ArrayList<>();

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (grid[y][x] == 0 && !visited[y][x]) {
                    List<Position> region = new ArrayList<>();
                    Queue<Position> queue = new LinkedList<>();

                    queue.add(new Position(x, y));
                    visited[y][x] = true;

                    while (!queue.isEmpty()) {
                        Position current = queue.poll();
                        region.add(current);

                        int[][] dirs = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
                        for (int[] dir : dirs) {
                            int nx = current.getX() + dir[0];
                            int ny = current.getY() + dir[1];

                            if (nx >= 1 && nx < width - 1 && ny >= 1 && ny < height - 1 &&
                                    grid[ny][nx] == 0 && !visited[ny][nx]) {
                                visited[ny][nx] = true;
                                queue.add(new Position(nx, ny));
                            }
                        }
                    }

                    if (!region.isEmpty()) {
                        regions.add(region);
                    }
                }
            }
        }

        if (regions.size() > 1) {
            System.out.println("Обнаружено " + regions.size() + " изолированных областей. Соединяем...");

            for (int i = 0; i < regions.size() - 1; i++) {
                Position from = regions.get(i).get(random.nextInt(regions.get(i).size()));
                Position to = regions.get(i + 1).get(random.nextInt(regions.get(i + 1).size()));
                createSimpleCorridor(grid, from, to);
            }
        }
    }

    private void createSimpleCorridor(int[][] grid, Position from, Position to) {
        int x1 = from.getX();
        int y1 = from.getY();
        int x2 = to.getX();
        int y2 = to.getY();

        int stepX = (x1 < x2) ? 1 : -1;
        for (int x = x1; x != x2; x += stepX) {
            if (isValid(x, y1)) grid[y1][x] = 0;
        }

        int stepY = (y1 < y2) ? 1 : -1;
        for (int y = y1; y != y2; y += stepY) {
            if (isValid(x2, y)) grid[y][x2] = 0;
        }
    }

    // НОВЫЙ УЛУЧШЕННЫЙ МЕТОД ДЛЯ СОЗДАНИЯ ПУТИ
    private void createGuaranteedPath(int[][] grid, int x1, int y1, int x2, int y2) {
        int currentX = x1;
        int currentY = y1;

        // Сначала идем по горизонтали
        int stepX = (x1 < x2) ? 1 : -1;
        while (currentX != x2) {
            currentX += stepX;

            if (isValid(currentX, currentY)) {
                grid[currentY][currentX] = 0; // Проходимая клетка

                // Делаем коридор шире
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = currentX + dx;
                        int ny = currentY + dy;
                        if (isValid(nx, ny) && (dx == 0 || dy == 0) && grid[ny][nx] == 1) {
                            grid[ny][nx] = 0;
                        }
                    }
                }
            }
        }

        // Затем по вертикали
        int stepY = (y1 < y2) ? 1 : -1;
        while (currentY != y2) {
            currentY += stepY;

            if (isValid(currentX, currentY)) {
                grid[currentY][currentX] = 0; // Проходимая клетка

                // Делаем коридор шире
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = currentX + dx;
                        int ny = currentY + dy;
                        if (isValid(nx, ny) && (dx == 0 || dy == 0) && grid[ny][nx] == 1) {
                            grid[ny][nx] = 0;
                        }
                    }
                }
            }
        }
    }

    private Position findEmptySpace(int[][] grid, int minDistance) {
        // Список приоритетных позиций для дверей и ключей
        List<Position> priorityPositions = new ArrayList<>();

        // Сначала ищем позиции в коридорах (рядом со стенами)
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (grid[y][x] == 0) {
                    // Проверяем, есть ли рядом стены - это хорошее место для двери
                    boolean nearWall = false;
                    int[][] dirs = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
                    for (int[] dir : dirs) {
                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        if (isValid(nx, ny) && grid[ny][nx] == 1) {
                            nearWall = true;
                            break;
                        }
                    }

                    if (nearWall) {
                        priorityPositions.add(new Position(x, y));
                    }
                }
            }
        }

        // Если нашли приоритетные позиции, выбираем случайную
        if (!priorityPositions.isEmpty()) {
            Collections.shuffle(priorityPositions, random);
            Position pos = priorityPositions.get(0);

            // Проверяем, что вокруг достаточно свободного пространства
            boolean clear = true;
            for (int dy = -minDistance; dy <= minDistance && clear; dy++) {
                for (int dx = -minDistance; dx <= minDistance && clear; dx++) {
                    int nx = pos.getX() + dx;
                    int ny = pos.getY() + dy;
                    if (isValid(nx, ny) &&
                            (grid[ny][nx] == 2 || grid[ny][nx] == 3 ||
                                    grid[ny][nx] == 4 || grid[ny][nx] == 5 ||
                                    grid[ny][nx] == 6)) {
                        clear = false;
                    }
                }
            }

            if (clear) {
                return pos;
            }
        }

        // Если не нашли приоритетных, ищем любую свободную клетку
        int attempts = 0;
        while (attempts < 200) {
            int x = minDistance + random.nextInt(width - 2 * minDistance);
            int y = minDistance + random.nextInt(height - 2 * minDistance);

            if (grid[y][x] == 0) {
                boolean clear = true;
                for (int dy = -minDistance; dy <= minDistance && clear; dy++) {
                    for (int dx = -minDistance; dx <= minDistance && clear; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (isValid(nx, ny) &&
                                (grid[ny][nx] == 2 || grid[ny][nx] == 3 ||
                                        grid[ny][nx] == 4 || grid[ny][nx] == 5 ||
                                        grid[ny][nx] == 6)) {
                            clear = false;
                        }
                    }
                }

                if (clear) {
                    return new Position(x, y);
                }
            }
            attempts++;
        }

        // В крайнем случае возвращаем первую свободную клетку
        for (int y = minDistance; y < height - minDistance; y++) {
            for (int x = minDistance; x < width - minDistance; x++) {
                if (grid[y][x] == 0) {
                    return new Position(x, y);
                }
            }
        }

        return null;
    }

    private void createRoom(int[][] grid, int x, int y, int w, int h) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (isValid(nx, ny)) {
                    grid[ny][nx] = 0;
                }
            }
        }
    }

    private void createPath(int[][] grid, int x1, int y1, int x2, int y2) {
        int currentX = x1;
        int currentY = y1;

        while (currentX != x2 || currentY != y2) {
            if (currentX != x2) {
                currentX += (currentX < x2) ? 1 : -1;
            } else {
                currentY += (currentY < y2) ? 1 : -1;
            }

            if (isValid(currentX, currentY)) {
                grid[currentY][currentX] = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = currentX + dx;
                        int ny = currentY + dy;
                        if (isValid(nx, ny) && random.nextDouble() > 0.8) {
                            grid[ny][nx] = 0;
                        }
                    }
                }
            }
        }
    }

    private void createDeadEnd(int[][] grid, int x, int y, int length) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[] dir = directions[random.nextInt(4)];

        for (int i = 1; i <= length; i++) {
            int nx = x + dir[0] * i;
            int ny = y + dir[1] * i;

            if (isValid(nx, ny)) {
                grid[ny][nx] = 1;
                for (int j = -1; j <= 1; j += 2) {
                    int sideX = nx + (dir[1] != 0 ? j : 0);
                    int sideY = ny + (dir[0] != 0 ? j : 0);
                    if (isValid(sideX, sideY)) {
                        grid[sideY][sideX] = 1;
                    }
                }
            }
        }
    }

    private boolean isValid(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private static class Edge implements Comparable<Edge> {
        int x, y, wallX, wallY, weight;

        Edge(int x, int y, int wallX, int wallY, int weight) {
            this.x = x;
            this.y = y;
            this.wallX = wallX;
            this.wallY = wallY;
            this.weight = weight;
        }

        @Override
        public int compareTo(Edge other) {
            return Integer.compare(this.weight, other.weight);
        }
    }
}