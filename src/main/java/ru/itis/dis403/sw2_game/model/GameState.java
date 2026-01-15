package ru.itis.dis403.sw2_game.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private Maze maze;
    private List<Player> players;
    private boolean gameStarted;
    private boolean gameFinished;
    private long gameStartTime;
    private int currentLevel;
    private int maxLevels = 10;
    private boolean allLevelsCompleted = false;

    public GameState() {
        this.players = new ArrayList<>();
        this.gameStarted = false;
        this.gameFinished = false;
        this.currentLevel = 1;
        this.allLevelsCompleted = false;
        generateMazeForCurrentLevel();
    }

    private void generateMazeForCurrentLevel() {
        int mazeSize = 25;

        this.maze = new Maze(mazeSize, mazeSize, currentLevel);
        System.out.println("Создан лабиринт уровня " + currentLevel +
                " размером " + mazeSize + "x" + mazeSize +
                " тип: " + maze.getMazeType());
    }

    public void addPlayer(Player player) {
        players.add(player);
        if (players.size() == 1) {
            player.setPosition(maze.getStart1());
        } else if (players.size() == 2) {
            player.setPosition(maze.getStart2());
        }
    }

    public void removePlayer(String playerName) {
        players.removeIf(p -> p.getName().equals(playerName));
    }

    public Player getPlayer(String name) {
        return players.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public void startGame() {
        gameStarted = true;
        gameStartTime = System.currentTimeMillis();
        for (Player player : players) {
            player.setStartTime(gameStartTime);
            player.setAtExit(false);
        }
        gameFinished = false;

        System.out.println("=== ИГРА НАЧАТА ===");
        System.out.println("Уровень: " + currentLevel);
        System.out.println("Игроков: " + players.size());
    }

    public void checkWinCondition() {
        boolean allAtExit = players.stream().allMatch(Player::isAtExit);
        if (allAtExit && !gameFinished) {
            gameFinished = true;
            long finishTime = System.currentTimeMillis();
            for (Player player : players) {
                player.setFinishTime(finishTime);
            }

            System.out.println("=== УРОВЕНЬ " + currentLevel + " ПРОЙДЕН ===");
            System.out.println("Время: " + (finishTime - gameStartTime) + "мс");

            if (currentLevel >= maxLevels) {
                allLevelsCompleted = true;
                System.out.println("=== ВСЕ УРОВНИ ПРОЙДЕНЫ! ===");
            }
        }
    }

    public void nextLevel() {
        if (currentLevel < maxLevels) {
            currentLevel++;
            generateMazeForCurrentLevel();

            gameStarted = false;
            gameFinished = false;
            gameStartTime = 0;

            for (Player player : players) {
                player.setAtExit(false);
                player.setStartTime(0);
                player.setFinishTime(0);
                player.resetSpecialStates();
                player.setKeys(0);

                if (players.indexOf(player) == 0) {
                    player.setPosition(maze.getStart1());
                } else if (players.indexOf(player) == 1) {
                    player.setPosition(maze.getStart2());
                }
            }

            System.out.println("=== ПЕРЕХОД НА УРОВЕНЬ " + currentLevel + " ===");
            System.out.println("Состояние игроков сброшено (готовность сохраняется)");

            if (players.size() == 2) {
                for (Player player : players) {
                    player.setReady(true);
                }
                System.out.println("Все игроки автоматически помечены как готовые");
            }
        } else {
            allLevelsCompleted = true;
            System.out.println("=== ИГРА ПРОЙДЕНА! ===");
        }
    }

    public GameState createCopy() {
        GameState copy = new GameState();

        copy.gameStarted = this.gameStarted;
        copy.gameFinished = this.gameFinished;
        copy.currentLevel = this.currentLevel;
        copy.gameStartTime = this.gameStartTime;
        copy.maxLevels = this.maxLevels;
        copy.allLevelsCompleted = this.allLevelsCompleted;

        copy.maze = this.maze;

        copy.players = new ArrayList<>();
        for (Player original : this.players) {
            Player playerCopy = new Player(
                    original.getName(),
                    new Position(original.getPosition().getX(), original.getPosition().getY()),
                    original.getColor()
            );
            playerCopy.setReady(original.isReady());
            playerCopy.setAtExit(original.isAtExit());
            playerCopy.setStartTime(original.getStartTime());
            playerCopy.setFinishTime(original.getFinishTime());
            playerCopy.setKeys(original.getKeys());
            playerCopy.setHasTeleported(original.hasTeleported());
            playerCopy.setTrapActive(original.isTrapActive());

            copy.players.add(playerCopy);
        }

        return copy;
    }

    public Maze getMaze() { return maze; }
    public List<Player> getPlayers() { return players; }
    public boolean isGameStarted() { return gameStarted; }
    public boolean isGameFinished() { return gameFinished; }
    public long getGameStartTime() { return gameStartTime; }
    public int getCurrentLevel() { return currentLevel; }
    public int getMaxLevels() { return maxLevels; }
    public boolean isAllLevelsCompleted() { return allLevelsCompleted; }

    public void setGameStarted(boolean gameStarted) { this.gameStarted = gameStarted; }
    public void setGameFinished(boolean gameFinished) { this.gameFinished = gameFinished; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
    public void setAllLevelsCompleted(boolean allLevelsCompleted) {
        this.allLevelsCompleted = allLevelsCompleted;
    }

    @Override
    public String toString() {
        return "GameState{" +
                "level=" + currentLevel + "/" + maxLevels +
                ", players=" + players.size() +
                ", gameStarted=" + gameStarted +
                ", gameFinished=" + gameFinished +
                ", mazeSize=" + maze.getWidth() + "x" + maze.getHeight() +
                ", allCompleted=" + allLevelsCompleted +
                '}';
    }
}