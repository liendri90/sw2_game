package ru.itis.dis403.sw2_game.common.message;

import java.io.Serializable;

/**
 * Базовый тип всех сообщений, передаваемых по сокету.
 */
public interface Message extends Serializable {

    MessageType getType();

    enum MessageType {
        JOIN,             // клиент подключился
        INPUT,            // действие игрока
        STATE,            // состояние игры
        GAME_OVER,        // завершение матча
        MATCHES_REQUEST,  // клиент просит список матчей
        MATCHES_LIST,     // сервер шлёт список матчей
        PING,
        PONG,
        PLAYER_INDEX, READY
    }
}
