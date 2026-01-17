package ru.itis.dis403.sw2_game.model;

import java.io.Serializable;
import java.util.*;

public class Maze implements Serializable {
    private static final long serialVersionUID = 1L;
    private int width;
    private int height;
    private int[][] grid;
    private Position start1;
    private Position start2;
    private Position exit;
    private int level;
    private String mazeType;
    private List<Position> teleports;
    private List<Position> fakeExits;
    private List<Position> doors;
    private List<Position> keys;
    private List<Position> traps;
    private Random random;

    public Maze(int width, int height, int level) {
        this.width = width;
        this.height = height;
        this.level = level;
        this.grid = new int[height][width];
        this.teleports = new ArrayList<>();
        this.fakeExits = new ArrayList<>();
        this.doors = new ArrayList<>();
        this.keys = new ArrayList<>();
        this.traps = new ArrayList<>();
        this.random = new Random(level * 1000 + width * height);
        generateMaze();
    }

    private void generateMaze() {
        System.out.println("=== ГЕНЕРАЦИЯ УНИКАЛЬНОГО ЛАБИРИНТА УРОВНЯ " + level + " ===");
        System.out.println("Размер: " + width + "x" + height);

        MazeGenerator generator = new MazeGenerator(width, height, level);
        this.grid = generator.generateMaze();

        String[] mazeTypes = {
                "СПИРАЛЬ", "КРЕСТ", "АЛМАЗ", "КРУГИ", "ТЕЛЕПОРТЫ",
                "МНОГО ВЫХОДОВ", "СЛУЧАЙНЫЙ", "РЕКУРСИВНЫЙ", "КЛЮЧИ", "ФИНАЛЬНЫЙ"
        };

        this.mazeType = (level <= 10) ? mazeTypes[level - 1] : "СЛУЧАЙНЫЙ";

        System.out.println("Тип лабиринта: " + mazeType);

        collectSpecialCells();

        setPositions();

        ensurePathsForAllLevels();

        System.out.println("=== ЛАБИРИНТ СОЗДАН ===");
        printSpecialCellsInfo();
    }

    private void collectSpecialCells() {
        teleports.clear();
        fakeExits.clear();
        doors.clear();
        keys.clear();
        traps.clear();

        int doorCount = 0;
        int keyCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                switch (grid[y][x]) {
                    case 2:
                        teleports.add(new Position(x, y));
                        break;
                    case 3:
                        fakeExits.add(new Position(x, y));
                        break;
                    case 4:
                        doors.add(new Position(x, y));
                        doorCount++;
                        break;
                    case 5:
                        keys.add(new Position(x, y));
                        keyCount++;
                        break;
                    case 6:
                        traps.add(new Position(x, y));
                        break;
                }
            }
        }



        if (doorCount == 0) {
            addEmergencyDoor();
        }
    }

    private void addEmergencyDoor() {
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (grid[y][x] == 0) {
                    grid[y][x] = 4;
                    doors.add(new Position(x, y));
                    return;
                }
            }
        }
    }

    private void printSpecialCellsInfo() {
        System.out.println("- Телепортов: " + teleports.size());
        for (int i = 0; i < teleports.size(); i++) {
            System.out.println("  Телепорт " + (i+1) + " на позиции: " + teleports.get(i));
        }

        System.out.println("- Ложных выходов: " + fakeExits.size());
        for (int i = 0; i < fakeExits.size(); i++) {
            System.out.println("  Ложный выход " + (i+1) + " на позиции: " + fakeExits.get(i));
        }

        System.out.println("- Дверей: " + doors.size());
        for (int i = 0; i < doors.size(); i++) {
            System.out.println("  Дверь " + (i+1) + " на позиции: " + doors.get(i));
        }

        System.out.println("- Ключей: " + keys.size());
        for (int i = 0; i < keys.size(); i++) {
            System.out.println("  Ключ " + (i+1) + " на позиции: " + keys.get(i));
        }

        System.out.println("- Ловушек: " + traps.size());
        for (int i = 0; i < traps.size(); i++) {
            System.out.println("  Ловушка " + (i+1) + " на позиции: " + traps.get(i));
        }

        System.out.println("- Старт 1: " + start1);
        System.out.println("- Старт 2: " + start2);
        System.out.println("- Выход: " + exit);
    }

    private void setPositions() {
        start1 = findSafePosition(true);
        start2 = findSafePosition(false);
        exit = findExitPosition();

        if (start1 != null) grid[start1.getY()][start1.getX()] = 0;
        if (start2 != null) grid[start2.getY()][start2.getX()] = 0;
        if (exit != null) grid[exit.getY()][exit.getX()] = 0;

        System.out.println("Старт 1: " + start1);
        System.out.println("Старт 2: " + start2);
        System.out.println("Выход: " + exit);
    }

    private Position findSafePosition(boolean isFirst) {
        int attempts = 0;
        while (attempts < 100) {
            int x, y;

            if (isFirst) {
                x = 2 + random.nextInt(Math.max(1, width / 4));
                y = 2 + random.nextInt(Math.max(1, height / 4));
            } else {
                x = width - 3 - random.nextInt(Math.max(1, width / 4));
                y = height - 3 - random.nextInt(Math.max(1, height / 4));
            }

            if (isValidPosition(x, y) && !isSpecialCell(x, y) && hasSpaceAround(x, y)) {
                return new Position(x, y);
            }
            attempts++;
        }

        return isFirst ? new Position(1, 1) : new Position(width - 2, height - 2);
    }

    private Position findExitPosition() {
        int attempts = 0;
        while (attempts < 100) {
            int x = width / 4 + random.nextInt(width / 2);
            int y = height / 4 + random.nextInt(height / 2);

            if (start1 != null && start2 != null) {
                double dist1 = distance(x, y, start1.getX(), start1.getY());
                double dist2 = distance(x, y, start2.getX(), start2.getY());
                double minDist = Math.min(width, height) / 3.0;

                if (isValidPosition(x, y) && !isSpecialCell(x, y) &&
                        dist1 > minDist && dist2 > minDist) {
                    return new Position(x, y);
                }
            } else {
                if (isValidPosition(x, y) && !isSpecialCell(x, y)) {
                    return new Position(x, y);
                }
            }
            attempts++;
        }

        return new Position(width / 2, height / 2);
    }

    private void ensurePathsForAllLevels() {
        System.out.println("Проверка путей для уровня " + level + ":");

        boolean path1 = isPathExists(start1, exit);
        boolean path2 = isPathExists(start2, exit);

        System.out.println("Путь от старта 1 к выходу: " + (path1 ? "ЕСТЬ" : "НЕТ"));
        System.out.println("Путь от старта 2 к выходу: " + (path2 ? "ЕСТЬ" : "НЕТ"));

        if (!path1) {
            System.out.println("Создаю путь от старта 1 к выходу");
            createGuaranteedPath(start1, exit);
        }

        if (!path2) {
            System.out.println("Создаю путь от старта 2 к выходу");
            createGuaranteedPath(start2, exit);
        }

        if (level >= 8) {
            System.out.println("Проверка путей к ключам для уровня " + level);

            for (Position key : keys) {
                Position nearestDoor = findNearestDoor(key);
                if (nearestDoor != null && !isPathExists(key, nearestDoor)) {
                    System.out.println("Создаю путь от ключа " + key + " к двери " + nearestDoor);
                    createGuaranteedPath(key, nearestDoor);
                }
            }
        }
    }

    private Position findNearestDoor(Position from) {
        Position nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Position door : doors) {
            double dist = distance(from.getX(), from.getY(), door.getX(), door.getY());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = door;
            }
        }

        return nearest;
    }

    private void createGuaranteedPath(Position from, Position to) {
        if (from == null || to == null) return;

        int currentX = from.getX();
        int currentY = from.getY();

        while (currentX != to.getX() || currentY != to.getY()) {
            if (currentX != to.getX()) {
                currentX += (currentX < to.getX()) ? 1 : -1;
            } else {
                currentY += (currentY < to.getY()) ? 1 : -1;
            }

            if (isValidPosition(currentX, currentY)) {
                grid[currentY][currentX] = 0;
            }
        }
    }

    private boolean isPathExists(Position from, Position to) {
        if (from == null || to == null) {
            return false;
        }

        if (!isValidPosition(from.getX(), from.getY()) || !isValidPosition(to.getX(), to.getY())) {
            return false;
        }

        if (grid[from.getY()][from.getX()] != 0 || grid[to.getY()][to.getX()] != 0) {
            return false;
        }

        boolean[][] visited = new boolean[height][width];
        Queue<Position> queue = new LinkedList<>();
        queue.add(from);
        visited[from.getY()][from.getX()] = true;

        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

        while (!queue.isEmpty()) {
            Position current = queue.poll();

            if (current.equals(to)) {
                return true;
            }

            for (int[] dir : directions) {
                int newX = current.getX() + dir[0];
                int newY = current.getY() + dir[1];

                if (isValidPosition(newX, newY) && grid[newY][newX] == 0 && !visited[newY][newX]) {
                    visited[newY][newX] = true;
                    queue.add(new Position(newX, newY));
                }
            }
        }

        return false;
    }

    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private boolean isSpecialCell(int x, int y) {
        if (!isValidPosition(x, y)) return false;
        int cellValue = grid[y][x];
        return cellValue >= 2 && cellValue <= 6;
    }

    private boolean hasSpaceAround(int x, int y) {
        int freeCells = 0;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (isValidPosition(nx, ny) && grid[ny][nx] == 0 && !isSpecialCell(nx, ny)) {
                    freeCells++;
                }
            }
        }
        return freeCells >= 4;
    }

    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public boolean canMove(Position from, Position to) {
        if (to.getX() < 0 || to.getX() >= width ||
                to.getY() < 0 || to.getY() >= height) {
            return false;
        }

        int cellValue = grid[to.getY()][to.getX()];

        switch (cellValue) {
            case 1:
                return false;
            case 4:
                return true;
            default:
                return true;
        }
    }

    public int getCellValue(Position pos) {
        if (pos.getX() < 0 || pos.getX() >= width ||
                pos.getY() < 0 || pos.getY() >= height) {
            return 1;
        }
        return grid[pos.getY()][pos.getX()];
    }

    public void setCellValue(Position pos, int value) {
        if (pos.getX() >= 0 && pos.getX() < width &&
                pos.getY() >= 0 && pos.getY() < height) {
            grid[pos.getY()][pos.getX()] = value;
        }
    }

    public boolean isExit(Position position) {
        return exit != null && exit.equals(position);
    }

    public boolean isFakeExit(Position position) {
        for (Position fakeExit : fakeExits) {
            if (fakeExit.equals(position)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTeleport(Position position) {
        for (Position teleport : teleports) {
            if (teleport.equals(position)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDoor(Position position) {
        for (Position door : doors) {
            if (door.equals(position)) {
                return true;
            }
        }
        return false;
    }

    public boolean isKey(Position position) {
        for (Position key : keys) {
            if (key.equals(position)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTrap(Position position) {
        for (Position trap : traps) {
            if (trap.equals(position)) {
                return true;
            }
        }
        return false;
    }

    public Position getPairedTeleport(Position teleport) {
        if (teleports.size() < 2) return null;

        int index = teleports.indexOf(teleport);
        if (index == -1) return null;

        if (index % 2 == 0 && index + 1 < teleports.size()) {
            return teleports.get(index + 1);
        } else if (index % 2 == 1) {
            return teleports.get(index - 1);
        }
        return null;
    }

    public void removeKey(Position key) {
        keys.remove(key);
        grid[key.getY()][key.getX()] = 0;
    }

    public void openDoor(Position door) {
        doors.remove(door);
        grid[door.getY()][door.getX()] = 0;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int[][] getGrid() { return grid; }
    public Position getStart1() { return start1; }
    public Position getStart2() { return start2; }
    public Position getExit() { return exit; }
    public int getLevel() { return level; }
    public String getMazeType() { return mazeType; }
    public List<Position> getTeleports() { return teleports; }
    public List<Position> getFakeExits() { return fakeExits; }
    public List<Position> getDoors() { return doors; }
    public List<Position> getKeys() { return keys; }
    public List<Position> getTraps() { return traps; }
}