package ru.mazegame.service;

import ru.mazegame.domain.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CombatService {
    private final DataSource dataSource;
    private final InventoryService inventoryService;   // тоже без шлюзов

    public CombatService(DataSource dataSource, InventoryService inventoryService) {
        this.dataSource = dataSource;
        this.inventoryService = inventoryService;
    }

    /**
     * Проводит простой пошаговый бой между игроком и врагом.
     * Игрок атакует первым, затем враг, пока один не погибнет.
     * При победе игрока враг удаляется из БД.
     */
    public CombatResult fight(Player player, Enemy enemy) {
        int playerAttack = 5 + inventoryService.getEquippedAttackBonus(player);
        int playerDefense = inventoryService.getEquippedDefenseBonus(player);

        while (player.isAlive() && enemy.isAlive()) {
            // Ход игрока
            int damageToEnemy = Math.max(0, playerAttack - enemy.getDefense());
            enemy.takeDamage(damageToEnemy);
            if (!enemy.isAlive()) {
                // Удаляем врага напрямую из БД
                String sql = "DELETE FROM enemy WHERE id = ?";
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, enemy.getId());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Ошибка удаления врага", e);
                }
                return new CombatResult(true, enemy.getName());
            }

            // Ход врага
            int damageToPlayer = Math.max(0, enemy.getAttack() - playerDefense);
            player.takeDamage(damageToPlayer);
        }

        // Игрок погиб
        return new CombatResult(false, enemy.getName());
    }

    public static class CombatResult {
        private final boolean playerWon;
        private final String enemyName;

        public CombatResult(boolean playerWon, String enemyName) {
            this.playerWon = playerWon;
            this.enemyName = enemyName;
        }

        public boolean playerWon() { return playerWon; }
        public String getEnemyName() { return enemyName; }
    }
}