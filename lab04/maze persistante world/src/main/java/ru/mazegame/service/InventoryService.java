package ru.mazegame.service;

import ru.mazegame.domain.*;
import ru.mazegame.gateway.GroundItemGateway;
import ru.mazegame.gateway.InventoryGateway;

import java.util.*;

public class InventoryService {
    private final InventoryGateway inventoryGateway;

    public InventoryService(InventoryGateway inventoryGateway) {
        this.inventoryGateway = inventoryGateway;
    }

  
    public void pickUpGroundItem(GroundItem groundItem, Player player, GroundItemGateway groundItemGateway) {
        InventoryItem item = new InventoryItem(
                groundItem.getName(),
                groundItem.getType(),
                groundItem.getAttackBonus(),
                groundItem.getDefenseBonus(),
                groundItem.getHealAmount(),
                false
        );
        inventoryGateway.addItem(item, player.getId());
        groundItemGateway.removeItem(groundItem.getId());
    }

    public List<InventoryItem> getInventory(Player player) {
        return inventoryGateway.findItemsByPlayerId(player.getId());
    }

    public void equip(InventoryItem item) {
        unequipSlot(item.getType(), item);
        inventoryGateway.equipItem(item.getId(), true);
    }

    public void unequip(InventoryItem item) {
        inventoryGateway.equipItem(item.getId(), false);
    }

    private void unequipSlot(ItemType type, InventoryItem except) {
        List<InventoryItem> all = inventoryGateway.findItemsByPlayerId(except.getPlayerId());
        for (InventoryItem it : all) {
            if (it.getType() == type && it.isEquipped() && it.getId() != except.getId()) {
                inventoryGateway.equipItem(it.getId(), false);
            }
        }
    }

   
    public int getEquippedAttackBonus(Player player) {
        return inventoryGateway.findItemsByPlayerId(player.getId()).stream()
                .filter(InventoryItem::isEquipped)
                .mapToInt(InventoryItem::getAttackBonus)
                .sum();
    }

    public int getEquippedDefenseBonus(Player player) {
        return inventoryGateway.findItemsByPlayerId(player.getId()).stream()
                .filter(InventoryItem::isEquipped)
                .mapToInt(InventoryItem::getDefenseBonus)
                .sum();
    }

  
    public void usePotion(InventoryItem potion, Player player) {
        if (potion.getType() != ItemType.POTION) return;
        player.heal(potion.getHealAmount());
        inventoryGateway.removeItem(potion.getId());
    }

    public void addItem(InventoryItem item, int playerId) {
        inventoryGateway.addItem(item, playerId);
    }

    public void removeItem(int itemId) {
        inventoryGateway.removeItem(itemId);
    }
}