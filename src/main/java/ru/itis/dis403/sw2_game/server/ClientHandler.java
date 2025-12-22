package ru.itis.dis403.sw2_game.server;

import ru.itis.dis403.sw2_game.common.model.GameState;
import ru.itis.dis403.sw2_game.common.message.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Один клиент на сервере: читает сообщения и передаёт их GameServer.
 * ИСПРАВЛЕНИЕ: добавлен метод sendGameState() для синхронизации отсчёта
 */
public class ClientHandler implements Runnable {

    private final GameServer server;
    private final Socket socket;
    private int playerIndex;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(GameServer server, Socket socket, int playerIndex) {
        this.server = server;
        this.socket = socket;
        this.playerIndex = playerIndex;
    }

    @Override
    public void run() {
        try (socket) {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Сразу сообщаем клиенту его индекс
            send(new PlayerIndexMessage(playerIndex));

            // Отправляем начальное состояние
            sendGameState(server.state);

            while (!socket.isClosed()) {
                Object obj = in.readObject();

                if (!(obj instanceof Message msg)) {
                    continue;
                }

                switch (msg.getType()) {
                    case INPUT -> server.handleInput(playerIndex, (InputMessage) msg);
                    case MATCHES_REQUEST -> server.handleMatchesRequest(this);
                    case JOIN -> handleJoin((JoinMessage) msg);
                    case READY -> server.setPlayerReady(playerIndex);
                    default -> { }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client " + playerIndex + " disconnected: " + e.getMessage());
        } finally {
            server.removeClient(this, playerIndex);
        }
    }

    private void handleJoin(JoinMessage join) {
        GameState.PlayerState p = server.getPlayerState(playerIndex);
        if (p != null) {
            // Имя только по индексу
            p.setName("Игрок " + (playerIndex + 1));
            // Цвет шляпы по индексу: 0 – красный, 1 – синий
            if (playerIndex == 0) {
                p.setHatColor("RED");
            } else {
                p.setHatColor("BLUE");
            }
        }
    }

    /**
     * Отправить сообщение клиенту
     */
    public synchronized void send(Message msg) {
        try {
            if (out != null) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ИСПРАВЛЕНИЕ: отправить состояние игры клиенту
     * Используется для синхронизации GameState с информацией о gameStartTime
     */
    public synchronized void sendGameState(GameState state) {
        try {
            if (out != null) {
                out.writeObject(state);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Failed to send GameState: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
