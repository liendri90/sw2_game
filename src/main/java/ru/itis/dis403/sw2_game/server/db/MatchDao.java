package ru.itis.dis403.sw2_game.server.db;

import ru.itis.dis403.sw2_game.common.model.MatchResult;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Простая DAO для таблицы matches.
 *
 * CREATE TABLE matches (
 *   id SERIAL PRIMARY KEY,
 *   winner_name       VARCHAR(50),
 *   winner_hat_color  VARCHAR(20),
 *   match_date_time   TIMESTAMP,
 *   duration_seconds  INTEGER,
 *   win_type          VARCHAR(20)
 * );
 */
public class MatchDao {

    private final String url;
    private final String user;
    private final String password;

    public MatchDao(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void insertMatch(MatchResult result) throws SQLException {
        String sql = "INSERT INTO matches " +
                "(winner_name, winner_hat_color, match_date_time, duration_seconds, win_type) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, result.getWinnerName());
            ps.setString(2, result.getWinnerHatColor());
            ps.setTimestamp(3, Timestamp.valueOf(result.getDateTime()));
            ps.setInt(4, result.getDurationSeconds());
            ps.setString(5, result.getWinType());
            ps.executeUpdate();
        }
    }

    public List<MatchResult> findAll() throws SQLException {
        List<MatchResult> list = new ArrayList<>();
        String sql = "SELECT winner_name, winner_hat_color, " +
                "match_date_time, duration_seconds, win_type " +
                "FROM matches ORDER BY match_date_time DESC";

        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("winner_name");
                String color = rs.getString("winner_hat_color");
                LocalDateTime dt = rs.getTimestamp("match_date_time").toLocalDateTime();
                int dur = rs.getInt("duration_seconds");
                String winType = rs.getString("win_type");

                list.add(new MatchResult(name, color, dt, dur, winType));
            }
        }
        return list;
    }
}
