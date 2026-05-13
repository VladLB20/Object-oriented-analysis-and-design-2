package ru.mazegame.gateway;

import ru.mazegame.domain.Cell;
import ru.mazegame.domain.Maze;

import java.util.List;
import java.util.Optional;


public interface MazeGateway {

    Optional<Maze> findMazeById(int id);

    List<Cell> findCellsByMazeId(int mazeId);

    void createMaze(Maze maze, List<Cell> cells);
}