package ru.itis.dis403.sw2_game.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO для записи/чтения строки из таблицы matches.
 */
public class MatchResult implements Serializable {

    private final String winnerName;
    private final String winnerHatColor;
    private final LocalDateTime dateTime;
    private final int durationSeconds;
    private final String winType;

    public MatchResult(String winnerName,
                       String winnerHatColor,
                       LocalDateTime dateTime,
                       int durationSeconds,
                       String winType) {
        this.winnerName = winnerName;
        this.winnerHatColor = winnerHatColor;
        this.dateTime = dateTime;
        this.durationSeconds = durationSeconds;
        this.winType = winType;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public String getWinnerHatColor() {
        return winnerHatColor;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public String getWinType() {
        return winType;
    }

    @Override
    public String toString() {
        return "MatchResult{" +
                "winnerName='" + winnerName + '\'' +
                ", winnerHatColor='" + winnerHatColor + '\'' +
                ", dateTime=" + dateTime +
                ", durationSeconds=" + durationSeconds +
                ", winType='" + winType + '\'' +
                '}';
    }
}
