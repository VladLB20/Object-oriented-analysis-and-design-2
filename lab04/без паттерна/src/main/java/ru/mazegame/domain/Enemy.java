package ru.mazegame.domain;

public class Enemy {
    private final int id;
    private final String name;
    private final EnemyType enemyType;   // новый атрибут
    private int health;
    private final int maxHealth;
    private final int attack;
    private final int defense;
    private final int x;
    private final int y;

    public enum EnemyType {
    RAT, SKELETON, GOBLIN, BOSS
    }

    public Enemy(int id, String name, EnemyType enemyType, int health, int attack, int defense, int x, int y) {
        this.id = id;
        this.name = name;
        this.enemyType = enemyType;
        this.maxHealth = health;
        this.health = health;
        this.attack = attack;
        this.defense = defense;
        this.x = x;
        this.y = y;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public EnemyType getEnemyType() { return enemyType; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getX() { return x; }
    public int getY() { return y; }

    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
    }

    public boolean isAlive() {
        return health > 0;
    }
}