package ru.mazegame.gateway;

import ru.mazegame.domain.InventoryItem;

import java.util.List;

public interface InventoryGateway {
    List<InventoryItem> findItemsByPlayerId(int playerId);
    void addItem(InventoryItem item, int playerId);
    void removeItem(int itemId);
    void equipItem(int itemId, boolean equip);
}