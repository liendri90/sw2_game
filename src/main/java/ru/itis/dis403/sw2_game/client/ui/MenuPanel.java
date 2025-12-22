package ru.itis.dis403.sw2_game.client.ui;

import javax.swing.*;
import java.awt.*;

public class MenuPanel extends JPanel {

    private final MainFrame frame;

    public MenuPanel(MainFrame frame) {
        this.frame = frame;
        initUi();
    }

    private void initUi() {
        setLayout(new GridBagLayout());
        setBackground(new Color(5, 10, 60));

        JButton playBtn = createMenuButton("Играть");
        JButton matchesBtn = createMenuButton("Матчи");
        JButton exitBtn = createMenuButton("Выход");

        playBtn.addActionListener(e -> {
            frame.getClient().sendReady(); // отправили READY
            frame.showGame();              // переключились на GamePanel
        });
        matchesBtn.addActionListener(e -> frame.showMatches());
        exitBtn.addActionListener(e -> System.exit(0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        add(playBtn, gbc);
        add(matchesBtn, gbc);
        add(exitBtn, gbc);
    }

    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(180, 40));
        return btn;
    }
}
