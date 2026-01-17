package ru.itis.dis403.sw2_game.model;

import java.awt.Color;
import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private Position position;
    private Color color;
    private boolean ready;
    private boolean atExit;
    private long startTime;
    private long finishTime;
    private int keys;
    private boolean hasTeleported;
    private boolean trapActive;

    public Player(String name, Position position, Color color) {
        this.name = name;
        this.position = position;
        this.color = color;
        this.ready = false;
        this.atExit = false;
        this.startTime = 0;
        this.finishTime = 0;
        this.keys = 0;
        this.hasTeleported = false;
        this.trapActive = false;
    }

    public String getName() { return name; }
    public Position getPosition() { return position; }
    public Color getColor() { return color; }
    public boolean isReady() { return ready; }
    public boolean isAtExit() { return atExit; }
    public long getStartTime() { return startTime; }
    public long getFinishTime() { return finishTime; }
    public int getKeys() { return keys; }
    public boolean hasTeleported() { return hasTeleported; }
    public boolean isTrapActive() { return trapActive; }

    public void setPosition(Position position) { this.position = position; }
    public void setReady(boolean ready) { this.ready = ready; }
    public void setAtExit(boolean atExit) { this.atExit = atExit; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setFinishTime(long finishTime) { this.finishTime = finishTime; }
    public void setHasTeleported(boolean hasTeleported) { this.hasTeleported = hasTeleported; }
    public void setTrapActive(boolean trapActive) { this.trapActive = trapActive; }

    public void addKey() { this.keys++; }
    public boolean useKey() {
        if (this.keys > 0) {
            this.keys--;
            return true;
        }
        return false;
    }
    public boolean hasKey() { return this.keys > 0; }

    public void resetSpecialStates() {
        this.hasTeleported = false;
        this.trapActive = false;
    }


    public long getLevelTimeElapsed() {
        if (startTime == 0) return 0;
        if (finishTime > 0) {
            return finishTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    @Override
    public String toString() {
        return name + " [ключей: " + keys + ", готов: " + ready + "]";
    }

    public void setKeys(int keys) { this.keys = keys; }

    public int[] getKeyArray() {
        return new int[]{keys};
    }
}