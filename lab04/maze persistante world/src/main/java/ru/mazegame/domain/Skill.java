package ru.mazegame.domain;

public class Skill {
    private final String name;
    private final int attackBonus;
    private final int healAmount;
    private final boolean singleUse;

    public Skill(String name, int attackBonus, int healAmount, boolean singleUse) {
        this.name = name;
        this.attackBonus = attackBonus;
        this.healAmount = healAmount;
        this.singleUse = singleUse;
    }

    public String getName() { return name; }
    public int getAttackBonus() { return attackBonus; }
    public int getHealAmount() { return healAmount; }
    public boolean isSingleUse() { return singleUse; }
}