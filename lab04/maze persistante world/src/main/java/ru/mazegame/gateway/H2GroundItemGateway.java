package ru.mazegame.gateway;

import ru.mazegame.domain.GroundItem;
import ru.mazegame.domain.ItemType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class H2GroundItemGateway implements GroundItemGateway {
    private final DataSource dataSource;

    public H2GroundItemGateway(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<GroundItem> findItemsByMazeId(int mazeId) {
        List<GroundItem> items = new ArrayList<>();
        String sql = "SELECT id, name, type, attack_bonus, defense_bonus, heal_amount, x, y " +
                     "FROM ground_item WHERE maze_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mazeId);
            ResultSet rs = stmt.executeQuery();
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
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки наземных предметов", e);
        }
        return items;
    }

    @Override
    public void removeItem(int itemId) {
        String sql = "DELETE FROM ground_item WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления наземного предмета", e);
        }
    }

    @Override
    public void createItem(GroundItem item, int mazeId) {
        String sql = "INSERT INTO ground_item (name, type, attack_bonus, defense_bonus, heal_amount, maze_id, x, y) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getType().name());
            stmt.setInt(3, item.getAttackBonus());
            stmt.setInt(4, item.getDefenseBonus());
            stmt.setInt(5, item.getHealAmount());
            stmt.setInt(6, mazeId);
            stmt.setInt(7, item.getX());
            stmt.setInt(8, item.getY());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания наземного предмета", e);
        }
    }
}