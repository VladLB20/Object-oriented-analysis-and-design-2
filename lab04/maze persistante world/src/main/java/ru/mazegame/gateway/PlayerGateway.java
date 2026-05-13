package ru.mazegame.gateway;

import ru.mazegame.domain.Player;

import java.util.Optional;


public interface PlayerGateway {


    Optional<Player> findPlayerByMazeId(int mazeId);

    void updatePlayerPosition(Player player);

    Player createPlayer(String name, int mazeId, int x, int y);
}