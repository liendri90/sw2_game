package ru.itis.dis403.sw2_game;


import ru.itis.dis403.sw2_game.ui.MainMenu;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Не удалось установить системный Look and Feel: " + e.getMessage());
        }

        MainMenu.main(args);
    }
}
