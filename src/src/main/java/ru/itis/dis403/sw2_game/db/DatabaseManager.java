package ru.itis.dis403.sw2_game.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private Connection connection;

    private String url = "jdbc:postgresql://localhost:5432/best_times";
    private String user = "postgres";
    private String password = "safiullina90";

    public DatabaseManager() {
        try {
            connect();
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Не удалось подключиться к БД: " + e.getMessage());
        }
    }

    public void connect() throws SQLException, ClassNotFoundException {
        if (connection == null || connection.isClosed()) {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Подключено к базе данных");

            createTables();
        }
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS room_stats (" +
                "id SERIAL PRIMARY KEY," +
                "room_name VARCHAR(100) NOT NULL UNIQUE," +
                "total_time BIGINT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "levels_completed INT DEFAULT 0" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Таблица room_stats создана/проверена");
        } catch (SQLException e) {
            System.err.println("Ошибка при создании таблицы: " + e.getMessage());
        }
    }

    public void saveRoomResult(String roomName, long totalTime, int levelsCompleted) {
        String checkSql = "SELECT COUNT(*) FROM room_stats WHERE room_name = ?";
        String insertSql = "INSERT INTO room_stats (room_name, total_time, levels_completed) VALUES (?, ?, ?)";
        String updateSql = "UPDATE room_stats SET total_time = ?, levels_completed = ?, created_at = CURRENT_TIMESTAMP WHERE room_name = ?";

        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, roomName);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                String selectTimeSql = "SELECT total_time FROM room_stats WHERE room_name = ?";
                try (PreparedStatement selectStmt = connection.prepareStatement(selectTimeSql)) {
                    selectStmt.setString(1, roomName);
                    ResultSet timeRs = selectStmt.executeQuery();
                    if (timeRs.next()) {
                        long existingTime = timeRs.getLong("total_time");
                        if (totalTime < existingTime) {
                            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                                updateStmt.setLong(1, totalTime);
                                updateStmt.setInt(2, levelsCompleted);
                                updateStmt.setString(3, roomName);
                                updateStmt.executeUpdate();
                                System.out.println("Обновлен рекорд для комнаты " + roomName + ": " + totalTime + "мс");
                            }
                        } else {
                            System.out.println("Существующий рекорд лучше для комнаты " + roomName);
                        }
                    }
                }
            } else {
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, roomName);
                    insertStmt.setLong(2, totalTime);
                    insertStmt.setInt(3, levelsCompleted);
                    insertStmt.executeUpdate();
                    System.out.println("Создан новый рекорд для комнаты " + roomName);
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при сохранении статистики комнаты: " + e.getMessage());
        }
    }

    public List<BestTimeRecord> getRoomLeaderboard() {
        List<BestTimeRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM room_stats ORDER BY total_time ASC, levels_completed DESC LIMIT 10";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                records.add(new BestTimeRecord(
                        rs.getInt("id"),
                        rs.getString("room_name"),
                        rs.getLong("total_time"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getInt("levels_completed")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при получении лидерборда комнат: " + e.getMessage());
        }

        return records;
    }

    public List<BestTimeRecord> findAll() {
        return getRoomLeaderboard();
    }

    public void clearAllRecords() throws SQLException {
        String sql = "DELETE FROM room_stats";

        try (Statement stmt = connection.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            System.out.println("Удалено " + deleted + " записей из таблицы room_stats");
        } catch (SQLException e) {
            System.err.println("Ошибка при очистке записей: " + e.getMessage());
            throw e;
        }
    }

    public void cleanupOldRecords(int daysToKeep) {
        String sql = "DELETE FROM room_stats WHERE created_at < NOW() - INTERVAL ? DAY";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, daysToKeep);
            int deleted = stmt.executeUpdate();
            System.out.println("Удалено " + deleted + " старых записей");
        } catch (SQLException e) {
            System.err.println("Ошибка при очистке старых записей: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Ошибка при отключении от БД: " + e.getMessage());
            }
        }
    }
}