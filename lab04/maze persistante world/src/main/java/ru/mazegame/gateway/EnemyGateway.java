package ru.mazegame.gateway;

import ru.mazegame.domain.Enemy;

import java.util.List;

public interface EnemyGateway {
    List<Enemy> findEnemiesByMazeId(int mazeId);
    void updateEnemy(Enemy enemy);
    void removeEnemy(int enemyId);
    void createEnemy(Enemy enemy, int mazeId);
}