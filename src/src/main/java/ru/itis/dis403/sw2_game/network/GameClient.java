package ru.itis.dis403.sw2_game.network;

import ru.itis.dis403.sw2_game.model.GameState;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GameClient {
    private Socket socket;
    private OutputStream output;
    private InputStream input;
    private String playerName;
    private boolean connected;
    private Thread receiveThread;
    private BlockingQueue<Message> messageQueue;
    private GameState currentGameState;

    public GameClient(String serverAddress, int port, String playerName) throws IOException {
        this.socket = new Socket(serverAddress, port);
        this.output = socket.getOutputStream();
        this.input = socket.getInputStream();
        this.playerName = playerName;
        this.connected = true;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.currentGameState = null;

        sendMessage(new Message(MessageType.PLAYER_JOIN, playerName));
    }

    public void startListening() {
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[65536];

            while (connected) {
                try {
                    int bytesRead = input.read(buffer);
                    if (bytesRead > 0) {
                        byte[] messageData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, messageData, 0, bytesRead);
                        Message message = Protocol.deserializeMessage(messageData);
                        processIncomingMessage(message);
                        messageQueue.put(message);
                    } else if (bytesRead == -1) {
                        disconnect();
                        break;
                    }
                } catch (IOException e) {
                    if (connected) {
                        System.err.println("Ошибка при чтении сообщения: " + e.getMessage());
                    }
                    disconnect();
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("Неизвестный тип сообщения: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        receiveThread.start();
    }

    private void processIncomingMessage(Message message) {
        switch (message.getType()) {
            case GAME_STATE:
                if (message.getPayload() instanceof GameState) {
                    currentGameState = (GameState) message.getPayload();
                }
                break;
            case LEVEL_COMPLETE:
            case NEW_LEVEL:
                if (message.getPayload() instanceof GameState) {
                    currentGameState = (GameState) message.getPayload();
                }
                break;
            case PLAYER_LEFT:
                System.out.println("Игрок " + message.getData() + " покинул игру");
                break;
        }
    }

    public void sendMessage(Message message) {
        if (connected) {
            try {
                byte[] data = Protocol.serializeMessage(message);
                output.write(data);
                output.flush();
            } catch (IOException e) {
                System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
                disconnect();
            }
        }
    }

    public Message receiveMessage() throws InterruptedException {
        return messageQueue.take();
    }

    public Message pollMessage() {
        return messageQueue.poll();
    }

    public GameState getCurrentGameState() {
        return currentGameState;
    }

    public void setCurrentGameState(GameState state) {
        this.currentGameState = state;
    }

    public void disconnect() {
        connected = false;
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
            if (receiveThread != null) {
                receiveThread.interrupt();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String getPlayerName() {
        return playerName;
    }
}