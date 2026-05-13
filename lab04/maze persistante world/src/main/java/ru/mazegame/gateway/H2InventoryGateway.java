package ru.mazegame.gateway;

import ru.mazegame.domain.InventoryItem;
import ru.mazegame.domain.ItemType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class H2InventoryGateway implements InventoryGateway {
    private final DataSource dataSource;

    public H2InventoryGateway(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<InventoryItem> findItemsByPlayerId(int playerId) {
        List<InventoryItem> items = new ArrayList<>();
        String sql = "SELECT id, player_id, item_name, item_type, attack_bonus, defense_bonus, heal_amount, equipped " +
                     "FROM inventory_item WHERE player_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(new InventoryItem(
                        rs.getInt("id"),
                        rs.getInt("player_id"),
                        rs.getString("item_name"),
                        ItemType.valueOf(rs.getString("item_type")),
                        rs.getInt("attack_bonus"),
                        rs.getInt("defense_bonus"),
                        rs.getInt("heal_amount"),
                        rs.getBoolean("equipped")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки инвентаря", e);
        }
        return items;
    }

    @Override
    public void addItem(InventoryItem item, int playerId) {
        String sql = "INSERT INTO inventory_item (player_id, item_name, item_type, attack_bonus, defense_bonus, heal_amount, equipped) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playerId);
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getType().name());
            stmt.setInt(4, item.getAttackBonus());
            stmt.setInt(5, item.getDefenseBonus());
            stmt.setInt(6, item.getHealAmount());
            stmt.setBoolean(7, item.isEquipped());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка добавления предмета в инвентарь", e);
        }
    }

    @Override
    public void removeItem(int itemId) {
        String sql = "DELETE FROM inventory_item WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления предмета из инвентаря", e);
        }
    }

    @Override
    public void equipItem(int itemId, boolean equip) {
        String sql = "UPDATE inventory_item SET equipped = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, equip);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка изменения экипировки предмета", e);
        }
    }
}