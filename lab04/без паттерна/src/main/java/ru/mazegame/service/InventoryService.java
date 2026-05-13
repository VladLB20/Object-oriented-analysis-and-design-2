package ru.mazegame.service;

import ru.mazegame.domain.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryService {
    private final DataSource dataSource;

    public InventoryService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Подобрать предмет с пола – переносит его в инвентарь игрока.
     */
    public void pickUpGroundItem(GroundItem groundItem, Player player, DataSource ds) {
        String addSql = "INSERT INTO inventory_item (player_id, item_name, item_type, attack_bonus, defense_bonus, heal_amount, equipped) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String delSql = "DELETE FROM ground_item WHERE id = ?";
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement addStmt = conn.prepareStatement(addSql)) {
                addStmt.setInt(1, player.getId());
                addStmt.setString(2, groundItem.getName());
                addStmt.setString(3, groundItem.getType().name());
                addStmt.setInt(4, groundItem.getAttackBonus());
                addStmt.setInt(5, groundItem.getDefenseBonus());
                addStmt.setInt(6, groundItem.getHealAmount());
                addStmt.setBoolean(7, false);
                addStmt.executeUpdate();
            }
            try (PreparedStatement delStmt = conn.prepareStatement(delSql)) {
                delStmt.setInt(1, groundItem.getId());
                delStmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при подборе предмета", e);
        }
    }

    /**
     * Получить все предметы инвентаря игрока.
     */
    public List<InventoryItem> getInventory(Player player) {
        String sql = "SELECT id, player_id, item_name, item_type, attack_bonus, defense_bonus, heal_amount, equipped " +
                     "FROM inventory_item WHERE player_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, player.getId());
            ResultSet rs = stmt.executeQuery();
            List<InventoryItem> items = new ArrayList<>();
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
            return items;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки инвентаря", e);
        }
    }

    /**
     * Надеть предмет (снять все того же слота и экипировать выбранный).
     */
    public void equip(InventoryItem item) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            // Снять все того же типа
            try (PreparedStatement unequipStmt = conn.prepareStatement(
                    "UPDATE inventory_item SET equipped = false WHERE player_id = ? AND item_type = ?")) {
                unequipStmt.setInt(1, item.getPlayerId());
                unequipStmt.setString(2, item.getType().name());
                unequipStmt.executeUpdate();
            }
            // Надеть выбранный
            try (PreparedStatement equipStmt = conn.prepareStatement(
                    "UPDATE inventory_item SET equipped = true WHERE id = ?")) {
                equipStmt.setInt(1, item.getId());
                equipStmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка экипировки", e);
        }
    }

    /**
     * Снять предмет.
     */
    public void unequip(InventoryItem item) {
        String sql = "UPDATE inventory_item SET equipped = false WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка снятия предмета", e);
        }
    }

    /**
     * Суммарный бонус атаки от экипированных предметов.
     */
    public int getEquippedAttackBonus(Player player) {
        String sql = "SELECT COALESCE(SUM(attack_bonus), 0) FROM inventory_item WHERE player_id = ? AND equipped = true";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, player.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка подсчёта бонуса атаки", e);
        }
        return 0;
    }

    /**
     * Суммарный бонус защиты от экипированных предметов.
     */
    public int getEquippedDefenseBonus(Player player) {
        String sql = "SELECT COALESCE(SUM(defense_bonus), 0) FROM inventory_item WHERE player_id = ? AND equipped = true";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, player.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка подсчёта бонуса защиты", e);
        }
        return 0;
    }

    /**
     * Использовать зелье (лечит и удаляет предмет).
     */
    public void usePotion(InventoryItem potion, Player player) {
        if (potion.getType() != ItemType.POTION) return;
        String delSql = "DELETE FROM inventory_item WHERE id = ?";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delStmt = conn.prepareStatement(delSql)) {
                delStmt.setInt(1, potion.getId());
                delStmt.executeUpdate();
            }
            player.heal(potion.getHealAmount());
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка использования зелья", e);
        }
    }

    /**
     * Добавить предмет в инвентарь (используется для награды после боя).
     */
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
            throw new RuntimeException("Ошибка добавления предмета", e);
        }
    }

    /**
     * Удалить предмет из инвентаря (например, использованное умение).
     */
    public void removeItem(int itemId) {
        String sql = "DELETE FROM inventory_item WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления предмета", e);
        }
    }
}