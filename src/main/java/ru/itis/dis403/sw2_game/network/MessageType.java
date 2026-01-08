package ru.itis.dis403.sw2_game.network;

public enum MessageType {
    PLAYER_JOIN,
    PLAYER_MOVE,
    READY_STATE,
    LEVEL_COMPLETE,
    NEW_LEVEL,
    GAME_COMPLETE,
    GAME_STATE,
    PLAYER_LEFT,
    GAME_STARTED,
    ERROR,
    INFO,
    REQUEST_GAME_STATE,
    NEXT_LEVEL,
    ROOM_INFO,
    LEADERBOARD_DATA,
    CREATE_ROOM,
    ROOM_CREATED,
    REQUEST_ROOMS // Новый тип сообщения для запроса списка комнат
}