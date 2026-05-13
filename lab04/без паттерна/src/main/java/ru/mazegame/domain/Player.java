package ru.mazegame.domain;

public class Player {
    private final int id;
    private final String name;
    private int x;
    private int y;
    private int health;
    private final int maxHealth;

    public Player(int id, String name, int x, int y) {
        this(id, name, x, y, 100, 100);
    }

    public Player(int id, String name, int x, int y, int health, int maxHealth) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.maxHealth = maxHealth;
        this.health = health;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }

    public void moveTo(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
    }

    public void heal(int amount) {
        health += amount;
        if (health > maxHealth) health = maxHealth;
    }

    public boolean isAlive() {
        return health > 0;
    }
}