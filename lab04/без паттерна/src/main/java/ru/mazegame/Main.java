package ru.mazegame;

import org.h2.jdbcx.JdbcDataSource;
import ru.mazegame.database.DatabaseInitializer;
import ru.mazegame.service.CombatService;
import ru.mazegame.service.InventoryService;
import ru.mazegame.service.MazeWorldService;
import ru.mazegame.ui.MazeGameGUI;

import javax.swing.*;

public class Main {
    public static void main(String[] args) throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:file:./mazeworld;AUTO_SERVER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");

        DatabaseInitializer.initialize(ds);

        // Сервисы без шлюзов – каждый принимает DataSource
        InventoryService inventoryService = new InventoryService(ds);
        CombatService combatService = new CombatService(ds, inventoryService);
        MazeWorldService worldService = new MazeWorldService(ds, inventoryService, combatService);

        // GUI теперь тоже зависит от DataSource (чтобы напрямую удалять врагов)
        SwingUtilities.invokeLater(() -> new MazeGameGUI(worldService, combatService, inventoryService, ds));
    }
}