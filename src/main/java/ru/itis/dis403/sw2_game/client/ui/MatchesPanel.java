package ru.itis.dis403.sw2_game.client.ui;

import ru.itis.dis403.sw2_game.common.model.MatchResult;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Панель "Матчи": показывает список матчей, присланный сервером.
 */
public class MatchesPanel extends JPanel {

    private final MainFrame frame;
    private final JTable table = new JTable();

    public MatchesPanel(MainFrame frame) {
        this.frame = frame;
        initUi();
    }

    private void initUi() {
        setLayout(new BorderLayout());
        setBackground(new Color(5, 10, 60));

        JButton backBtn = new JButton("Меню");
        backBtn.addActionListener(e -> frame.showMenu());
        add(backBtn, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     * Вызывается GameClient, когда от сервера пришёл MatchesListMessage.
     */
    public void showMatches(List<MatchResult> matches) {
        String[] cols = {"Победитель", "Цвет шляпы", "Дата/время", "Длительность, с", "Тип победы"};
        Object[][] data = new Object[matches.size()][cols.length];

        for (int i = 0; i < matches.size(); i++) {
            MatchResult m = matches.get(i);
            data[i][0] = m.getWinnerName();
            data[i][1] = m.getWinnerHatColor();
            data[i][2] = m.getDateTime();
            data[i][3] = m.getDurationSeconds();
            data[i][4] = m.getWinType();
        }

        table.setModel(new javax.swing.table.DefaultTableModel(data, cols));
    }
}
