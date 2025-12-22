package ru.itis.dis403.sw2_game.common.message;

/**
 * Сообщение от клиента к серверу — действие игрока.
 */
public class InputMessage implements Message {

    public enum InputType {
        FLIP_GRAVITY,   // переворот гравитации
        EXIT_GAME       // выход игрока
    }

    private final InputType inputType;
    private final int playerIndex; // 0 или 1

    public InputMessage(InputType inputType, int playerIndex) {
        this.inputType = inputType;
        this.playerIndex = playerIndex;
    }

    @Override
    public MessageType getType() {
        return MessageType.INPUT;
    }

    public InputType getInputType() {
        return inputType;
    }

    public int getPlayerIndex() {
        return playerIndex;
    }

    @Override
    public String toString() {
        return "InputMessage{" +
                "inputType=" + inputType +
                ", playerIndex=" + playerIndex +
                '}';
    }
}