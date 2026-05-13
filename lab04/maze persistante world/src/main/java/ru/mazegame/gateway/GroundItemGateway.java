package ru.mazegame.gateway;

import ru.mazegame.domain.GroundItem;

import java.util.List;

public interface GroundItemGateway {
    List<GroundItem> findItemsByMazeId(int mazeId);
    void removeItem(int itemId);
    void createItem(GroundItem item, int mazeId);
}