package ru.itis.dis403.sw2_game.client.ui;

import ru.itis.dis403.sw2_game.client.GameClient;

import javax.swing.*;
import java.awt.*;

/**
 * Главное окно клиента с CardLayout.
 */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);

    private final MenuPanel menuPanel;
    private final GamePanel gamePanel;
    private final MatchesPanel matchesPanel;

    private final GameClient client;

    public GameClient getClient() {
        return client;
    }

    public MainFrame(String host, int port) {
        super("Snow Gravity Race");

        client = new GameClient(host, port, this);

        menuPanel = new MenuPanel(this);
        gamePanel = new GamePanel(this, client);
        matchesPanel = new MatchesPanel(this);

        root.add(menuPanel, "MENU");
        root.add(gamePanel, "GAME");
        root.add(matchesPanel, "MATCHES");

        setContentPane(root);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 720);
        setLocationRelativeTo(null);
    }

    public void showMenu() {
        cardLayout.show(root, "MENU");
        menuPanel.requestFocusInWindow();
    }

    public void showGame() {
        cardLayout.show(root, "GAME");
        gamePanel.requestFocusInWindow();
        gamePanel.startGameIfNeeded();
    }

    public void showMatches() {
        // просим сервер прислать список матчей
        client.requestMatches();
        cardLayout.show(root, "MATCHES");
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    public MatchesPanel getMatchesPanel() {
        return matchesPanel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame("localhost", 5555);
            frame.setVisible(true);
        });
    }
}
