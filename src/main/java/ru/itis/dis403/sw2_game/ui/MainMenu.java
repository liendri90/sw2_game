package ru.itis.dis403.sw2_game.ui;

import ru.itis.dis403.sw2_game.db.DatabaseManager;
import ru.itis.dis403.sw2_game.network.GameClient;
import ru.itis.dis403.sw2_game.network.GameServer;
import ru.itis.dis403.sw2_game.network.Message;
import ru.itis.dis403.sw2_game.network.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.awt.event.*;

public class MainMenu extends JFrame {
    private JButton hostButton;
    private JButton joinButton;
    private JButton bestTimesButton;
    private JButton exitButton;
    private DatabaseManager dbManager;

    public MainMenu() {
        setTitle("Maze Escape - Главное меню");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        dbManager = new DatabaseManager();

        initUI();
    }

    private void initUI() {
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("MAZE ESCAPE");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 100, 0));
        titlePanel.add(titleLabel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(4, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        hostButton = new JButton("Создать игру (Хост)");
        joinButton = new JButton("Присоединиться");
        bestTimesButton = new JButton("Лучшие времена");
        exitButton = new JButton("Выход");


        styleButton(hostButton, new Color(70, 130, 180));
        styleButton(joinButton, new Color(60, 179, 113));
        styleButton(bestTimesButton, new Color(255, 165, 0));
        styleButton(exitButton, new Color(220, 20, 60));

        buttonPanel.add(hostButton);
        buttonPanel.add(joinButton);
        buttonPanel.add(bestTimesButton);
        buttonPanel.add(exitButton);

        hostButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    createHostGame();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        joinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                joinGame();
            }
        });

        bestTimesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showBestTimes();
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        JPanel infoPanel = new JPanel();
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel infoLabel = new JLabel("<html><center>Два игрока должны одновременно достичь выхода.<br>Дверь откроется только когда оба будут у выхода!</center></html>");
        infoPanel.add(infoLabel);

        add(titlePanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.SOUTH);
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
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
    }

    private void createHostGame() throws SQLException {
        String playerName = JOptionPane.showInputDialog(this,
                "Введите ваше имя (Хост):", "Создание игры", JOptionPane.QUESTION_MESSAGE);

        if (playerName == null || playerName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Имя хоста не может быть пустым", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String roomName = JOptionPane.showInputDialog(this,
                "Введите название комнаты:", "Создание игры", JOptionPane.QUESTION_MESSAGE);

        if (roomName == null || roomName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Название комнаты не может быть пустым", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final String finalPlayerName = playerName.trim();
        final String finalRoomName = roomName.trim();

        try {
            Thread serverThread = new Thread(() -> {
                try {
                    GameServer server = new GameServer(5555);
                    System.out.println("Сервер запущен на порту 5555");

                    server.createRoom(finalRoomName, finalPlayerName);
                    server.start();
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(MainMenu.this,
                                    "Ошибка сервера: " + e.getMessage(),
                                    "Ошибка", JOptionPane.ERROR_MESSAGE));
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            Thread.sleep(2000);

            try {
                GameClient client = new GameClient("localhost", 5555, finalPlayerName);
                client.startListening();

                client.sendMessage(new Message(MessageType.CREATE_ROOM, finalPlayerName, finalRoomName));

                SwingUtilities.invokeLater(() -> {
                    GamePanel gamePanel = new GamePanel(client, dbManager,
                            finalRoomName, "Хост: " + finalPlayerName);
                    gamePanel.setVisible(true);
                    this.dispose();
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(MainMenu.this,
                                "Не удалось подключиться к серверу: " + e.getMessage(),
                                "Ошибка", JOptionPane.ERROR_MESSAGE));
            }

        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "Ошибка при создании игры: " + e.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE));
        }
    }

    private void joinGame() {
        ConnectionDialog dialog = new ConnectionDialog(this);
        dialog.setVisible(true);

        if (dialog.isConnected()) {
            GameClient client = dialog.getClient();
            String playerName = dialog.getPlayerName();

            SwingUtilities.invokeLater(() -> {
                GamePanel gamePanel = new GamePanel(client, dbManager, "Подключение...", "Ожидание информации о комнате");
                gamePanel.setVisible(true);
                this.dispose();
            });
        }
    }

    private void showBestTimes() {
        try {
            BestTimesDialog bestTimesDialog = new BestTimesDialog(this, dbManager);
            bestTimesDialog.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка при загрузке данных: " + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainMenu menu = new MainMenu();
            menu.setVisible(true);
        });
    }
}