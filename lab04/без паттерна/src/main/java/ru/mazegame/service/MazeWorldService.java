package ru.mazegame.service;

import ru.mazegame.domain.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class MazeWorldService {
    private final DataSource dataSource;
    private final InventoryService inventoryService;  // тоже без шлюзов
    private final CombatService combatService;        // тоже без шлюзов

    public MazeWorldService(DataSource dataSource, InventoryService inventoryService, CombatService combatService) {
        this.dataSource = dataSource;
        this.inventoryService = inventoryService;
        this.combatService = combatService;
    }

    /**
     * Загружает существующий мир или создаёт новый (лабиринт + враги + предметы).
     */
    public WorldState loadOrCreateWorld() {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement checkMaze = conn.prepareStatement("SELECT id FROM maze WHERE id = 1");
            ResultSet rs = checkMaze.executeQuery();
            if (rs.next()) {
                // Мир уже существует – загружаем его
                Maze maze = loadMaze(conn, 1);
                List<Enemy> enemies = loadEnemies(conn, 1);
                List<GroundItem> items = loadItems(conn, 1);
                return new WorldState(maze, enemies, items);
            } else {
                // Генерация нового мира
                MazeGenerator.MazeData data = MazeGenerator.generate(40, 30);
                conn.setAutoCommit(false);
                int mazeId;
                try {
                    // Сохраняем лабиринт
                    PreparedStatement insertMaze = conn.prepareStatement(
                        "INSERT INTO maze (name, width, height) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                    );
                    insertMaze.setString(1, "Generated Maze");
                    insertMaze.setInt(2, 40);
                    insertMaze.setInt(3, 30);
                    insertMaze.executeUpdate();
                    ResultSet keys = insertMaze.getGeneratedKeys();
                    keys.next();
                    mazeId = keys.getInt(1);

                    // Ячейки
                    PreparedStatement insertCell = conn.prepareStatement(
                        "INSERT INTO cell (maze_id, x, y, type) VALUES (?, ?, ?, ?)"
                    );
                    for (Cell c : data.getCells()) {
                        insertCell.setInt(1, mazeId);
                        insertCell.setInt(2, c.getX());
                        insertCell.setInt(3, c.getY());
                        insertCell.setString(4, c.getType().name());
                        insertCell.addBatch();
                    }
                    insertCell.executeBatch();

                    // Враги
                    PreparedStatement insertEnemy = conn.prepareStatement(
                        "INSERT INTO enemy (name, enemy_type, health, attack, defense, maze_id, x, y) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                    for (Enemy e : data.getEnemies()) {
                        insertEnemy.setString(1, e.getName());
                        insertEnemy.setString(2, e.getEnemyType().name());
                        insertEnemy.setInt(3, e.getHealth());
                        insertEnemy.setInt(4, e.getAttack());
                        insertEnemy.setInt(5, e.getDefense());
                        insertEnemy.setInt(6, mazeId);
                        insertEnemy.setInt(7, e.getX());
                        insertEnemy.setInt(8, e.getY());
                        insertEnemy.addBatch();
                    }
                    insertEnemy.executeBatch();

                    // Предметы
                    PreparedStatement insertItem = conn.prepareStatement(
                        "INSERT INTO ground_item (name, type, attack_bonus, defense_bonus, heal_amount, maze_id, x, y) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                    for (GroundItem gi : data.getItems()) {
                        insertItem.setString(1, gi.getName());
                        insertItem.setString(2, gi.getType().name());
                        insertItem.setInt(3, gi.getAttackBonus());
                        insertItem.setInt(4, gi.getDefenseBonus());
                        insertItem.setInt(5, gi.getHealAmount());
                        insertItem.setInt(6, mazeId);
                        insertItem.setInt(7, gi.getX());
                        insertItem.setInt(8, gi.getY());
                        insertItem.addBatch();
                    }
                    insertItem.executeBatch();

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }

                // Загружаем сохранённый мир
                Maze savedMaze = loadMaze(conn, mazeId);
                List<Enemy> enemies = loadEnemies(conn, mazeId);
                List<GroundItem> items = loadItems(conn, mazeId);
                return new WorldState(savedMaze, enemies, items);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки/создания мира", e);
        }
    }

    private Maze loadMaze(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT id, name, width, height FROM maze WHERE id = ?");
        stmt.setInt(1, id);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            String name = rs.getString("name");
            int w = rs.getInt("width");
            int h = rs.getInt("height");
            List<Cell> cells = new ArrayList<>();
            PreparedStatement cellStmt = conn.prepareStatement("SELECT x, y, type FROM cell WHERE maze_id = ?");
            cellStmt.setInt(1, id);
            ResultSet cellRs = cellStmt.executeQuery();
            while (cellRs.next()) {
                cells.add(new Cell(
                    cellRs.getInt("x"),
                    cellRs.getInt("y"),
                    Cell.Type.valueOf(cellRs.getString("type"))
                ));
            }
            return new Maze(id, name, w, h, cells);
        }
        throw new RuntimeException("Лабиринт не найден");
    }

    private List<Enemy> loadEnemies(Connection conn, int mazeId) throws SQLException {
        String sql = "SELECT id, name, enemy_type, health, attack, defense, x, y FROM enemy WHERE maze_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, mazeId);
        ResultSet rs = stmt.executeQuery();
        List<Enemy> enemies = new ArrayList<>();
        while (rs.next()) {
            enemies.add(new Enemy(
                rs.getInt("id"),
                rs.getString("name"),
                Enemy.EnemyType.valueOf(rs.getString("enemy_type")),
                rs.getInt("health"),
                rs.getInt("attack"),
                rs.getInt("defense"),
                rs.getInt("x"),
                rs.getInt("y")
            ));
        }
        return enemies;
    }

    private List<GroundItem> loadItems(Connection conn, int mazeId) throws SQLException {
        String sql = "SELECT id, name, type, attack_bonus, defense_bonus, heal_amount, x, y FROM ground_item WHERE maze_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, mazeId);
        ResultSet rs = stmt.executeQuery();
        List<GroundItem> items = new ArrayList<>();
        while (rs.next()) {
            items.add(new GroundItem(
                rs.getInt("id"),
                rs.getString("name"),
                ItemType.valueOf(rs.getString("type")),
                rs.getInt("attack_bonus"),
                rs.getInt("defense_bonus"),
                rs.getInt("heal_amount"),
                rs.getInt("x"),
                rs.getInt("y")
            ));
        }
        return items;
    }

    /**
     * Загружает или создаёт игрока для переданного лабиринта.
     */
    public Player loadOrCreatePlayer(Maze maze) {
        String selectSql = "SELECT id, name, current_x, current_y, health, max_health FROM player WHERE maze_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setInt(1, maze.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Player(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("current_x"),
                    rs.getInt("current_y"),
                    rs.getInt("health"),
                    rs.getInt("max_health")
                );
            } else {
                // Создаём нового игрока
                String insertSql = "INSERT INTO player (name, maze_id, current_x, current_y, health, max_health) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setString(1, "Hero");
                    insertStmt.setInt(2, maze.getId());
                    insertStmt.setInt(3, 1);
                    insertStmt.setInt(4, 1);
                    insertStmt.setInt(5, 100);
                    insertStmt.setInt(6, 100);
                    insertStmt.executeUpdate();
                    ResultSet keys = insertStmt.getGeneratedKeys();
                    if (keys.next()) {
                        return new Player(keys.getInt(1), "Hero", 1, 1, 100, 100);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки/создания игрока", e);
        }
        throw new RuntimeException("Не удалось создать игрока");
    }

    /**
     * Попытка перемещения игрока. Возвращает результат: стена, враг, предмет или обычное движение.
     */
    public MoveResult movePlayer(Player player, Maze maze, List<Enemy> enemies, List<GroundItem> groundItems, int dx, int dy) {
        int newX = player.getX() + dx;
        int newY = player.getY() + dy;
        Cell target = maze.getCell(newX, newY);
        if (target == null || target.isWall()) {
            return MoveResult.blocked("Там стена!");
        }

        // Проверка врага
        Optional<Enemy> enemyOpt = enemies.stream()
                .filter(e -> e.getX() == newX && e.getY() == newY)
                .findFirst();
        if (enemyOpt.isPresent()) {
            return MoveResult.enemyEncounter(enemyOpt.get());
        }

        // Проверка предмета
        Optional<GroundItem> itemOpt = groundItems.stream()
                .filter(gi -> gi.getX() == newX && gi.getY() == newY)
                .findFirst();
        if (itemOpt.isPresent()) {
            GroundItem groundItem = itemOpt.get();
            // Подбираем предмет (прямой вызов InventoryService без шлюзов)
            inventoryService.pickUpGroundItem(groundItem, player, dataSource);
            groundItems.remove(groundItem);
            // Обновляем позицию игрока
            player.moveTo(newX, newY);
            updatePlayerPosition(player);
            return MoveResult.success("Вы подобрали " + groundItem.getName() + "!");
        }

        // Обычное движение
        player.moveTo(newX, newY);
        updatePlayerPosition(player);
        return MoveResult.success(null);
    }

    public void updatePlayerPosition(Player player) {
        String sql = "UPDATE player SET current_x = ?, current_y = ?, health = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, player.getX());
            stmt.setInt(2, player.getY());
            stmt.setInt(3, player.getHealth());
            stmt.setInt(4, player.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления позиции игрока", e);
        }
    }

    // ---- Вспомогательные классы (без изменений) ----
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