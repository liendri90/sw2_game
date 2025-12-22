package ru.itis.dis403.sw2_game.common.model;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Состояние игры, синхронизируемое между сервером и клиентом.
 * Содержит всю необходимую информацию о позициях игроков, препятствиях и состоянии игры.
 */
public class GameState implements Serializable {

    private static final long serialVersionUID = 1L;

    // === КОНСТАНТЫ ===
    public static final int PLAYERS_COUNT = 2;

    // === ПОЛЯ ИГРОКОВ ===
    private PlayerState[] players;

    // === УРОВЕНЬ ===
    private List<Rectangle> platforms;      // Платформы на которых можно стоять
    private List<Rectangle> spikes;         // Шипы (смертельные)
    private Rectangle finish;               // Финиш

    // === РАЗМЕРЫ МИРА ===
    private int worldWidth;
    private int worldHeight;

    // === СОСТОЯНИЕ ОТСЧЁТА ===
    private int countdown;                  // Текущее значение отсчёта (0-N)
    private long gameStartTime = -1;        // Серверное время начала игры (для синхронизации)

    // === СОСТОЯНИЕ ИГРЫ ===
    private boolean running = false;        // Игра в процессе?
    private int winnerIndex = -1;           // Индекс победителя (-1 = нет победителя)
    private String winType = "";            // Тип победы (FINISH, LAST_ALIVE, NO_ONE)
    private long elapsedSeconds = 0;        // Сколько секунд прошло с начала игры

    // === КОНСТРУКТОРЫ ===

    /**
     * Конструктор по умолчанию
     */
    public GameState() {
        this.players = new PlayerState[PLAYERS_COUNT];
        this.players[0] = new PlayerState(0, 0, true, "Player1");
        this.players[1] = new PlayerState(0, 0, true, "Player2");
        this.platforms = new ArrayList<>();
        this.spikes = new ArrayList<>();
        this.finish = null;
        this.countdown = 0;
        this.worldWidth = 0;
        this.worldHeight = 0;
        this.gameStartTime = -1;
        this.running = false;
        this.winnerIndex = -1;
        this.winType = "";
        this.elapsedSeconds = 0;
    }

    /**
     * Конструктор с параметрами
     */
    public GameState(PlayerState[] players, List<Rectangle> spikes,
                     Rectangle finish, int countdown, int worldWidth) {
        if (players == null || players.length != PLAYERS_COUNT) {
            this.players = new PlayerState[PLAYERS_COUNT];
            this.players[0] = new PlayerState(0, 0, true, "Player1");
            this.players[1] = new PlayerState(0, 0, true, "Player2");
        } else {
            this.players = players;
        }
        this.platforms = new ArrayList<>();
        this.spikes = spikes != null ? spikes : new ArrayList<>();
        this.finish = finish;
        this.countdown = countdown;
        this.worldWidth = worldWidth;
        this.worldHeight = 0;
        this.gameStartTime = -1;
        this.running = false;
        this.winnerIndex = -1;
        this.winType = "";
        this.elapsedSeconds = 0;
    }

    // === GETTERS И SETTERS ДЛЯ PLAYERS ===

    public PlayerState[] getPlayers() {
        return players;
    }

    public void setPlayers(PlayerState[] players) {
        this.players = players;
    }

    // === GETTERS И SETTERS ДЛЯ PLATFORMS ===

    public List<Rectangle> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<Rectangle> platforms) {
        this.platforms = platforms;
    }

    public void addPlatform(Rectangle platform) {
        if (this.platforms == null) {
            this.platforms = new ArrayList<>();
        }
        this.platforms.add(platform);
    }

    // === GETTERS И SETTERS ДЛЯ SPIKES ===

    public List<Rectangle> getSpikes() {
        return spikes;
    }

    public void setSpikes(List<Rectangle> spikes) {
        this.spikes = spikes;
    }

    public void addSpike(Rectangle spike) {
        if (this.spikes == null) {
            this.spikes = new ArrayList<>();
        }
        this.spikes.add(spike);
    }

    // === GETTERS И SETTERS ДЛЯ FINISH ===

    public Rectangle getFinish() {
        return finish;
    }

    public void setFinish(Rectangle finish) {
        this.finish = finish;
    }

    // === GETTERS И SETTERS ДЛЯ COUNTDOWN ===

    public int getCountdown() {
        return countdown;
    }

    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    // === GETTERS И SETTERS ДЛЯ RUNNING STATE ===

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    // === GETTERS И SETTERS ДЛЯ WORLD DIMENSIONS ===

    public int getWorldWidth() {
        return worldWidth;
    }

    public void setWorldWidth(int worldWidth) {
        this.worldWidth = worldWidth;
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public void setWorldHeight(int worldHeight) {
        this.worldHeight = worldHeight;
    }

    // === GETTERS И SETTERS ДЛЯ GAME START TIME (синхронизация) ===

    /**
     * Получить серверное время начала отсчёта (миллисекунды)
     */
    public long getGameStartTime() {
        return gameStartTime;
    }

    /**
     * Установить серверное время начала отсчёта
     */
    public void setGameStartTime(long gameStartTime) {
        this.gameStartTime = gameStartTime;
    }

    /**
     * Проверить, началась ли игра
     */
    public boolean isGameStarted() {
        return gameStartTime > 0;
    }

    // === GETTERS И SETTERS ДЛЯ WINNER INFO ===

    public int getWinnerIndex() {
        return winnerIndex;
    }

    public void setWinnerIndex(int winnerIndex) {
        this.winnerIndex = winnerIndex;
    }

    public String getWinType() {
        return winType;
    }

    public void setWinType(String winType) {
        this.winType = winType;
    }

    // === GETTERS И SETTERS ДЛЯ ELAPSED TIME ===

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    // === ВНУТРЕННИЙ КЛАСС PLAYERSTATE ===

    /**
     * Состояние одного игрока
     */
    public static class PlayerState implements Serializable {
        private static final long serialVersionUID = 1L;

        // Позиция
        private double x;
        private double y;

        // Скорость
        private double vx = 0;
        private double vy = 0;

        // Состояние
        private boolean alive;
        private String name;

        // Гравитация
        private boolean gravityDown = true;

        /**
         * Конструктор PlayerState
         */
        public PlayerState(double x, double y, boolean alive, String name) {
            this.x = x;
            this.y = y;
            this.alive = alive;
            this.name = name;
            this.gravityDown = true;
            this.vx = 0;
            this.vy = 0;
        }

        // === POSITION ===

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        // === VELOCITY ===

        public double getVx() {
            return vx;
        }

        public void setVx(double vx) {
            this.vx = vx;
        }

        public double getVy() {
            return vy;
        }

        public void setVy(double vy) {
            this.vy = vy;
        }

        // === ALIVE STATE ===

        public boolean isAlive() {
            return alive;
        }

        public void setAlive(boolean alive) {
            this.alive = alive;
        }

        // === NAME ===

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        // === GRAVITY ===

        public boolean isGravityDown() {
            return gravityDown;
        }

        public void setGravityDown(boolean gravityDown) {
            this.gravityDown = gravityDown;
        }
    }
}