package ru.itis.dis403.sw2_game.ui;

import ru.itis.dis403.sw2_game.db.BestTimeRecord;
import ru.itis.dis403.sw2_game.db.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.time.format.*;
import java.awt.event.*;

public class BestTimesDialog extends JDialog {
    private final DatabaseManager dbManager;
    private JTable roomTable;
    private final String currentRoom;

    public BestTimesDialog(JFrame parent, DatabaseManager dbManager,
                           List<BestTimeRecord> leaderboard, String currentRoom) {
        super(parent, "Полная таблица рекордов комнат", true);
        this.dbManager = dbManager;
        this.currentRoom = currentRoom;

        setSize(900, 650);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        initUI();
        loadRoomLeaderboard(leaderboard);
    }

    public BestTimesDialog(JFrame parent, DatabaseManager dbManager) {
        super(parent, "Таблица рекордов комнат", true);
        this.dbManager = dbManager;
        this.currentRoom = null;

        setSize(900, 650);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        initUI();
        loadRoomLeaderboard(null);
    }

    private void initUI() {
        // Создаем панель с таблицей
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Заголовок
        JLabel titleLabel = new JLabel("ТАБЛИЦА РЕКОРДОВ КОМНАТ", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(0, 100, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Создаем таблицу
        String[] columns = {"Место", "Название комнаты", "Уровней пройдено", "Общее время", "Дата прохождения"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Integer.class : String.class;
            }
        };

        roomTable = new JTable(model);
        roomTable.setRowHeight(35);
        roomTable.setFont(new Font("Arial", Font.PLAIN, 12));
        roomTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTable.setGridColor(new Color(200, 200, 200));
        roomTable.setShowGrid(true);

        // Настраиваем ширину колонок
        roomTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        roomTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        roomTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        roomTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        roomTable.getColumnModel().getColumn(4).setPreferredWidth(150);

        // Настраиваем рендерер для выделения текущей комнаты
        roomTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                String roomName = (String) table.getValueAt(row, 1);
                if (currentRoom != null && roomName.contains(currentRoom)) {
                    c.setBackground(new Color(220, 255, 220));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else if (row % 2 == 0) {
                    c.setBackground(new Color(240, 240, 240));
                } else {
                    c.setBackground(Color.WHITE);
                }

                if (isSelected) {
                    c.setBackground(new Color(180, 220, 255));
                }

                setHorizontalAlignment(SwingConstants.CENTER);
                if (column == 1) { // Название комнаты
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                return c;
            }
        });

        // Прокручиваемая панель для таблицы
        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 149, 237), 2),
                "ТОП-10 комнат по общему времени прохождения"
        ));

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton refreshButton = createStyledButton("Обновить", new Color(70, 130, 180));
        JButton clearButton = createStyledButton("Очистить все", new Color(220, 20, 60));
        JButton closeButton = createStyledButton("Закрыть", new Color(100, 100, 100));

        refreshButton.addActionListener(e -> loadRoomLeaderboard(null));
        clearButton.addActionListener(e -> clearAllRecords());
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(closeButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Информационная панель
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String infoText = "<html><b>Примечание:</b> В таблице отображаются комнаты, успешно прошедшие все 10 уровней.<br>" +
                "Результаты сортируются по общему времени прохождения (меньшее время - лучшее место).";

        if (currentRoom != null) {
            infoText += "<br><font color='green'><b>Текущая комната выделена зеленым цветом.</b></font>";
        }
        infoText += "</html>";

        JLabel infoLabel = new JLabel(infoText);
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        infoLabel.setForeground(Color.DARK_GRAY);
        infoPanel.add(infoLabel);

        add(mainPanel, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.SOUTH);
    }

    private void returnToMenu() {
        dispose();
        SwingUtilities.invokeLater(() -> {
            MainMenu menu = new MainMenu();
            menu.setVisible(true);
        });
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(140, 35));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(color.darker());
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(color);
            }
        });
        return button;
    }

    private void loadRoomLeaderboard(List<BestTimeRecord> externalLeaderboard) {
        DefaultTableModel model = (DefaultTableModel) roomTable.getModel();
        model.setRowCount(0);

        List<BestTimeRecord> records;
        if (externalLeaderboard != null) {
            records = externalLeaderboard;
        } else {
            records = dbManager.getRoomLeaderboard();
        }

        if (records.isEmpty()) {
            model.addRow(new Object[]{"-", "Нет записей", "-", "-", "-"});
        } else {
            int position = 1;
            for (BestTimeRecord record : records) {
                String roomName = record.getRoomName();
                if (currentRoom != null && roomName.equals(currentRoom)) {
                    roomName += " (ваша)";
                }

                model.addRow(new Object[]{
                        position,
                        roomName,
                        record.getLevelsCompleted() + " из 10",
                        formatTime(record.getTotalTime()),
                        record.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                });
                position++;
                if (position > 10) break;
            }
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        milliseconds = milliseconds % 1000;

        if (minutes > 0) {
            return String.format("%d мин %02d сек", minutes, seconds);
        } else if (seconds > 0) {
            return String.format("%d.%03d сек", seconds, milliseconds);
        } else {
            return String.format("%d мс", milliseconds);
        }
    }

    private void clearAllRecords() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><b>Вы уверены, что хотите удалить ВСЕ записи?</b><br>" +
                        "Это действие нельзя отменить. Все результаты будут потеряны.</html>",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dbManager.clearAllRecords();
                JOptionPane.showMessageDialog(this,
                        "Все записи успешно удалены",
                        "Успех",
                        JOptionPane.INFORMATION_MESSAGE);

                loadRoomLeaderboard(null);

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                        "Ошибка при очистке записей: " + e.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}