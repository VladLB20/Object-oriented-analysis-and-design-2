package ru.mazegame.service;

import ru.mazegame.domain.Enemy;
import ru.mazegame.domain.Player;
import ru.mazegame.gateway.EnemyGateway;

public class CombatService {
    private final EnemyGateway enemyGateway;
    private final InventoryService inventoryService;

    public CombatService(EnemyGateway enemyGateway, InventoryService inventoryService) {
        this.enemyGateway = enemyGateway;
        this.inventoryService = inventoryService;
    }

    public CombatResult fight(Player player, Enemy enemy) {
        int playerAttack = 5 + inventoryService.getEquippedAttackBonus(player);
        int playerDefense = inventoryService.getEquippedDefenseBonus(player);

        while (player.isAlive() && enemy.isAlive()) {
            int damageToEnemy = Math.max(0, playerAttack - enemy.getDefense());
            enemy.takeDamage(damageToEnemy);
            if (!enemy.isAlive()) {
                enemyGateway.removeEnemy(enemy.getId());
                return new CombatResult(true, enemy.getName());
            }

            int damageToPlayer = Math.max(0, enemy.getAttack() - playerDefense);
            player.takeDamage(damageToPlayer);
        }

        return new CombatResult(false, enemy.getName());
    }

    public static class CombatResult {
        private final boolean playerWon;
        private final String enemyName;

        public CombatResult(boolean playerWon, String enemyName) {
            this.playerWon = playerWon;
            this.enemyName = enemyName;
        }

        public boolean playerWon() {
            return playerWon;
        }

        public String getEnemyName() {
            return enemyName;
        }
    }
}