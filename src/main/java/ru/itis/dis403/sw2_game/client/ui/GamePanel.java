package ru.itis.dis403.sw2_game.client.ui;

import ru.itis.dis403.sw2_game.client.GameClient;
import ru.itis.dis403.sw2_game.common.model.GameState;
import ru.itis.dis403.sw2_game.server.GameServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.RenderingHints;

/**
 * Игровая панель: отрисовка уровня и снеговиков.
 * Шипы расположены слева от прямоугольника и остриём смотрят влево.
 */
public class GamePanel extends JPanel {

    private final MainFrame frame;
    private final GameClient client;

    private volatile GameState state;

    // Синхронизация отсчёта с сервером
    private long localGameStartTime = -1;  // Время начала игры на локальном клиенте
    private int initialCountdown = -1;     // Начальное значение отсчёта

    private static final int TILE_W = 64;
    private static final int TILE_H = 64;

    private static int instanceCount = 0;
    private int myInstanceId;

    public GamePanel(MainFrame frame, GameClient client) {
        this.frame = frame;
        this.client = client;

        this.myInstanceId = instanceCount++;

        initUi();

        setFocusable(true);
        requestFocusInWindow();
    }

    private void initUi() {
        setBackground(new Color(5, 10, 60));
        setLayout(null);

        JButton backBtn = new JButton("Меню");
        backBtn.setBounds(10, 10, 90, 30);
        backBtn.addActionListener(e -> frame.showMenu());
        add(backBtn);

        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (myInstanceId == 0 && e.getKeyCode() == KeyEvent.VK_SPACE) {
                    client.sendFlipGravity();
                } else if (myInstanceId == 1 && e.getKeyCode() == KeyEvent.VK_UP) {
                    client.sendFlipGravity();
                }
            }
        });
    }

    /**
     * Обновить состояние игры с синхронизацией времени отсчёта
     */
    public void updateState(GameState newState) {
        this.state = newState;

        // ИСПРАВЛЕНИЕ: Синхронизируем время начала игры только один раз
        if (localGameStartTime == -1 && newState.getGameStartTime() > 0) {
            localGameStartTime = newState.getGameStartTime();
            initialCountdown = newState.getCountdown();
            System.out.println("[GamePanel] Game started at: " + localGameStartTime +
                    ", initial countdown: " + initialCountdown);
        }

        // Логирование для отладки (можно удалить позже)
        int displayCountdown = calculateCountdown(newState);
        System.out.println("[GamePanel] Server countdown: " + newState.getCountdown() +
                ", calculated countdown: " + displayCountdown);

        repaint();
    }

    /**
     * Рассчитать текущий отсчёт на основе серверного времени
     * @param s текущее состояние игры
     * @return значение отсчёта, синхронизированное с сервером
     */
    private int calculateCountdown(GameState s) {
        // Если отсчёт ещё не начался на сервере
        if (localGameStartTime == -1 || s.getGameStartTime() <= 0) {
            return s.getCountdown();
        }

        // Получаем текущее серверное время на основе времени начала
        long elapsed = System.currentTimeMillis() - localGameStartTime;
        int remainingCountdown = initialCountdown - (int)(elapsed / 1000);

        // Если время истекло, возвращаем 0
        if (remainingCountdown <= 0) {
            return 0;
        }

        return remainingCountdown;
    }

    public void startGameIfNeeded() {
        requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        GameState s = this.state;
        if (s == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Очищаем фон
        drawBackground(g2, w, h);

        // Получаем позиции игроков для определения камеры
        GameState.PlayerState[] players = s.getPlayers();
        double avgX = 0;
        int aliveCount = 0;

        for (GameState.PlayerState p : players) {
            if (p != null && p.isAlive()) {
                avgX += p.getX();
                aliveCount++;
            }
        }

        if (aliveCount > 0) {
            avgX /= aliveCount;
        }

        // Смещаем камеру за игроками
        int cameraX = (int)avgX - w / 3;
        if (cameraX < 0) cameraX = 0;
        if (cameraX > s.getWorldWidth() - w) cameraX = s.getWorldWidth() - w;

        // Рисуем мир
        drawBorderTiles(g2, s.getWorldWidth(), h);
        drawSpikesAndFinish(g2, s);

        // Рисуем игроков
        for (int i = 0; i < players.length; i++) {
            GameState.PlayerState p = players[i];
            if (p == null || !p.isAlive()) continue;
            drawSnowman(g2, p, i);
        }

        // ИСПРАВЛЕНИЕ: Рисуем отсчёт с синхронизацией
        int displayCountdown = calculateCountdown(s);
        if (displayCountdown > 0) {
            g2.setFont(getFont().deriveFont(Font.BOLD, 72f));
            g2.setColor(Color.WHITE);
            String text = String.valueOf(displayCountdown);
            FontMetrics fm = g2.getFontMetrics();
            int x = (w - fm.stringWidth(text)) / 2;
            int y = (h + fm.getAscent()) / 2;
            g2.drawString(text, x, y);
        }
    }

    private void drawBackground(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(5, 10, 60));
        g2.fillRect(0, 0, w, h);
    }

    private void drawBorderTiles(Graphics2D g2, int worldWidth, int h) {
        int tilesCount = worldWidth / TILE_W + 2;

        Color tileColor = new Color(80, 130, 255);
        g2.setColor(tileColor);

        int bottomY = h - TILE_H;
        int topY = 0;

        for (int i = 0; i < tilesCount; i++) {
            int x = i * TILE_W;
            g2.fillRoundRect(x, bottomY, TILE_W, TILE_H, 10, 10);
            g2.fillRoundRect(x, topY, TILE_W, TILE_H, 10, 10);
        }
    }

    private void drawSpikesAndFinish(Graphics2D g2, GameState s) {
        for (java.awt.Rectangle r : s.getSpikes()) {
            drawSpikeColumnLeft(g2, r);
        }

        if (s.getFinish() != null) {
            g2.setColor(new Color(0, 200, 0));
            java.awt.Rectangle f = s.getFinish();
            g2.fillRoundRect(f.x, f.y, f.width, f.height, 10, 10);
        }
    }

    /**
     * Рисует колонну шипов, направленных ВЛЕВО.
     * rect.x – правая граница колонны; остриё треугольника на стороне x.
     */
    private void drawSpikeColumnLeft(Graphics2D g2, java.awt.Rectangle rect) {
        int spikes = rect.height / 20;
        if (spikes < 1) spikes = 1;
        int spikeH = rect.height / spikes;
        int spikeW = rect.width;

        for (int i = 0; i < spikes; i++) {
            int yTop = rect.y + i * spikeH;

            // цветные полосы справа от треугольника
            g2.setColor(new Color(80, 200, 255)); // голубой
            g2.fillRect(rect.x + spikeW * 2 / 3, yTop, spikeW / 3, spikeH);

            g2.setColor(new Color(120, 230, 100)); // зелёный
            g2.fillRect(rect.x + spikeW / 3, yTop, spikeW / 3, spikeH);

            // жёлтый треугольник, остриём ВЛЕВО
            int xRight = rect.x + spikeW / 3;
            int[] xs = {
                    xRight,      // правая верхняя
                    rect.x,      // острие слева
                    xRight       // правая нижняя
            };
            int[] ys = {
                    yTop,
                    yTop + spikeH / 2,
                    yTop + spikeH
            };
            g2.setColor(new Color(255, 210, 0));
            g2.fillPolygon(xs, ys, 3);
        }
    }

    private void drawSnowman(Graphics2D g2, GameState.PlayerState p, int index) {
        int x = (int) Math.round(p.getX());
        int y = (int) Math.round(p.getY());

        // Цвет шляпы
        Color hatColor = index == 0 ?
                new Color(200, 30, 30) : // Красный для игрока 1
                new Color(30, 30, 200);  // Синий для игрока 2

        // 1. Нижний шар (тело)
        g2.setColor(Color.WHITE);
        int bodySize = 36;
        int bodyX = x + (GameServer.PLAYER_WIDTH - bodySize) / 2;
        int bodyY = y + GameServer.PLAYER_HEIGHT - bodySize;
        g2.fillOval(bodyX, bodyY, bodySize, bodySize);

        // 2. Верхний шар (голова)
        int headSize = 24;
        int headX = x + (GameServer.PLAYER_WIDTH - headSize) / 2;
        int headY = bodyY - headSize + 8; // Перекрытие
        g2.fillOval(headX, headY, headSize, headSize);

        // 3. Глаза
        g2.setColor(Color.BLACK);
        int eyeSize = 3;
        int leftEyeX = headX + 6;
        int rightEyeX = headX + headSize - 6 - eyeSize;
        int eyeY = headY + 8;
        g2.fillOval(leftEyeX, eyeY, eyeSize, eyeSize);
        g2.fillOval(rightEyeX, eyeY, eyeSize, eyeSize);

        // 4. Нос (простой оранжевый овал)
        g2.setColor(new Color(255, 140, 0));
        int noseX = headX + headSize / 2 - 2;
        int noseY = eyeY + 4;
        g2.fillOval(noseX, noseY, 4, 3);

        // 5. Шляпа (простая)
        g2.setColor(hatColor);
        // Поля
        g2.fillRect(headX - 3, headY - 5, headSize + 6, 5);
        // Верх
        g2.fillRect(headX + 3, headY - 15, headSize - 6, 10);
    }
}
