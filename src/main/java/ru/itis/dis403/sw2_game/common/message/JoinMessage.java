package ru.itis.dis403.sw2_game.common.message;

/**
 * Сообщение о подключении игрока с его именем и цветом шляпы.
 */
public class JoinMessage implements Message {

    private final String playerName;
    private final String hatColor;

    public JoinMessage(String playerName, String hatColor) {
        this.playerName = playerName;
        this.hatColor = hatColor;
    }

    @Override
    public MessageType getType() {
        return MessageType.JOIN;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getHatColor() {
        return hatColor;
    }

    @Override
    public String toString() {
        return "JoinMessage{" +
                "playerName='" + playerName + '\'' +
                ", hatColor='" + hatColor + '\'' +
                '}';
    }
}