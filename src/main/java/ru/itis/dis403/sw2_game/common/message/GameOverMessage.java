package ru.itis.dis403.sw2_game.common.message;

import java.time.LocalDateTime;

/**
 * Сообщение от сервера клиентам о завершении матча.
 */
public class GameOverMessage implements Message {

    private final int winnerIndex;        // -1 если ничья
    private final String winnerName;
    private final String winnerHatColor;
    private final int durationSeconds;
    private final String winType;         // "FINISH", "LAST_ALIVE"
    private final LocalDateTime dateTime;

    public GameOverMessage(int winnerIndex,
                           String winnerName,
                           String winnerHatColor,
                           int durationSeconds,
                           String winType,
                           LocalDateTime dateTime) {
        this.winnerIndex = winnerIndex;
        this.winnerName = winnerName;
        this.winnerHatColor = winnerHatColor;
        this.durationSeconds = durationSeconds;
        this.winType = winType;
        this.dateTime = dateTime;
    }

    @Override
    public MessageType getType() {
        return MessageType.GAME_OVER;
    }

    public int getWinnerIndex() {
        return winnerIndex;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public String getWinnerHatColor() {
        return winnerHatColor;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public String getWinType() {
        return winType;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    @Override
    public String toString() {
        return "GameOverMessage{" +
                "winnerIndex=" + winnerIndex +
                ", winnerName='" + winnerName + '\'' +
                ", winnerHatColor='" + winnerHatColor + '\'' +
                ", durationSeconds=" + durationSeconds +
                ", winType='" + winType + '\'' +
                ", dateTime=" + dateTime +
                '}';
    }
}