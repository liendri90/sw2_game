package ru.itis.dis403.sw2_game.common.message;

/**
 * Клиент просит у сервера список матчей (для окна "Матчи").
 */
public class MatchesRequestMessage implements Message {

    @Override
    public MessageType getType() {
        return MessageType.MATCHES_REQUEST;
    }

    @Override
    public String toString() {
        return "MatchesRequestMessage{}";
    }
}
