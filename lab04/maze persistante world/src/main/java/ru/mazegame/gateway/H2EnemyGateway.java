package ru.mazegame.gateway;

import ru.mazegame.domain.Enemy;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class H2EnemyGateway implements EnemyGateway {
    private final DataSource dataSource;

    public H2EnemyGateway(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Enemy> findEnemiesByMazeId(int mazeId) {
        List<Enemy> enemies = new ArrayList<>();
        String sql = "SELECT id, name, enemy_type, health, attack, defense, x, y FROM enemy WHERE maze_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mazeId);
            ResultSet rs = stmt.executeQuery();
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
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки врагов", e);
        }
        return enemies;
    }

    @Override
    public void updateEnemy(Enemy enemy) {
        String sql = "UPDATE enemy SET health = ?, x = ?, y = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enemy.getHealth());
            stmt.setInt(2, enemy.getX());
            stmt.setInt(3, enemy.getY());
            stmt.setInt(4, enemy.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления врага", e);
        }
    }

    @Override
    public void removeEnemy(int enemyId) {
        String sql = "DELETE FROM enemy WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enemyId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления врага", e);
        }
    }

    @Override
    public void createEnemy(Enemy enemy, int mazeId) {
        String sql = "INSERT INTO enemy (name, enemy_type, health, attack, defense, maze_id, x, y) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, enemy.getName());
            stmt.setString(2, enemy.getEnemyType().name());
            stmt.setInt(3, enemy.getHealth());
            stmt.setInt(4, enemy.getAttack());
            stmt.setInt(5, enemy.getDefense());
            stmt.setInt(6, mazeId);
            stmt.setInt(7, enemy.getX());
            stmt.setInt(8, enemy.getY());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания врага", e);
        }
    }
}