package ru.mazegame.domain;

public class GroundItem {
    private final int id;
    private final String name;
    private final ItemType type;
    private final int attackBonus;
    private final int defenseBonus;
    private final int healAmount;
    private final int x;
    private final int y;

    public GroundItem(int id, String name, ItemType type, int attackBonus,
                      int defenseBonus, int healAmount, int x, int y) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
        this.healAmount = healAmount;
        this.x = x;
        this.y = y;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public ItemType getType() { return type; }
    public int getAttackBonus() { return attackBonus; }
    public int getDefenseBonus() { return defenseBonus; }
    public int getHealAmount() { return healAmount; }
    public int getX() { return x; }
    public int getY() { return y; }
}