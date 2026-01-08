package ru.itis.dis403.sw2_game;


import ru.itis.dis403.sw2_game.ui.MainMenu;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Устанавливаем Look and Feel для лучшего внешнего вида
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Не удалось установить системный Look and Feel: " + e.getMessage());
        }

        // Запускаем главное меню
        MainMenu.main(args);
    }
}
