package ru.itis.dis403.sw2_game.db;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BestTimeRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String roomName; // Название комнаты
    private long totalTime; // Общее время прохождения всех уровней
    private LocalDateTime createdAt; // Дата прохождения
    private int levelsCompleted; // Сколько уровней пройдено

    public BestTimeRecord(int id, String roomName, long totalTime,
                          LocalDateTime createdAt, int levelsCompleted) {
        this.id = id;
        this.roomName = roomName;
        this.totalTime = totalTime;
        this.createdAt = createdAt;
        this.levelsCompleted = levelsCompleted;
    }

    // Геттеры
    public int getId() { return id; }
    public String getRoomName() { return roomName; }
    public long getTotalTime() { return totalTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getLevelsCompleted() { return levelsCompleted; }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        String formattedDate = createdAt.format(formatter);

        return String.format("Комната: %s - %d уровней за %d мс (%s)",
                roomName, levelsCompleted, totalTime, formattedDate);
    }
}