package ru.itis.dis403.sw2_game.common.message;

public class PlayerIndexMessage implements Message {
    private final int playerIndex;

    public PlayerIndexMessage(int playerIndex) {
        this.playerIndex = playerIndex;
    }

    @Override
    public MessageType getType() {
        return MessageType.PLAYER_INDEX;
    }

    public int getPlayerIndex() {
        return playerIndex;
    }
}
