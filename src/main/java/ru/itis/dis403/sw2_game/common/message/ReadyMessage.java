package ru.itis.dis403.sw2_game.common.message;

public class ReadyMessage implements Message {

    @Override
    public MessageType getType() {
        return MessageType.READY;
    }

    @Override
    public String toString() {
        return "ReadyMessage{}";
    }
}
