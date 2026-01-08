package ru.itis.dis403.sw2_game.ui;

import ru.itis.dis403.sw2_game.network.GameClient;
import ru.itis.dis403.sw2_game.network.Message;
import ru.itis.dis403.sw2_game.network.MessageType;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ConnectionDialog extends JDialog {
    private JTextField nameField;
    private JTextField hostField;
    private JTextField portField;
    private JButton connectButton;
    private JButton cancelButton;
    private GameClient client;
    private boolean connected;
    private String playerName;

    public ConnectionDialog(JFrame parent) {
        super(parent, "Подключение к игре", true);
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        connected = false;

        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        panel.add(new JLabel("Ваше имя:"));
        nameField = new JTextField("Игрок2");
        panel.add(nameField);

        panel.add(new JLabel("Адрес сервера:"));
        hostField = new JTextField("localhost");
        panel.add(hostField);

        panel.add(new JLabel("Порт:"));
        portField = new JTextField("5555");
        panel.add(portField);

        panel.add(new JLabel());
        connectButton = new JButton("Подключиться");
        connectButton.addActionListener(e -> connectToServer());
        panel.add(connectButton);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void connectToServer() {
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();
        playerName = nameField.getText().trim(); // Получаем имя игрока

        if (playerName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Введите имя игрока",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portText);

            // Пытаемся подключиться
            client = new GameClient(host, port, playerName);
            client.startListening();

            // Отправляем сообщение о присоединении
            client.sendMessage(new Message(MessageType.PLAYER_JOIN, playerName));

            connected = true;
            dispose();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Неверный номер порта",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось подключиться к серверу: " + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public GameClient getClient() {
        return client;
    }

    public String getPlayerName() {
        return playerName;
    }
}