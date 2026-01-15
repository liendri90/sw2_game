package ru.itis.dis403.sw2_game.network;

import ru.itis.dis403.sw2_game.model.GameState;
import ru.itis.dis403.sw2_game.model.Maze;
import ru.itis.dis403.sw2_game.model.Player;
import ru.itis.dis403.sw2_game.model.Position;
import ru.itis.dis403.sw2_game.db.DatabaseManager;
import ru.itis.dis403.sw2_game.db.BestTimeRecord;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.*;

public class GameServer {
    private ServerSocket serverSocket;
    private GameState gameState;
    private Map<String, ClientHandler> clients;
    private ExecutorService threadPool;
    private boolean running;
    private DatabaseManager dbManager;
    private Random random = new Random();

    private String roomName;
    private String roomDescription;
    private long roomTotalTime;
    private int levelsCompleted;
    private long levelStartTime;
    private boolean roomActive = false;
    private boolean gameCompleted = false;

    private boolean sendingState = false;
    private final Object stateLock = new Object();

    public GameServer(int port) throws IOException, SQLException, ClassNotFoundException {
        serverSocket = new ServerSocket(port);
        gameState = null;
        clients = new HashMap<>();
        threadPool = Executors.newCachedThreadPool();
        running = true;
        dbManager = new DatabaseManager();
        dbManager.connect();

        gameCompleted = false;

        System.out.println("Сервер запущен на порту " + port);
        System.out.println("Ожидание создания комнаты...");
        System.out.println("База данных подключена");
    }

    public void createRoom(String roomName, String hostName) {
        this.roomName = roomName;
        this.roomDescription = "Хост: " + hostName;
        this.gameState = new GameState();
        this.roomTotalTime = 0;
        this.levelsCompleted = 0;
        this.levelStartTime = 0;
        this.roomActive = true;
        this.gameCompleted = false;

        System.out.println("Комната создана: " + roomName);
        System.out.println("Хост: " + hostName);
    }

    public void start() {
        threadPool.execute(this::acceptConnections);
        System.out.println("Сервер готов принимать подключения");
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler);
                System.out.println("Новое подключение принято");
            } catch (IOException e) {
                if (running) {
                    System.err.println("Ошибка при принятии соединения: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
            System.out.println("Сервер остановлен");
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии сервера: " + e.getMessage());
        }
        threadPool.shutdown();
        dbManager.disconnect();
    }

    private void sendGuaranteedGameState() {
        synchronized (stateLock) {
            if (sendingState || gameCompleted || gameState == null) {
                return;
            }
            sendingState = true;

            try {
                GameState stateCopy = gameState.createCopy();
                Message stateMessage = new Message(MessageType.GAME_STATE, "SERVER", null, stateCopy);

                List<ClientHandler> disconnectedClients = new ArrayList<>();

                for (ClientHandler client : clients.values()) {
                    if (client.connected) {
                        try {
                            client.output.writeObject(stateMessage);
                            client.output.flush();
                            client.output.reset();
                        } catch (IOException e) {
                            System.err.println("Ошибка отправки состояния клиенту " +
                                    client.playerName + ": " + e.getMessage());
                            disconnectedClients.add(client);
                        }
                    }
                }

                for (ClientHandler client : disconnectedClients) {
                    client.disconnect();
                }

            } finally {
                sendingState = false;
            }
        }
    }

    private synchronized void broadcast(Message message, String excludeSender) {
        for (ClientHandler client : clients.values()) {
            if (excludeSender == null || !client.playerName.equals(excludeSender)) {
                try {
                    client.sendMessage(message);
                } catch (Exception e) {
                    System.err.println("Ошибка при отправке сообщения клиенту " +
                            client.playerName + ": " + e.getMessage());
                }
            }
        }
    }

    private synchronized void broadcastToAll(Message message) {
        for (ClientHandler client : clients.values()) {
            try {
                client.sendMessage(message);
            } catch (Exception e) {
                System.err.println("Ошибка при отправке сообщения клиенту " +
                        client.playerName + ": " + e.getMessage());
            }
        }
    }

    private synchronized void goToNextLevel() {
        System.out.println("=== ПЕРЕХОД НА СЛЕДУЮЩИЙ УРОВЕНЬ ===");

        if (gameState.isAllLevelsCompleted()) {
            handleGameComplete();
            return;
        }

        int oldLevel = gameState.getCurrentLevel();
        gameState.nextLevel();
        int newLevel = gameState.getCurrentLevel();

        System.out.println("Переход с уровня " + oldLevel + " на уровень " + newLevel);

        broadcastToAll(new Message(MessageType.NEW_LEVEL,
                "SERVER",
                "Уровень " + newLevel + " - " + gameState.getMaze().getMazeType(),
                gameState.createCopy()));

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        sendGuaranteedGameState();

        if (gameState.getPlayers().size() == 2) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (gameState) {
                        boolean allConnected = true;
                        for (Player player : gameState.getPlayers()) {
                            if (!clients.containsKey(player.getName())) {
                                allConnected = false;
                                break;
                            }
                            player.setReady(true);
                        }

                        if (allConnected && !gameState.isGameStarted()) {
                            System.out.println("Автоматический старт уровня " + newLevel);
                            gameState.startGame();
                            levelStartTime = System.currentTimeMillis();

                            sendGuaranteedGameState();

                            broadcastToAll(new Message(MessageType.GAME_STARTED,
                                    "SERVER",
                                    "Уровень " + newLevel + " начался! Идите к выходу!",
                                    gameState.createCopy()));
                        }
                    }
                }
            }, 2000);
        }
    }

    private synchronized void handleGameComplete() {
        if (gameCompleted) {
            return;
        }

        gameCompleted = true;
        System.out.println("=== ВСЕ 10 УРОВНЕЙ ПРОЙДЕНЫ ===");
        System.out.println("Комната: " + roomName);
        System.out.println("Общее время: " + roomTotalTime + " мс");
        System.out.println("Уровней пройдено: " + gameState.getCurrentLevel());

        dbManager.saveRoomResult(roomName, roomTotalTime, gameState.getCurrentLevel());
        System.out.println("Результат сохранен в БД");

        List<BestTimeRecord> leaderboard = dbManager.getRoomLeaderboard();

        System.out.println("Отправляю результаты всем " + clients.size() + " клиентам");
        for (ClientHandler client : clients.values()) {
            Message finalMessage = new Message(MessageType.GAME_COMPLETE,
                    "SERVER",
                    roomTotalTime + "|" + gameState.getCurrentLevel() + "|" + roomName,
                    leaderboard);
            client.sendMessage(finalMessage);
            System.out.println("Отправлено клиенту: " + client.playerName);
        }

        System.out.println("=== ТАБЛИЦА РЕКОРДОВ КОМНАТ ===");
        if (leaderboard.isEmpty()) {
            System.out.println("(пока нет записей)");
        } else {
            for (BestTimeRecord record : leaderboard) {
                System.out.println(record);
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream output;
        private ObjectInputStream input;
        private String playerName;
        private boolean connected;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.connected = true;
        }

        @Override
        public void run() {
            try {
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());

                Message joinMessage = (Message) input.readObject();

                if (joinMessage.getType() == MessageType.CREATE_ROOM) {
                    String roomName = joinMessage.getData();

                    if (!roomActive) {
                        createRoom(roomName, joinMessage.getSender());
                        playerName = "Хост";
                        sendMessage(new Message(MessageType.ROOM_CREATED, "SERVER", "Комната создана успешно!"));
                    } else {
                        sendMessage(new Message(MessageType.ERROR, "SERVER", "Комната уже создана"));
                    }
                    return;
                }
                if (joinMessage.getType() == MessageType.PLAYER_JOIN) {
                    playerName = joinMessage.getSender();

                    if (clients.containsKey(playerName)) {
                        sendMessage(new Message(MessageType.ERROR, "SERVER", "Имя уже занято"));
                        return;
                    }

                    System.out.println("Попытка подключения игрока: " + playerName);

                    if (!roomActive || gameState == null) {
                        sendMessage(new Message(MessageType.ERROR, "SERVER", "Комната не создана"));
                        return;
                    }

                    if (gameCompleted) {
                        sendMessage(new Message(MessageType.ERROR, "SERVER", "Игра уже завершена"));
                        return;
                    }

                    Player player = null;
                    synchronized (this) {
                        if (gameState.getPlayers().size() >= 2) {
                            sendMessage(new Message(MessageType.ERROR, "SERVER", "Комната уже заполнена (2 игрока)"));
                            return;
                        }

                        if (gameState.getPlayers().isEmpty()) {
                            player = new Player(playerName,
                                    gameState.getMaze().getStart1(),
                                    Color.BLUE);
                            System.out.println("Первый игрок добавлен: " + playerName);
                        } else {
                            player = new Player(playerName,
                                    gameState.getMaze().getStart2(),
                                    Color.RED);
                            System.out.println("Второй игрок добавлен: " + playerName);
                        }

                        gameState.addPlayer(player);
                    }

                    clients.put(playerName, this);

                    System.out.println("Клиент " + playerName + " подключен. Всего клиентов: " + clients.size());

                    sendMessage(new Message(MessageType.ROOM_INFO, "SERVER", roomName + "|" + roomDescription));

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    sendMessage(new Message(MessageType.GAME_STATE, "SERVER", null, gameState.createCopy()));

                    broadcast(new Message(MessageType.PLAYER_JOIN, "SERVER",
                            playerName + " присоединился к игре"), playerName);

                    if (gameState.getPlayers().size() == 2) {
                        System.out.println("Оба игрока подключены!");
                        sendGuaranteedGameState();
                        broadcastToAll(new Message(MessageType.INFO, "SERVER",
                                "Оба игроки подключены! Нажмите 'Готов' для старта."));
                    }
                }

                while (connected && !gameCompleted) {
                    try {
                        Message message = (Message) input.readObject();
                        processMessage(message);
                    } catch (EOFException e) {
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("Неизвестный тип сообщения: " + e.getMessage());
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Ошибка в соединении с " + playerName + ": " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void processMessage(Message message) {
            if (gameCompleted) {
                return;
            }

            switch (message.getType()) {
                case PLAYER_MOVE:
                    handlePlayerMove(message);
                    break;
                case READY_STATE:
                    handleReadyState(message);
                    break;
                case REQUEST_GAME_STATE:
                    sendMessage(new Message(MessageType.GAME_STATE, "SERVER", null, gameState.createCopy()));
                    break;
                case NEXT_LEVEL:
                    handleNextLevelRequest();
                    break;
            }
        }

        private void handlePlayerMove(Message message) {
            synchronized (gameState) {
                Player player = gameState.getPlayer(playerName);
                if (player == null || !gameState.isGameStarted() ||
                        gameState.isGameFinished() || gameCompleted) {
                    return;
                }

                String[] coords = message.getData().split(",");
                if (coords.length == 2) {
                    try {
                        int newX = Integer.parseInt(coords[0]);
                        int newY = Integer.parseInt(coords[1]);
                        Position newPos = new Position(newX, newY);
                        Position oldPos = player.getPosition();

                        if (!gameState.getMaze().canMove(oldPos, newPos)) {
                            return;
                        }

                        int cellValue = gameState.getMaze().getCellValue(newPos);
                        boolean moveAllowed = true;

                        switch (cellValue) {
                            case 4:
                                if (!player.hasKey()) {
                                    sendMessage(new Message(MessageType.INFO, "SERVER",
                                            "Нужен ключ, чтобы открыть дверь!"));
                                    moveAllowed = false;
                                } else {
                                    player.useKey();
                                    gameState.getMaze().setCellValue(newPos, 0);
                                    broadcastToAll(new Message(MessageType.INFO, "SERVER",
                                            playerName + " открыл дверь!"));
                                    System.out.println(playerName + " открыл дверь на позиции " + newPos);
                                }
                                break;

                            case 5:
                                player.addKey();
                                gameState.getMaze().removeKey(newPos);
                                sendMessage(new Message(MessageType.INFO, "SERVER",
                                        "Вы подобрали ключ! Всего ключей: " + player.getKeys()));
                                System.out.println(playerName + " подобрал ключ на позиции " + newPos);
                                break;

                            case 6:
                                handleTrap(player, newPos);
                                break;
                        }

                        if (!moveAllowed) {
                            return;
                        }

                        player.setPosition(newPos);

                        if (gameState.getMaze().isTeleport(newPos)) {
                            handleTeleport(player, newPos);
                        }

                        sendGuaranteedGameState();

                        if (gameState.getMaze().isExit(newPos)) {
                            System.out.println("Игрок " + playerName + " достиг выхода на позиции " + newPos);
                            player.setAtExit(true);

                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            sendGuaranteedGameState();

                            boolean allAtExit = gameState.getPlayers().stream()
                                    .allMatch(Player::isAtExit);

                            if (allAtExit && !gameState.isGameFinished()) {
                                System.out.println("ВСЕ игроки достигли выхода! Уровень завершен.");
                                gameState.setGameFinished(true);

                                try {
                                    Thread.sleep(150);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }

                                handleLevelComplete();
                            }
                        } else if (gameState.getMaze().isFakeExit(newPos)) {
                            sendMessage(new Message(MessageType.INFO, "SERVER",
                                    "Это ложный выход! Ищите дальше."));
                        }

                    } catch (NumberFormatException e) {
                        System.err.println("Неверный формат координат: " + message.getData());
                    }
                }
            }
        }

        private void handleTeleport(Player player, Position teleportPos) {
            Maze maze = gameState.getMaze();
            Position pairedTeleport = maze.getPairedTeleport(teleportPos);

            if (pairedTeleport != null) {
                if (maze.getCellValue(pairedTeleport) == 0 ||
                        maze.getCellValue(pairedTeleport) == 2) {

                    player.setPosition(pairedTeleport);
                    player.setHasTeleported(true);

                    broadcastToAll(new Message(MessageType.INFO, "SERVER",
                            player.getName() + " телепортировался!"));
                    System.out.println(player.getName() + " телепортировался с " + teleportPos + " на " + pairedTeleport);

                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            player.setHasTeleported(false);
                            sendGuaranteedGameState();
                        }
                    }, 2000);
                }
            }
        }

        private void handleTrap(Player player, Position trapPos) {
            System.out.println(player.getName() + " попал в ловушку на позиции " + trapPos);

            player.setTrapActive(true);
            player.setPosition(trapPos);

            int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
            int[] dir = directions[random.nextInt(4)];
            int distance = 2 + random.nextInt(3);

            Position oldPos = trapPos;
            Position newPos = oldPos;

            for (int i = 1; i <= distance; i++) {
                Position testPos = new Position(
                        oldPos.getX() + dir[0] * i,
                        oldPos.getY() + dir[1] * i
                );

                if (gameState.getMaze().canMove(oldPos, testPos)) {
                    newPos = testPos;
                } else {
                    break;
                }
            }

            player.setPosition(newPos);

            broadcastToAll(new Message(MessageType.INFO, "SERVER",
                    player.getName() + " попал в ловушку и отброшен!"));

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    player.setTrapActive(false);
                    sendGuaranteedGameState();
                }
            }, 2000);
        }

        private synchronized void handleLevelComplete() {
            if (gameCompleted) {
                return;
            }

            System.out.println("Уровень " + gameState.getCurrentLevel() + " пройден!");

            long levelTime = System.currentTimeMillis() - levelStartTime;
            roomTotalTime += levelTime;
            levelsCompleted = gameState.getCurrentLevel();

            System.out.println("Время уровня: " + levelTime + "мс");
            System.out.println("Общее время комнаты: " + roomTotalTime + "мс");

            for (Player player : gameState.getPlayers()) {
                ClientHandler client = clients.get(player.getName());
                if (client != null) {
                    Message levelCompleteMsg = new Message(MessageType.LEVEL_COMPLETE,
                            "SERVER",
                            levelTime + "|" + roomTotalTime + "|" + levelsCompleted,
                            null);
                    client.sendMessage(levelCompleteMsg);
                }
            }

            if (gameState.getCurrentLevel() >= 10) {
                System.out.println("=== ЭТО БЫЛ 10 УРОВЕНЬ! ЗАВЕРШАЕМ ИГРУ ===");
                gameState.setAllLevelsCompleted(true);

                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (gameState) {
                            handleGameComplete();
                        }
                    }
                }, 2000);
            } else {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (gameState) {
                            goToNextLevel();
                        }
                    }
                }, 3000);
            }
        }

        private void handleNextLevelRequest() {
            synchronized (gameState) {
                if (gameState.isGameFinished() && !gameState.isAllLevelsCompleted() && !gameCompleted) {
                    goToNextLevel();
                }
            }
        }

        private void handleReadyState(Message message) {
            synchronized (gameState) {
                Player player = gameState.getPlayer(playerName);
                if (player != null) {
                    boolean isReady = Boolean.parseBoolean(message.getData());
                    player.setReady(isReady);

                    System.out.println("Игрок " + playerName + " теперь " + (isReady ? "готов" : "не готов"));

                    boolean allReady = gameState.getPlayers().size() == 2 &&
                            gameState.getPlayers().stream().allMatch(Player::isReady);

                    if (allReady && !gameState.isGameStarted() && !gameCompleted) {
                        System.out.println("Все игроки готовы! Запуск игры");
                        gameState.startGame();
                        levelStartTime = System.currentTimeMillis();

                        sendGuaranteedGameState();

                        broadcastToAll(new Message(MessageType.GAME_STARTED,
                                "SERVER", "Игра началась! Идите к выходу!", gameState.createCopy()));
                    } else {
                        sendGuaranteedGameState();
                    }
                }
            }
        }

        public void sendMessage(Message message) {
            try {
                if (connected && output != null) {
                    output.writeObject(message);
                    output.flush();
                }
            } catch (IOException e) {
                System.err.println("Ошибка при отправке сообщения " + playerName + ": " + e.getMessage());
                disconnect();
            }
        }

        private void disconnect() {
            connected = false;

            clients.remove(playerName);

            if (playerName != null) {
                synchronized (gameState) {
                    gameState.removePlayer(playerName);
                }
                System.out.println("Игрок " + playerName + " отключился");
                System.out.println("Осталось клиентов: " + clients.size());

                broadcast(new Message(MessageType.PLAYER_LEFT,
                        "SERVER", playerName + " покинул игру"), null);

                sendGuaranteedGameState();
            }

            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
            }
        }
    }
}