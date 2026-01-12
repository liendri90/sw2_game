package ru.itis.dis403.sw2_game.ui;

import ru.itis.dis403.sw2_game.db.BestTimeRecord;
import ru.itis.dis403.sw2_game.db.DatabaseManager;
import ru.itis.dis403.sw2_game.model.GameState;
import ru.itis.dis403.sw2_game.model.Player;
import ru.itis.dis403.sw2_game.model.Position;
import ru.itis.dis403.sw2_game.network.GameClient;
import ru.itis.dis403.sw2_game.network.Message;
import ru.itis.dis403.sw2_game.network.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.awt.event.*;
import java.time.format.*;

public class GamePanel extends JFrame {
    private static final int CELL_SIZE = 25;
    private static final int REFRESH_RATE_MS = 100;
    private static final int NEXT_LEVEL_DELAY_MS = 3000;

    private final GameClient client;
    private final DatabaseManager dbManager;
    private GameState gameState;
    private MazeCanvas mazeCanvas;

    private JLabel roomLabel;
    private JLabel levelLabel;
    private JLabel statusLabel;
    private JLabel timeLabel;
    private JButton readyButton;
    private JButton nextLevelButton;
    private JButton backToMenuButton;

    private boolean isReady;
    private ScheduledExecutorService scheduler;
    private String currentRoom;
    private String roomDescription;
    private long roomTotalTime;
    private final Map<Integer, Long> levelTimes;
    private boolean showingLevelResults;
    private boolean showingFinalResults;
    private Timer nextLevelTimer;
    private boolean gameCompleted = false;

    public GamePanel(GameClient client, DatabaseManager dbManager,
                     String roomName, String roomDescription) {
        this.client = client;
        this.dbManager = dbManager;
        this.gameState = null;
        this.isReady = false;
        this.currentRoom = roomName;
        this.roomDescription = roomDescription;
        this.roomTotalTime = 0;
        this.levelTimes = new HashMap<>();
        this.showingLevelResults = false;
        this.showingFinalResults = false;
        this.gameCompleted = false;

        initializeWindow();
        initUI();
        startMessageListener();

        SwingUtilities.invokeLater(() -> {
            client.sendMessage(new Message(MessageType.REQUEST_GAME_STATE, client.getPlayerName()));
        });
    }

    private void initializeWindow() {
        setTitle("Maze Escape - " + client.getPlayerName() + " | " + currentRoom);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    private void initUI() {
        add(createInfoPanel(), BorderLayout.NORTH);

        mazeCanvas = new MazeCanvas();
        mazeCanvas.setPreferredSize(new Dimension(900, 650));
        JScrollPane scrollPane = new JScrollPane(mazeCanvas);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(Color.LIGHT_GRAY);
        add(scrollPane, BorderLayout.CENTER);

        add(createControlPanel(), BorderLayout.SOUTH);

        setupKeyBindings();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::updateTime, 0, REFRESH_RATE_MS, TimeUnit.MILLISECONDS);
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(240, 240, 240));

        roomLabel = new JLabel("Комната: " + currentRoom, SwingConstants.CENTER);
        roomLabel.setFont(new Font("Arial", Font.BOLD, 16));
        roomLabel.setForeground(new Color(70, 130, 180));

        levelLabel = new JLabel("Уровень: 1/10", SwingConstants.CENTER);
        levelLabel.setFont(new Font("Arial", Font.BOLD, 14));
        levelLabel.setForeground(new Color(0, 100, 0));

        statusLabel = new JLabel("Ожидание игроков...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.RED);

        timeLabel = new JLabel("Время уровня: 0 | Общее время: 0", SwingConstants.CENTER);
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        timeLabel.setForeground(Color.BLUE);

        panel.add(roomLabel);
        panel.add(levelLabel);
        panel.add(statusLabel);
        panel.add(timeLabel);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(240, 240, 240));

        JButton helpButton = createStyledButton("Справка", new Color(100, 149, 237));
        helpButton.addActionListener(e -> showLevelHelp());

        readyButton = createStyledButton("Готов", new Color(60, 179, 113));
        readyButton.addActionListener(e -> toggleReady());

        nextLevelButton = createStyledButton("Следующий уровень", new Color(70, 130, 180));
        nextLevelButton.setEnabled(false);
        nextLevelButton.addActionListener(e -> requestNextLevel());

        backToMenuButton = createStyledButton("В меню", new Color(220, 20, 60));
        backToMenuButton.addActionListener(e -> returnToMenu());

        panel.add(helpButton);
        panel.add(readyButton);
        panel.add(nextLevelButton);
        panel.add(backToMenuButton);

        return panel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(color.darker());
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(color);
            }
        });
        return button;
    }


    private void startMessageListener() {
        Thread listenerThread = new Thread(() -> {
            while (client.isConnected() && !gameCompleted) {
                try {
                    Message message = client.receiveMessage();
                    if (message != null) {
                        System.out.println("Получено сообщение от сервера: " + message.getType());
                        processServerMessage(message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void processServerMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Обработка сообщения: " + message.getType());

            switch (message.getType()) {
                case ROOM_INFO:
                    handleRoomInfo(message);
                    break;

                case PLAYER_JOIN:
                    statusLabel.setText("Игрок присоединился: " + message.getData());
                    break;

                case PLAYER_LEFT:
                    statusLabel.setText("Игрок вышел: " + message.getData());
                    break;

                case GAME_STATE:
                case GAME_STARTED:
                case NEW_LEVEL:
                    handleGameStateUpdate(message);
                    break;

                case LEVEL_COMPLETE:
                    handleLevelComplete(message);
                    break;

                case GAME_COMPLETE:
                    System.out.println("=== ПОЛУЧЕНО СООБЩЕНИЕ GAME_COMPLETE ===");
                    System.out.println("Данные: " + message.getData());
                    handleGameComplete(message);
                    break;

                case INFO:
                    statusLabel.setText(message.getData());
                    break;

                case ERROR:
                    JOptionPane.showMessageDialog(this,
                            message.getData(),
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                    break;

                default:
                    System.out.println("Необработанный тип сообщения: " + message.getType());
            }
        });
    }

    private void handleRoomInfo(Message message) {
        if (message.getData() != null) {
            String[] roomData = message.getData().split("\\|");
            if (roomData.length >= 1) {
                currentRoom = roomData[0];
                roomLabel.setText("Комната: " + currentRoom);
                setTitle("Maze Escape - " + client.getPlayerName() + " | " + currentRoom);
            }
        }
    }

    private void handleGameStateUpdate(Message message) {
        if (gameCompleted || showingFinalResults) {
            return;
        }

        if (message.getPayload() instanceof GameState) {
            GameState newState = (GameState) message.getPayload();
            updateGameState(newState);

            if (message.getType() == MessageType.NEW_LEVEL) {
                levelLabel.setText("Уровень: " + newState.getCurrentLevel() + "/10");
                if (!newState.isGameStarted()) {
                    statusLabel.setText("Начинается уровень " + newState.getCurrentLevel() + "...");
                }
            }
        }
    }

    private void handleLevelComplete(Message message) {
        if (gameCompleted || showingFinalResults) {
            return;
        }

        if (showingLevelResults) return;

        showingLevelResults = true;
        String data = message.getData();
        System.out.println("Получены результаты уровня: " + data);

        if (data != null) {
            String[] parts = data.split("\\|");
            if (parts.length >= 3) {
                try {
                    long levelTime = Long.parseLong(parts[0]);
                    roomTotalTime = Long.parseLong(parts[1]);
                    int completedLevels = Integer.parseInt(parts[2]);

                    int currentLevel = gameState.getCurrentLevel();
                    levelTimes.put(currentLevel, levelTime);

                    if (currentLevel < 10) {
                        showLevelCompleteDialog(currentLevel, levelTime);
                    } else {
                        statusLabel.setText("Уровень 10 пройден! Подсчет результатов...");
                        timeLabel.setText("Общее время: " + formatTime(roomTotalTime));
                    }

                    if (currentLevel < 10) {
                        startNextLevelTimer();
                    }

                } catch (NumberFormatException e) {
                    System.err.println("Ошибка парсинга данных уровня: " + e.getMessage());
                }
            }
        }
    }

    private void handleGameComplete(Message message) {
        if (gameCompleted || showingFinalResults) {
            System.out.println("Игра уже завершена, игнорируем повторное сообщение");
            return;
        }

        gameCompleted = true;
        showingFinalResults = true;

        System.out.println("=== НАЧИНАЕМ ОБРАБОТКУ ЗАВЕРШЕНИЯ ИГРЫ ===");

        // Останавливаем таймеры
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (nextLevelTimer != null) {
            nextLevelTimer.stop();
        }

        String data = message.getData();
        System.out.println("Данные завершения: " + data);

        long totalTime = 0;
        int levelsCompleted = 10;

        if (data != null) {
            String[] parts = data.split("\\|");
            if (parts.length >= 3) {
                try {
                    totalTime = Long.parseLong(parts[0]);
                    levelsCompleted = Integer.parseInt(parts[1]);
                    currentRoom = parts[2];

                    roomTotalTime = totalTime;

                    System.out.println("Парсинг успешен:");
                    System.out.println("- Время: " + totalTime + " мс");
                    System.out.println("- Уровней: " + levelsCompleted);
                    System.out.println("- Комната: " + currentRoom);
                } catch (NumberFormatException e) {
                    System.err.println("Ошибка парсинга финальных данных: " + e.getMessage());
                }
            }
        }

        // Обновляем интерфейс
        long finalTotalTime = totalTime;
        SwingUtilities.invokeLater(() -> {
            roomLabel.setText("Комната: " + currentRoom + " - ЗАВЕРШЕНО");
            levelLabel.setText("Все 10 уровней пройдены!");
            statusLabel.setText("Игра завершена - поздравляем!");
            timeLabel.setText("Финальное время: " + formatTime(finalTotalTime));

            readyButton.setEnabled(false);
            readyButton.setText("Игра завершена");
            readyButton.setBackground(Color.GRAY);

            nextLevelButton.setEnabled(false);
            nextLevelButton.setText("Все уровни пройдены");
            nextLevelButton.setBackground(Color.GRAY);

            backToMenuButton.setBackground(new Color(0, 150, 0));
            backToMenuButton.setForeground(Color.WHITE);
            backToMenuButton.setFont(new Font("Arial", Font.BOLD, 12));

            mazeCanvas.repaint();
        });

        // Получаем таблицу лидеров
        Object payload = message.getPayload();
        List<BestTimeRecord> leaderboard = null;

        if (payload instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<BestTimeRecord> tempList = (List<BestTimeRecord>) payload;
                leaderboard = tempList;
                System.out.println("Таблица лидеров получена: " +
                        (leaderboard != null ? leaderboard.size() + " записей" : "null"));
            } catch (ClassCastException e) {
                System.err.println("Ошибка приведения типа для таблицы лидеров: " + e.getMessage());
            }
        }

        // Показываем диалог с результатами
        long finalTotalTime1 = totalTime;
        int finalLevelsCompleted = levelsCompleted;
        List<BestTimeRecord> finalLeaderboard = leaderboard;
        SwingUtilities.invokeLater(() -> {
            showFinalResultsDialog(finalTotalTime1, finalLevelsCompleted, finalLeaderboard);
        });
    }


    private void showLevelCompleteDialog(int level, long levelTime) {
        String message = String.format(
                "<html><div style='width: 400px; text-align: center;'>" +
                        "<h3>Уровень %d пройден!</h3>" +
                        "<hr>" +
                        "<p><b>Время уровня:</b> <font color='blue'>%s</font></p>" +
                        "<p><b>Общее время:</b> <font color='green'>%s</font></p>" +
                        "<br>" +
                        "<p>Следующий уровень начнется через 3 секунды...</p>" +
                        "</div></html>",
                level,
                formatTime(levelTime),
                formatTime(roomTotalTime));

        JOptionPane.showMessageDialog(this,
                message,
                "Уровень " + level + " завершен",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showFinalResultsDialog(long totalTime, int levelsCompleted, List<BestTimeRecord> leaderboard) {
        System.out.println("Показываю финальный диалог результатов...");

        // диалоговое окно
        JDialog dialog = new JDialog(this, "Результаты игры - ПОЗДРАВЛЯЕМ!", true);
        dialog.setSize(800, 700);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // HTML-сообщение
        StringBuilder html = new StringBuilder();
        html.append("<html><div style='width: 750px; padding: 10px;'>");

        // Заголовок с иконкой
        html.append("<div style='text-align: center; background-color: #2E8B57; padding: 15px; border-radius: 10px;'>");
        html.append("<h1 style='color: white; margin: 0;'> ПОЗДРАВЛЯЕМ! </h1>");
        html.append("<h3 style='color: white; margin: 5px 0 0 0;'>Комната '").append(currentRoom).append("' прошла все 10 уровней!</h3>");
        html.append("</div>");

        html.append("<br>");

        // Результаты текущей комнаты
        html.append("<div style='background-color: #f0f8ff; padding: 15px; border-radius: 10px; border: 2px solid #4682b4;'>");
        html.append("<h3 style='color: #2E8B57; text-align: center;'>Ваши результаты:</h3>");
        html.append("<table border='0' cellspacing='10' style='margin: 0 auto; font-size: 14px;'>");
        html.append("<tr><td align='right'><b>Название комнаты:</b></td><td align='left'><b>").append(currentRoom).append("</b></td></tr>");
        html.append("<tr><td align='right'><b>Уровней пройдено:</b></td><td align='left'><font color='blue' size='+1'>").append(levelsCompleted).append("/10</font></td></tr>");
        html.append("<tr><td align='right'><b>Общее время:</b></td><td align='left'><font color='green' size='+2'><b>").append(formatTime(totalTime)).append("</b></font></td></tr>");
        html.append("</table>");
        html.append("</div>");

        // Таблица рекордов
        if (leaderboard != null && !leaderboard.isEmpty()) {
            html.append("<br><hr><br>");
            html.append("<h3 style='text-align: center; color: #2E8B57;'> ТАБЛИЦА РЕКОРДОВ КОМНАТ </h3>");
            html.append("<table border='1' cellpadding='8' style='margin: 0 auto; border-collapse: collapse; width: 95%; background-color: white;'>");
            html.append("<tr style='background-color: #2E8B57; color: white; font-weight: bold;'>");
            html.append("<th>Место</th><th>Название комнаты</th><th>Уровней</th><th>Общее время</th><th>Дата</th>");
            html.append("</tr>");

            int position = 1;
            for (BestTimeRecord record : leaderboard) {
                boolean isCurrentRoom = record.getRoomName().equals(currentRoom);
                String rowStyle = isCurrentRoom ? "style='background-color: #e6ffe6; font-weight: bold; border: 2px solid #32CD32;'" : "";

                html.append("<tr ").append(rowStyle).append(">");
                html.append("<td align='center'>").append(position).append("</td>");
                html.append("<td align='left'>").append(record.getRoomName());
                if (isCurrentRoom) html.append(" <span style='color: green;'>★ (ВАША) ★</span>");
                html.append("</td>");
                html.append("<td align='center'>").append(record.getLevelsCompleted()).append("/10</td>");
                html.append("<td align='center'>").append(formatTime(record.getTotalTime())).append("</td>");
                html.append("<td align='center'>").append(
                                record.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")))
                        .append("</td>");
                html.append("</tr>");
                position++;
                if (position > 10) break;
            }
            html.append("</table>");

            // Проверяем, попала ли текущая комната в топ-10
            boolean inTop10 = leaderboard.stream()
                    .limit(10)
                    .anyMatch(r -> r.getRoomName().equals(currentRoom));

            if (!inTop10) {
                html.append("<br><div style='text-align: center; color: #666; font-style: italic;'>");
                html.append("Ваша комната не вошла в топ-10, но результат сохранен!");
                html.append("</div>");
            }
        } else {
            html.append("<br><hr><br>");
            html.append("<div style='text-align: center; padding: 20px; background-color: #fffacd; border-radius: 10px;'>");
            html.append("<h3 style='color: #d2691e;'> ВЫ ПЕРВЫЕ! </h3>");
            html.append("<p style='font-size: 14px;'>");
            html.append("Вы первые, кто прошел все 10 уровней!<br>");
            html.append("Ваш результат будет отображаться в таблице рекордов для будущих игроков.");
            html.append("</p>");
            html.append("</div>");
        }

        html.append("<br><div style='text-align: center; color: #4682b4; font-size: 12px;'>");
        html.append("Результаты сохранены в таблицу рекордов. Вы можете просмотреть полную таблицу через главное меню.");
        html.append("</div>");

        html.append("</div></html>");

        // Текстовое поле с HTML
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText(html.toString());
        textPane.setEditable(false);
        textPane.setCaretPosition(0);
        textPane.setBackground(new Color(245, 245, 245));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        // Кнопка для просмотра полной таблицы
        if (leaderboard != null && !leaderboard.isEmpty()) {
            JButton fullTableButton = createDialogButton("Просмотр полной таблицы", new Color(60, 179, 113));
            fullTableButton.addActionListener(e -> {
                dialog.dispose();
                showFullLeaderboard(leaderboard);
            });
            buttonPanel.add(fullTableButton);
        }

        // Кнопка возврата в меню
        JButton menuButton = createDialogButton("В главное меню", new Color(70, 130, 180));
        menuButton.addActionListener(e -> {
            dialog.dispose();
            returnToMenu();
        });

        // Кнопка закрытия
        JButton closeButton = createDialogButton("Закрыть", new Color(100, 100, 100));
        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(menuButton);
        buttonPanel.add(closeButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Добавляем обработчик закрытия окна
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.out.println("Диалог результатов закрыт");
            }
        });

        // Показываем диалог
        System.out.println("Показываю диалог результатов");
        dialog.setVisible(true);
    }
    private JButton createDialogButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 40));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(color.darker());
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(color);
            }
        });
        return button;
    }

    private void showFullLeaderboard(List<BestTimeRecord> leaderboard) {
        BestTimesDialog dialog = new BestTimesDialog(this, dbManager, leaderboard, currentRoom);
        dialog.setVisible(true);
    }


    private void setupKeyBindings() {
        InputMap inputMap = mazeCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = mazeCanvas.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "moveUp");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "moveDown");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "moveLeft");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "moveRight");

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "moveUp");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "moveDown");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "moveLeft");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "moveRight");

        actionMap.put("moveUp", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { movePlayer(0, -1); }
        });
        actionMap.put("moveDown", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { movePlayer(0, 1); }
        });
        actionMap.put("moveLeft", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { movePlayer(-1, 0); }
        });
        actionMap.put("moveRight", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { movePlayer(1, 0); }
        });
    }

    private void movePlayer(int dx, int dy) {
        if (gameState == null || !gameState.isGameStarted() || gameState.isGameFinished() || gameCompleted) {
            return;
        }

        Player player = gameState.getPlayer(client.getPlayerName());
        if (player == null) return;

        Position current = player.getPosition();
        Position newPos = new Position(current.getX() + dx, current.getY() + dy);

        client.sendMessage(new Message(MessageType.PLAYER_MOVE,
                client.getPlayerName(),
                newPos.getX() + "," + newPos.getY()));
    }

    private void toggleReady() {
        if (gameState != null && (gameState.isGameFinished() || gameCompleted)) {
            return;
        }

        isReady = !isReady;
        readyButton.setText(isReady ? "Не готов" : "Готов");
        readyButton.setBackground(isReady ? new Color(220, 20, 60) : new Color(60, 179, 113));

        client.sendMessage(new Message(MessageType.READY_STATE,
                client.getPlayerName(),
                String.valueOf(isReady)));
    }

    private void requestNextLevel() {
        if (gameState != null && gameState.isGameFinished() && !gameState.isAllLevelsCompleted()) {
            showingLevelResults = false;
            client.sendMessage(new Message(MessageType.NEXT_LEVEL,
                    client.getPlayerName(),
                    "Запрос следующего уровня"));
        }
    }

    private void startNextLevelTimer() {
        if (nextLevelTimer != null) {
            nextLevelTimer.stop();
        }

        nextLevelTimer = new Timer(NEXT_LEVEL_DELAY_MS, e -> {
            if (gameState != null && gameState.isGameFinished() && !gameState.isAllLevelsCompleted() && !gameCompleted) {
                showingLevelResults = false;
                client.sendMessage(new Message(MessageType.NEXT_LEVEL,
                        client.getPlayerName(),
                        "Автоматический переход"));
            }
            nextLevelTimer.stop();
        });

        nextLevelTimer.setRepeats(false);
        nextLevelTimer.start();
    }


    private void updateGameState(GameState newState) {
        gameState = newState;
        client.setCurrentGameState(gameState);
        mazeCanvas.repaint();
        updateStatus();
    }

    private void updateStatus() {
        if (gameState == null) {
            statusLabel.setText("Ожидание подключения...");
            return;
        }

        if (gameCompleted) {
            return;
        }

        levelLabel.setText("Уровень: " + gameState.getCurrentLevel() + "/" + gameState.getMaxLevels());

        if (gameState.getPlayers() != null && !gameState.getPlayers().isEmpty()) {
            StringBuilder sb = new StringBuilder("Игроки: ");
            int readyCount = 0;

            for (Player player : gameState.getPlayers()) {
                sb.append(player.getName())
                        .append(player.isReady() ? "(Г)" : "(Н)")
                        .append(" ");
                if (player.isReady()) readyCount++;
            }

            sb.append("| Готовы: ").append(readyCount).append("/").append(gameState.getPlayers().size());
            statusLabel.setText(sb.toString());

            if (readyCount == gameState.getPlayers().size() && gameState.getPlayers().size() == 2) {
                statusLabel.setForeground(Color.GREEN);
            } else if (readyCount > 0) {
                statusLabel.setForeground(Color.ORANGE);
            } else {
                statusLabel.setForeground(Color.RED);
            }
        }

        long levelTime = gameState.isGameStarted() && !gameState.isGameFinished() ?
                System.currentTimeMillis() - gameState.getGameStartTime() : 0;

        timeLabel.setText(String.format("Время уровня: %s | Общее время: %s",
                formatTime(levelTime),
                formatTime(roomTotalTime)));

        updateButtons();
    }

    private void updateTime() {
        SwingUtilities.invokeLater(this::updateStatus);
    }

    private void updateButtons() {
        if (gameState == null || gameCompleted) return;

        if (gameState.isGameFinished()) {
            readyButton.setEnabled(false);
            readyButton.setText("Уровень пройден");
            readyButton.setBackground(new Color(128, 128, 128));

            if (gameState.isAllLevelsCompleted()) {
                nextLevelButton.setEnabled(false);
                nextLevelButton.setText("Все уровни пройдены");
                nextLevelButton.setBackground(new Color(128, 128, 128));
            } else {
                nextLevelButton.setEnabled(true);
                nextLevelButton.setText("Следующий уровень");
            }
        } else if (gameState.isGameStarted()) {
            readyButton.setEnabled(false);
            readyButton.setText("Игра идет...");
            readyButton.setBackground(new Color(128, 128, 128));
            nextLevelButton.setEnabled(false);
        } else {
            readyButton.setEnabled(true);
            Player player = gameState.getPlayer(client.getPlayerName());
            if (player != null) {
                isReady = player.isReady();
                readyButton.setText(isReady ? "Не готов" : "Готов");
                readyButton.setBackground(isReady ? new Color(220, 20, 60) : new Color(60, 179, 113));
            }
            nextLevelButton.setEnabled(false);
        }
    }


    private void showLevelHelp() {
        if (gameState == null) return;

        int currentLevel = gameState.getCurrentLevel();
        String helpText = getLevelHelpText(currentLevel);
        String legendText = getLegendText();

        String message = String.format(
                "<html><div style='width: 400px;'>" +
                        "<h3 style='text-align: center;'>Уровень %d</h3>" +
                        "<hr>" +
                        "<p><b>Тип лабиринта:</b> %s</p>" +
                        "<p><b>Размер:</b> %dx%d</p>" +
                        "<br>" +
                        "<p><b>Особенности уровня:</b></p>%s" +
                        "<br>" +
                        "<p><b>Обозначения:</b></p>%s" +
                        "</div></html>",
                currentLevel,
                gameState.getMaze().getMazeType(),
                gameState.getMaze().getWidth(),
                gameState.getMaze().getHeight(),
                helpText,
                legendText);

        JOptionPane.showMessageDialog(this,
                message,
                "Справка по уровню",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String getLevelHelpText(int level) {
        switch (level) {
            case 1: return "• Спиральный лабиринт<br>• Двигайтесь от краев к центру";
            case 2: return "• Крестообразная структура<br>• Используйте перекрестки";
            case 3: return "• Алмазная форма<br>• Диагональные проходы";
            case 4: return "• Концентрические круги<br>• Ищите разрывы";
            case 5: return "• Телепорты (T)<br>• Используйте для быстрого перемещения";
            case 6: return "• Несколько выходов<br>• Только один настоящий (зеленый)";
            case 7: return "• Случайные тропы<br>• Уникальная генерация";
            case 8: return "• Рекурсивное деление<br>• Симметричные комнаты";
            case 9: return "• Ключи (K) и двери (D)<br>• Собирайте ключи";
            case 10: return "• ФИНАЛЬНЫЙ УРОВЕНЬ<br>• Все виды препятствий";
            default: return "• Стандартный лабиринт";
        }
    }

    private String getLegendText() {
        return "<table border='0' cellspacing='5'>" +
                "<tr><td bgcolor='#4169E1' width='20' height='20'></td><td>Игрок 1 (синий)</td></tr>" +
                "<tr><td bgcolor='#DC143C' width='20' height='20'></td><td>Игрок 2 (красный)</td></tr>" +
                "<tr><td bgcolor='#8A2BE2' width='20' height='20'></td><td>T - Телепорт</td></tr>" +
                "<tr><td bgcolor='#FFA500' width='20' height='20'></td><td>? - Ложный выход</td></tr>" +
                "<tr><td bgcolor='#8B4513' width='20' height='20'></td><td>D - Дверь (нужен ключ)</td></tr>" +
                "<tr><td bgcolor='#FFD700' width='20' height='20'></td><td>K - Ключ</td></tr>" +
                "<tr><td bgcolor='#B22222' width='20' height='20'></td><td>X - Ловушка (отбрасывает)</td></tr>" +
                "<tr><td bgcolor='#32CD32' width='20' height='20'></td><td>EX - Настоящий выход</td></tr>" +
                "</table>";
    }

    private String formatTime(long milliseconds) {
        if (milliseconds < 1000) {
            return String.format("%d мс", milliseconds);
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        milliseconds = milliseconds % 1000;

        if (minutes > 0) {
            return String.format("%d мин %02d.%03d сек", minutes, seconds, milliseconds);
        } else {
            return String.format("%d.%03d сек", seconds, milliseconds);
        }
    }

    private void returnToMenu() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><b>Вернуться в главное меню?</b><br>" +
                        "Текущая игра будет завершена.</html>",
                "Подтверждение",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (nextLevelTimer != null) nextLevelTimer.stop();
            if (scheduler != null) scheduler.shutdown();
            if (client != null) client.disconnect();

            SwingUtilities.invokeLater(() -> {
                MainMenu menu = new MainMenu();
                menu.setVisible(true);
            });
            dispose();
        }
    }

    @Override
    public void dispose() {
        if (nextLevelTimer != null) nextLevelTimer.stop();
        if (scheduler != null) scheduler.shutdown();
        if (client != null) client.disconnect();
        super.dispose();
    }

    // ==================== ВНУТРЕННИЙ КЛАСС ДЛЯ ОТРИСОВКИ ЛАБИРИНТА ====================

    private class MazeCanvas extends JPanel {
        public MazeCanvas() {
            setBackground(new Color(240, 240, 240));
            setFocusable(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (gameState == null || gameState.getMaze() == null) {
                drawWaitingScreen(g2d);
                return;
            }

            drawMaze(g2d);
            drawPlayers(g2d);
            drawExit(g2d);

            if (gameCompleted) {
                drawGameCompleteMessage(g2d);
            }
        }

        private void drawGameCompleteMessage(Graphics2D g2d) {
            String message = "ИГРА ЗАВЕРШЕНА!";

            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2;
            g2d.drawString(message, x, y);

            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            String subMessage = "Поздравляем с прохождением всех уровней!";
            fm = g2d.getFontMetrics();
            x = (getWidth() - fm.stringWidth(subMessage)) / 2;
            y = getHeight() / 2 + 40;
            g2d.drawString(subMessage, x, y);
        }

        @Override
        public Dimension getPreferredSize() {
            if (gameState == null || gameState.getMaze() == null) {
                return new Dimension(600, 600);
            }

            int width = gameState.getMaze().getWidth() * CELL_SIZE + 20;
            int height = gameState.getMaze().getHeight() * CELL_SIZE + 20;
            return new Dimension(width, height);
        }

        private void drawWaitingScreen(Graphics2D g2d) {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.DARK_GRAY);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));

            String text = "Ожидание подключения к игре...";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = getHeight() / 2;

            g2d.drawString(text, x, y);
        }

        private void drawMaze(Graphics2D g2d) {
            int[][] grid = gameState.getMaze().getGrid();
            int width = gameState.getMaze().getWidth();
            int height = gameState.getMaze().getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int cellValue = grid[y][x];
                    Color cellColor = getCellColor(cellValue);

                    g2d.setColor(cellColor);
                    g2d.fillRect(x * CELL_SIZE + 10, y * CELL_SIZE + 10, CELL_SIZE, CELL_SIZE);

                    g2d.setColor(cellColor.darker());
                    g2d.drawRect(x * CELL_SIZE + 10, y * CELL_SIZE + 10, CELL_SIZE, CELL_SIZE);

                    if (cellValue >= 2) {
                        drawCellSymbol(g2d, x, y, cellValue);
                    }
                }
            }
        }

        private Color getCellColor(int cellValue) {
            switch (cellValue) {
                case 0: return new Color(245, 245, 245);
                case 1: return new Color(80, 80, 80);
                case 2: return new Color(138, 43, 226);
                case 3:
                    long time = System.currentTimeMillis();
                    boolean blink = (time / 500) % 2 == 0;
                    return blink ? new Color(255, 165, 0) : new Color(255, 140, 0);
                case 4: return new Color(139, 69, 19);
                case 5: return new Color(255, 215, 0);
                case 6:
                    long trapTime = System.currentTimeMillis();
                    boolean trapBlink = (trapTime / 300) % 2 == 0;
                    return trapBlink ? new Color(220, 20, 60) : new Color(178, 34, 34);
                default: return new Color(240, 240, 240);
            }
        }

        private void drawCellSymbol(Graphics2D g2d, int x, int y, int cellValue) {
            String symbol = "";
            Color textColor = Color.WHITE;

            switch (cellValue) {
                case 2: symbol = "T"; textColor = Color.WHITE; break;
                case 3: symbol = "?"; textColor = Color.BLACK; break;
                case 4: symbol = "D"; textColor = Color.WHITE; break;
                case 5: symbol = "K"; textColor = Color.BLACK; break;
                case 6: symbol = "X"; textColor = Color.WHITE; break;
            }

            if (!symbol.isEmpty()) {
                g2d.setColor(textColor);
                g2d.setFont(new Font("Arial", Font.BOLD, CELL_SIZE / 2));
                FontMetrics fm = g2d.getFontMetrics();
                int cellX = x * CELL_SIZE + 10;
                int cellY = y * CELL_SIZE + 10;
                int sx = cellX + (CELL_SIZE - fm.stringWidth(symbol)) / 2;
                int sy = cellY + CELL_SIZE / 2 + fm.getAscent() / 2;
                g2d.drawString(symbol, sx, sy);
            }
        }

        private void drawPlayers(Graphics2D g2d) {
            if (gameState.getPlayers() == null) return;

            for (Player player : gameState.getPlayers()) {
                Position pos = player.getPosition();
                if (pos == null) continue;

                drawPlayer(g2d, player, pos);

                if (player.getKeys() > 0) {
                    drawKeysIndicator(g2d, player, pos);
                }

                if (player.hasTeleported()) {
                    drawTeleportEffect(g2d, pos);
                }
                if (player.isTrapActive()) {
                    drawTrapEffect(g2d, pos);
                }
            }
        }

        private void drawPlayer(Graphics2D g2d, Player player, Position pos) {
            int centerX = pos.getX() * CELL_SIZE + 10 + CELL_SIZE / 2;
            int centerY = pos.getY() * CELL_SIZE + 10 + CELL_SIZE / 2;
            int radius = CELL_SIZE / 2 - 2;

            Color playerColor = player.getColor();
            g2d.setColor(playerColor);
            g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            g2d.setColor(playerColor.darker());
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g2d.getFontMetrics();
            String name = player.getName();

            if (name.length() > 6) {
                name = name.substring(0, 6) + ".";
            }

            int nameX = centerX - fm.stringWidth(name) / 2;
            int nameY = centerY + radius + fm.getHeight() + 5;

            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fillRect(nameX - 2, nameY - fm.getHeight() + 2,
                    fm.stringWidth(name) + 4, fm.getHeight() - 2);

            g2d.setColor(Color.BLACK);
            g2d.drawString(name, nameX, nameY);

            if (player.isAtExit()) {
                drawAtExitIndicator(g2d, centerX, centerY, radius);
            }
        }

        private void drawKeysIndicator(Graphics2D g2d, Player player, Position pos) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g2d.getFontMetrics();
            String keysText = "x" + player.getKeys();
            int tx = pos.getX() * CELL_SIZE + 10 + CELL_SIZE - fm.stringWidth(keysText) - 2;
            int ty = pos.getY() * CELL_SIZE + 10 + CELL_SIZE - 2;
            g2d.drawString(keysText, tx, ty);
        }

        private void drawTeleportEffect(Graphics2D g2d, Position pos) {
            long time = System.currentTimeMillis();
            float alpha = 0.5f + 0.5f * (float)Math.sin(time * 0.01);
            g2d.setColor(new Color(138, 43, 226, (int)(alpha * 255)));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(
                    pos.getX() * CELL_SIZE + 12,
                    pos.getY() * CELL_SIZE + 12,
                    CELL_SIZE - 4,
                    CELL_SIZE - 4
            );
        }

        private void drawTrapEffect(Graphics2D g2d, Position pos) {
            long time = System.currentTimeMillis();
            float alpha = 0.3f + 0.7f * (float)Math.sin(time * 0.02);
            g2d.setColor(new Color(255, 0, 0, (int)(alpha * 100)));
            g2d.fillOval(
                    pos.getX() * CELL_SIZE + 10,
                    pos.getY() * CELL_SIZE + 10,
                    CELL_SIZE,
                    CELL_SIZE
            );
        }

        private void drawAtExitIndicator(Graphics2D g2d, int centerX, int centerY, int radius) {
            g2d.setColor(new Color(0, 255, 0, 150));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(
                    centerX - radius - 3,
                    centerY - radius - 3,
                    (radius + 3) * 2,
                    (radius + 3) * 2
            );
        }

        private void drawExit(Graphics2D g2d) {
            Position exit = gameState.getMaze().getExit();
            int x = exit.getX() * CELL_SIZE + 10;
            int y = exit.getY() * CELL_SIZE + 10;

            long time = System.currentTimeMillis();
            boolean visible = (time / 500) % 2 == 0;

            if (visible) {
                g2d.setColor(new Color(0, 200, 0, 200));
            } else {
                g2d.setColor(new Color(0, 150, 0, 150));
            }

            g2d.fillRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);

            g2d.setColor(Color.GREEN);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            String text = "EX";
            int textX = x + (CELL_SIZE - fm.stringWidth(text)) / 2;
            int textY = y + CELL_SIZE / 2 + fm.getAscent() / 2;
            g2d.drawString(text, textX, textY);
        }
    }
}