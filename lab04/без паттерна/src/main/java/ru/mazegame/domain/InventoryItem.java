package ru.mazegame.domain;


public class InventoryItem {
    private final int id;
    private final int playerId;
    private final String name;
    private final ItemType type;
    private final int attackBonus;
    private final int defenseBonus;
    private final int healAmount;
    private boolean equipped;

    public InventoryItem(int id, int playerId, String name, ItemType type,
                         int attackBonus, int defenseBonus, int healAmount,
                         boolean equipped) {
        this.id = id;
        this.playerId = playerId;
        this.name = name;
        this.type = type;
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
        this.healAmount = healAmount;
        this.equipped = equipped;
    }

    public InventoryItem(String name, ItemType type, int attackBonus,
                         int defenseBonus, int healAmount, boolean equipped) {
        this(0, 0, name, type, attackBonus, defenseBonus, healAmount, equipped);
    }

    public int getId() { return id; }
    public int getPlayerId() { return playerId; }
    public String getName() { return name; }
    public ItemType getType() { return type; }
    public int getAttackBonus() { return attackBonus; }
    public int getDefenseBonus() { return defenseBonus; }
    public int getHealAmount() { return healAmount; }
    public boolean isEquipped() { return equipped; }

    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }
}