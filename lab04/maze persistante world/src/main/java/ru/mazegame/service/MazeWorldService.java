package ru.mazegame.service;

import ru.mazegame.domain.*;
import ru.mazegame.gateway.*;

import java.util.*;

public class MazeWorldService {

    private final MazeGateway mazeGateway;
    private final PlayerGateway playerGateway;
    private final EnemyGateway enemyGateway;
    private final GroundItemGateway groundItemGateway;
    private final InventoryGateway inventoryGateway;
    private final CombatService combatService;
    private final InventoryService inventoryService;

    public MazeWorldService(MazeGateway mazeGateway,
                            PlayerGateway playerGateway,
                            EnemyGateway enemyGateway,
                            GroundItemGateway groundItemGateway,
                            InventoryGateway inventoryGateway,
                            CombatService combatService,
                            InventoryService inventoryService) {
        this.mazeGateway = mazeGateway;
        this.playerGateway = playerGateway;
        this.enemyGateway = enemyGateway;
        this.groundItemGateway = groundItemGateway;
        this.inventoryGateway = inventoryGateway;
        this.combatService = combatService;
        this.inventoryService = inventoryService;
    }

    public WorldState loadOrCreateWorld() {
        Optional<Maze> optMaze = mazeGateway.findMazeById(1);
        if (optMaze.isPresent()) {
            Maze maze = optMaze.get();
            List<Enemy> enemies = enemyGateway.findEnemiesByMazeId(maze.getId());
            List<GroundItem> items = groundItemGateway.findItemsByMazeId(maze.getId());
            return new WorldState(maze, enemies, items);
        }

        final int width = 40;
        final int height = 30;
        MazeGenerator.MazeData data = MazeGenerator.generate(width, height);

        Maze newMaze = new Maze(0, "Generated Maze", width, height, data.getCells());
        mazeGateway.createMaze(newMaze, data.getCells());
        Maze savedMaze = mazeGateway.findMazeById(1).orElseThrow();

        for (Enemy enemy : data.getEnemies()) {
            enemyGateway.createEnemy(enemy, savedMaze.getId());
        }
        for (GroundItem item : data.getItems()) {
            groundItemGateway.createItem(item, savedMaze.getId());
        }

        List<Enemy> enemies = enemyGateway.findEnemiesByMazeId(savedMaze.getId());
        List<GroundItem> items = groundItemGateway.findItemsByMazeId(savedMaze.getId());
        return new WorldState(savedMaze, enemies, items);
    }

    public Player loadOrCreatePlayer(Maze maze) {
        Optional<Player> optPlayer = playerGateway.findPlayerByMazeId(maze.getId());
        if (optPlayer.isPresent()) {
            return optPlayer.get();
        }
        return playerGateway.createPlayer("Hero", maze.getId(), 1, 1);
    }

    public MoveResult movePlayer(Player player, Maze maze,
                                 List<Enemy> enemies,
                                 List<GroundItem> groundItems,
                                 int dx, int dy) {
        int newX = player.getX() + dx;
        int newY = player.getY() + dy;
        Cell target = maze.getCell(newX, newY);
        if (target == null || target.isWall()) {
            return MoveResult.blocked("Там стена!");
        }

        Optional<Enemy> enemyOpt = enemies.stream()
                .filter(e -> e.getX() == newX && e.getY() == newY)
                .findFirst();
        if (enemyOpt.isPresent()) {
            return MoveResult.enemyEncounter(enemyOpt.get());
        }

        Optional<GroundItem> itemOpt = groundItems.stream()
                .filter(gi -> gi.getX() == newX && gi.getY() == newY)
                .findFirst();
        if (itemOpt.isPresent()) {
            GroundItem groundItem = itemOpt.get();
            inventoryService.pickUpGroundItem(groundItem, player, groundItemGateway);
            groundItems.remove(groundItem);
            player.moveTo(newX, newY);
            playerGateway.updatePlayerPosition(player);
            return MoveResult.success("Вы подобрали " + groundItem.getName() + "!");
        }

        player.moveTo(newX, newY);
        playerGateway.updatePlayerPosition(player);
        return MoveResult.success(null);
    }

    public static class WorldState {
        private final Maze maze;
        private final List<Enemy> enemies;
        private final List<GroundItem> items;

        public WorldState(Maze maze, List<Enemy> enemies, List<GroundItem> items) {
            this.maze = maze;
            this.enemies = enemies;
            this.items = items;
        }

        public Maze getMaze() { return maze; }
        public List<Enemy> getEnemies() { return enemies; }
        public List<GroundItem> getItems() { return items; }
    }

    public static class MoveResult {
        private final boolean success;
        private final String message;
        private final Enemy encounteredEnemy;

        private MoveResult(boolean success, String message, Enemy encounteredEnemy) {
            this.success = success;
            this.message = message;
            this.encounteredEnemy = encounteredEnemy;
        }

        public static MoveResult success(String message) {
            return new MoveResult(true, message, null);
        }

        public static MoveResult blocked(String message) {
            return new MoveResult(false, message, null);
        }

        public static MoveResult enemyEncounter(Enemy enemy) {
            return new MoveResult(false, null, enemy);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Optional<Enemy> getEncounteredEnemy() {
            return Optional.ofNullable(encounteredEnemy);
        }
    }
}