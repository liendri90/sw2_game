package ru.itis.dis403.sw2_game.server;

import ru.itis.dis403.sw2_game.common.model.GameState;
import ru.itis.dis403.sw2_game.common.message.*;

import java.io.*;
import java.net.Socket;

/**
 * Один клиент на сервере: читает сообщения и передаёт их GameServer.
 */
public class ClientHandler implements Runnable {

    private final GameServer server;
    private final Socket socket;
    private final int playerIndex;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(GameServer server, Socket socket, int playerIndex) {
        this.server = server;
        this.socket = socket;
        this.playerIndex = playerIndex;
    }

    @Override
    public void run() {
        try (Socket s = socket) {
            out = new ObjectOutputStream(s.getOutputStream());
            out.flush();
            in = new ObjectInputStream(s.getInputStream());

            send(new PlayerIndexMessage(playerIndex));
            sendState(server.getState());

            while (!s.isClosed()) {
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
            p.setName("Игрок " + (playerIndex + 1));
        }
    }

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

    public synchronized void sendState(GameState state) {
        try {
            if (out != null) {
                out.writeObject(state);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
