package ru.itis.dis403.sw2_game.server;

import ru.itis.dis403.sw2_game.common.message.*;
import ru.itis.dis403.sw2_game.common.model.GameState;
import ru.itis.dis403.sw2_game.common.model.MatchResult;
import ru.itis.dis403.sw2_game.server.db.MatchDao;

import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameServer {

    private final int port;
    private final MatchDao matchDao;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();
    protected final GameState state = new GameState();

    private volatile boolean running;

    private long startTimeMillis;
    private long countdownStartMillis;
    private static final int COUNTDOWN_SECONDS = 5;

    public static final int WORLD_WIDTH = 2200;
    public static final int WORLD_HEIGHT = 720;
    public static final int TILE_SIZE = 64;
    public static final int FLOOR_Y = WORLD_HEIGHT - TILE_SIZE;
    public static final int CEIL_Y = TILE_SIZE;
    public static final int PLAYER_WIDTH = 40;
    public static final int PLAYER_HEIGHT = 60;

    private static final double GRAVITY = 900.0;
    private static final double RUN_SPEED = 260.0;

    private final boolean[] ready = new boolean[GameState.PLAYERS_COUNT];

    public GameServer(int port, MatchDao matchDao) {
        this.port = port;
        this.matchDao = matchDao;
        initLevelNotStarted();
    }

    private void initLevelNotStarted() {
        state.getPlatforms().clear();
        state.getSpikes().clear();

        state.addPlatform(new Rectangle(0, FLOOR_Y, WORLD_WIDTH, TILE_SIZE));
        state.addPlatform(new Rectangle(0, 0, WORLD_WIDTH, CEIL_Y));

        state.addSpike(new Rectangle(500, FLOOR_Y - 120, 40, 120));
        state.addSpike(new Rectangle(800, CEIL_Y, 40, 120));
        state.addSpike(new Rectangle(1100, FLOOR_Y - 180, 40, 180));
        state.addSpike(new Rectangle(1400, CEIL_Y, 40, 180));
        state.addSpike(new Rectangle(1700, FLOOR_Y - 120, 40, 120));

        state.setFinish(new Rectangle(2000, FLOOR_Y - 160, 60, 160));

        GameState.PlayerState[] players = state.getPlayers();

        GameState.PlayerState p0 = players[0];
        p0.setAlive(true);
        p0.setX(120);
        p0.setY(FLOOR_Y - PLAYER_HEIGHT);
        p0.setGravityDown(true);
        p0.setVy(0);

        GameState.PlayerState p1 = players[1];
        p1.setAlive(true);
        p1.setX(200);
        p1.setY(FLOOR_Y - PLAYER_HEIGHT);
        p1.setGravityDown(true);
        p1.setVy(0);

        state.setCountdown(0);
        state.setRunning(false);
        state.setWinnerIndex(-1);
        state.setWinType("");
        state.setElapsedSeconds(0);
        state.setWorldWidth(WORLD_WIDTH);
        state.setWorldHeight(WORLD_HEIGHT);
        state.setGameStartTime(-1);
    }

    private void startRoundCountdown() {
        initLevelNotStarted();

        long now = System.currentTimeMillis();
        countdownStartMillis = now;
        startTimeMillis = now;

        state.setGameStartTime(now);
        state.setCountdown(COUNTDOWN_SECONDS);
        state.setRunning(false);
        state.setWinnerIndex(-1);

        broadcastState(state);
    }

    public GameState.PlayerState getPlayerState(int index) {
        if (index < 0 || index >= GameState.PLAYERS_COUNT) return null;
        return state.getPlayers()[index];
    }

    public GameState getState() {
        return state;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (clients.size() < GameState.PLAYERS_COUNT) {
            Socket s = serverSocket.accept();
            int index = clients.size();
            ClientHandler handler = new ClientHandler(this, s, index);
            clients.add(handler);
            new Thread(handler, "Client-" + index).start();
            System.out.println("Client connected: index=" + index);
        }

        running = true;
        gameLoop();
    }

    private void gameLoop() {
        final int fps = 60;
        final double dt = 1.0 / fps;

        long lastNano = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            double delta = (now - lastNano) / 1e9;

            if (delta >= dt) {
                update(delta);
                broadcastState(state);
                lastNano = now;
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void update(double dt) {
        long now = System.currentTimeMillis();

        if (!ready[0] || !ready[1]) {
            return;
        }

        if (!state.isGameStarted()) {
            return;
        }

        long passed = (now - countdownStartMillis) / 1000;
        int left = COUNTDOWN_SECONDS - (int) passed;

        if (left > 0) {
            state.setCountdown(left);
            return;
        }

        state.setCountdown(0);
        state.setRunning(true);

        long elapsed = (now - startTimeMillis) / 1000;
        state.setElapsedSeconds(elapsed);

        if (state.getWinnerIndex() != -1) {
            return;
        }

        GameState.PlayerState[] players = state.getPlayers();
        for (GameState.PlayerState p : players) {
            if (p == null || !p.isAlive()) continue;

            p.setVx(RUN_SPEED);
            p.setX(p.getX() + p.getVx() * dt);

            double accY = p.isGravityDown() ? GRAVITY : -GRAVITY;
            p.setVy(p.getVy() + accY * dt);
            double newY = p.getY() + p.getVy() * dt;

            if (p.isGravityDown()) {
                if (newY > FLOOR_Y - PLAYER_HEIGHT) {
                    newY = FLOOR_Y - PLAYER_HEIGHT;
                    p.setVy(0);
                }
                if (newY < CEIL_Y) {
                    newY = CEIL_Y;
                    p.setVy(0);
                }
            } else {
                if (newY < CEIL_Y) {
                    newY = CEIL_Y;
                    p.setVy(0);
                }
                if (newY > FLOOR_Y - PLAYER_HEIGHT) {
                    newY = FLOOR_Y - PLAYER_HEIGHT;
                    p.setVy(0);
                }
            }

            p.setY(newY);

            if (p.getX() < -1000 || p.getX() > WORLD_WIDTH + 1000 ||
                    p.getY() < -1000 || p.getY() > WORLD_HEIGHT + 1000) {
                p.setAlive(false);
            }
        }

        checkSpikesAndFinish();
    }

    private void checkSpikesAndFinish() {
        GameState.PlayerState[] players = state.getPlayers();

        for (int i = 0; i < players.length; i++) {
            GameState.PlayerState p = players[i];
            if (p == null || !p.isAlive()) continue;

            Rectangle hitBox = new Rectangle(
                    (int) p.getX(),
                    (int) p.getY(),
                    PLAYER_WIDTH,
                    PLAYER_HEIGHT
            );

            for (Rectangle s : state.getSpikes()) {
                if (hitBox.intersects(s)) {
                    p.setAlive(false);
                    System.out.println("Player " + i + " died on spike");
                }
            }

            Rectangle finish = state.getFinish();
            if (finish != null && hitBox.intersects(finish)) {
                System.out.println("Player " + i + " reached finish");
                endGame(i, "FINISH");
                return;
            }
        }

        int aliveCount = 0;
        int lastAliveIndex = -1;
        for (int i = 0; i < players.length; i++) {
            if (players[i] != null && players[i].isAlive()) {
                aliveCount++;
                lastAliveIndex = i;
            }
        }

        if (aliveCount == 1) {
            endGame(lastAliveIndex, "LAST_ALIVE");
        } else if (aliveCount == 0) {
            endGame(-1, "NO_ONE");
        }
    }

    private void endGame(int winnerIndex, String winType) {
        state.setWinnerIndex(winnerIndex);
        state.setWinType(winType);
        running = false;

        String name = null;
        String color = null;
        if (winnerIndex == 0) {
            name = "Игрок 1";
            color = "RED";
        } else if (winnerIndex == 1) {
            name = "Игрок 2";
            color = "BLUE";
        }

        int duration = (int) state.getElapsedSeconds();
        LocalDateTime dt = LocalDateTime.now();

        MatchResult result = new MatchResult(name, color, dt, duration, winType);
        try {
            matchDao.insertMatch(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        GameOverMessage msg = new GameOverMessage(
                winnerIndex, name, color, duration, winType, dt
        );
        broadcast(msg);
    }

    public synchronized void broadcast(Message msg) {
        for (ClientHandler ch : clients) {
            ch.send(msg);
        }
    }

    public synchronized void broadcastState(GameState gameState) {
        for (ClientHandler ch : clients) {
            ch.sendState(gameState);
        }
    }

    public void handleInput(int playerIndex, InputMessage input) {
        GameState.PlayerState p = getPlayerState(playerIndex);
        if (p == null || !p.isAlive()) return;

        if (input.getInputType() == InputMessage.InputType.FLIP_GRAVITY) {
            p.setGravityDown(!p.isGravityDown());
            p.setVy(0);
        } else if (input.getInputType() == InputMessage.InputType.EXIT_GAME) {
            p.setAlive(false);
        }
    }

    public void handleMatchesRequest(ClientHandler from) {
        try {
            var list = matchDao.findAll();
            from.send(new MatchesListMessage(list));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void setPlayerReady(int index) {
        if (index < 0 || index >= ready.length) return;
        ready[index] = true;
        System.out.println("Player " + index + " READY");
        if (ready[0] && ready[1]) {
            startRoundCountdown();
        }
    }

    public synchronized void removeClient(ClientHandler ch, int index) {
        clients.remove(ch);
        if (index >= 0 && index < ready.length) {
            ready[index] = false;
        }
        System.out.println("Client removed: index=" + index);
    }

    public static void main(String[] args) throws IOException {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String pass = "safiullina90";

        MatchDao dao = new MatchDao(url, user, pass);
        GameServer server = new GameServer(5555, dao);
        server.start();
    }
}
