package ru.itis.dis403.sw2_game.client;

import ru.itis.dis403.sw2_game.client.ui.MainFrame;
import ru.itis.dis403.sw2_game.common.model.*;
import ru.itis.dis403.sw2_game.common.message.*;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * TCP‑клиент: подключается к серверу, отправляет Input/Requests,
 * принимает GameState, GameOver, MatchesList и передаёт их в UI.
 */
public class GameClient {

    private final String host;
    private final int port;
    private final MainFrame mainFrame;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private volatile int playerIndex = -1; // если ты захочешь его использовать

    public GameClient(String host, int port, MainFrame mainFrame) {
        this.host = host;
        this.port = port;
        this.mainFrame = mainFrame;
        connectAsync();
    }

    private void connectAsync() {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                listenLoop();
            } catch (IOException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(mainFrame,
                                "Не удалось подключиться к серверу: " + e.getMessage(),
                                "Ошибка", JOptionPane.ERROR_MESSAGE));
            }
        }, "Client-Connect-Thread").start();
    }

    private void listenLoop() {
        try {
            while (!socket.isClosed()) {
                Object obj = in.readObject();
                System.out.println("client got: " + obj.getClass().getSimpleName());
                if (!(obj instanceof Message msg)) {
                    continue;
                }

                switch (msg.getType()) {
                    case STATE -> handleGameState((GameState) msg);
                    case GAME_OVER -> handleGameOver((GameOverMessage) msg);
                    case MATCHES_LIST -> handleMatchesList((MatchesListMessage) msg);
                    case PLAYER_INDEX -> handlePlayerIndex((PlayerIndexMessage) msg);
                    default -> {
                        // остальные типы игнорируем или логируем
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // соединение оборвалось
            e.printStackTrace();
        }
    }
    private void handlePlayerIndex(PlayerIndexMessage msg) {
        this.playerIndex = msg.getPlayerIndex();
        System.out.println("My playerIndex = " + playerIndex);
    }

    private void handleGameState(GameState state) {
        SwingUtilities.invokeLater(() ->
                mainFrame.getGamePanel().updateState(state));
    }

    private void handleGameOver(GameOverMessage msg) {
        SwingUtilities.invokeLater(() -> {
            String text;
            if (msg.getWinnerIndex() >= 0) {
                int n = msg.getWinnerIndex() + 1; // 1‑й или 2‑й игрок
                text = "Победил: игрок " + n + "\n" +
                        "Тип победы: " + msg.getWinType() + "\n" +
                        "Длительность: " + msg.getDurationSeconds() + " с";
            } else {
                text = "Ничья!";
            }
            JOptionPane.showMessageDialog(mainFrame, text,
                    "Результат матча", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void handleMatchesList(MatchesListMessage msg) {
        SwingUtilities.invokeLater(() ->
                mainFrame.getMatchesPanel().showMatches(msg.getMatches()));
    }

    // --- публичные методы, которые дергает UI ---

    public void sendFlipGravity() {
        if (out == null) return;
        sendMessage(new InputMessage(InputMessage.InputType.FLIP_GRAVITY, playerIndex));
    }

    public void sendExitGame() {
        if (out == null) return;
        sendMessage(new InputMessage(InputMessage.InputType.EXIT_GAME, playerIndex));
    }

    public void requestMatches() {
        if (out == null) return;
        sendMessage(new MatchesRequestMessage());
    }

    private synchronized void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendReady() {
        sendMessage(new ReadyMessage());
    }
}
