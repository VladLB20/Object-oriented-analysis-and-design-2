package ru.mazegame.gateway;

import ru.mazegame.domain.Player;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

public class H2PlayerGateway implements PlayerGateway {
    private final DataSource dataSource;

    public H2PlayerGateway(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Player> findPlayerByMazeId(int mazeId) {
        String sql = "SELECT id, name, current_x, current_y, health, max_health FROM player WHERE maze_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mazeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new Player(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("current_x"),
                        rs.getInt("current_y"),
                        rs.getInt("health"),
                        rs.getInt("max_health")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки игрока", e);
        }
        return Optional.empty();
    }

    @Override
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

    @Override
    public Player createPlayer(String name, int mazeId, int x, int y) {
        String sql = "INSERT INTO player (name, maze_id, current_x, current_y, health, max_health) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setInt(2, mazeId);
            stmt.setInt(3, x);
            stmt.setInt(4, y);
            stmt.setInt(5, 100);  // начальное здоровье
            stmt.setInt(6, 100);  // максимальное здоровье
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                return new Player(keys.getInt(1), name, x, y, 100, 100);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания игрока", e);
        }
        throw new RuntimeException("Не удалось создать игрока");
    }
}