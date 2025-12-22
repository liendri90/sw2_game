package ru.itis.dis403.sw2_game.common.message;

import ru.itis.dis403.sw2_game.common.model.MatchResult;

import java.util.List;

/**
 * Сервер отправляет клиенту список матчей.
 */
public class MatchesListMessage implements Message {

    private final List<MatchResult> matches;

    public MatchesListMessage(List<MatchResult> matches) {
        this.matches = matches;
    }

    @Override
    public MessageType getType() {
        return MessageType.MATCHES_LIST;
    }

    public List<MatchResult> getMatches() {
        return matches;
    }

    @Override
    public String toString() {
        return "MatchesListMessage{" +
                "matchesCount=" + (matches == null ? 0 : matches.size()) +
                '}';
    }
}
