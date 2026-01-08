package ru.itis.dis403.sw2_game.common.model;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Общее состояние игры, которое рассылается клиентам.
 */
public class GameState implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int PLAYERS_COUNT = 2;

    // Игроки
    private PlayerState[] players;

    // Уровень
    private List<Rectangle> platforms = new ArrayList<>();
    private List<Rectangle> spikes = new ArrayList<>();
    private Rectangle finish;

    // Параметры мира
    private int worldWidth;
    private int worldHeight;

    // Отсчёт и тайминг
    private int countdown;              // текущее значение отсчёта
    private long gameStartTime = -1;    // время старта раунда (ms, server)

    // Состояние матча
    private boolean running;
    private int winnerIndex = -1;
    private String winType = "";
    private long elapsedSeconds = 0;

    public GameState() {
        this.players = new PlayerState[PLAYERS_COUNT];
        this.players[0] = new PlayerState(0, 0, true, "Игрок 1");
        this.players[1] = new PlayerState(0, 0, true, "Игрок 2");
    }

    // ----- PLAYERS -----
    public PlayerState[] getPlayers() {
        return players;
    }

    public void setPlayers(PlayerState[] players) {
        this.players = players;
    }

    // ----- PLATFORMS -----
    public List<Rectangle> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<Rectangle> platforms) {
        this.platforms = platforms;
    }

    public void addPlatform(Rectangle rect) {
        platforms.add(rect);
    }

    // ----- SPIKES -----
    public List<Rectangle> getSpikes() {
        return spikes;
    }

    public void setSpikes(List<Rectangle> spikes) {
        this.spikes = spikes;
    }

    public void addSpike(Rectangle rect) {
        spikes.add(rect);
    }

    // ----- FINISH -----
    public Rectangle getFinish() {
        return finish;
    }

    public void setFinish(Rectangle finish) {
        this.finish = finish;
    }

    // ----- WORLD -----
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

    // ----- COUNTDOWN / TIME -----
    public int getCountdown() {
        return countdown;
    }

    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    public long getGameStartTime() {
        return gameStartTime;
    }

    public void setGameStartTime(long gameStartTime) {
        this.gameStartTime = gameStartTime;
    }

    public boolean isGameStarted() {
        return gameStartTime > 0;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    // ----- MATCH STATE -----
    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

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

    // ----- PlayerState -----
    public static class PlayerState implements Serializable {
        private static final long serialVersionUID = 1L;

        private double x;
        private double y;
        private double vx;
        private double vy;
        private boolean alive;
        private String name;
        private boolean gravityDown = true;

        public PlayerState(double x, double y, boolean alive, String name) {
            this.x = x;
            this.y = y;
            this.alive = alive;
            this.name = name;
        }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        public double getVx() { return vx; }
        public void setVx(double vx) { this.vx = vx; }

        public double getVy() { return vy; }
        public void setVy(double vy) { this.vy = vy; }

        public boolean isAlive() { return alive; }
        public void setAlive(boolean alive) { this.alive = alive; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean isGravityDown() { return gravityDown; }
        public void setGravityDown(boolean gravityDown) { this.gravityDown = gravityDown; }
    }
}
