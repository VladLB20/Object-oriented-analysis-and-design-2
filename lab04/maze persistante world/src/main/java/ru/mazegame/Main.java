package ru.mazegame;

import org.h2.jdbcx.JdbcDataSource;
import ru.mazegame.database.DatabaseInitializer;
import ru.mazegame.gateway.*;
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

        MazeGateway mazeGateway = new H2MazeGateway(ds);
        PlayerGateway playerGateway = new H2PlayerGateway(ds);
        EnemyGateway enemyGateway = new H2EnemyGateway(ds);
        GroundItemGateway groundItemGateway = new H2GroundItemGateway(ds);
        InventoryGateway inventoryGateway = new H2InventoryGateway(ds);

        InventoryService inventoryService = new InventoryService(inventoryGateway);
        CombatService combatService = new CombatService(enemyGateway, inventoryService);
        MazeWorldService worldService = new MazeWorldService(
                mazeGateway, playerGateway, enemyGateway,
                groundItemGateway, inventoryGateway,
                combatService, inventoryService
        );

        SwingUtilities.invokeLater(() -> new MazeGameGUI(
                worldService, combatService, inventoryService,
                enemyGateway, playerGateway, groundItemGateway
        ));
    }
}