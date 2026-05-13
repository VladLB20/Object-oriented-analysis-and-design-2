package ru.mazegame.domain;

import java.util.List;


public class Maze {
    private final int id;
    private final String name;
    private final int width;
    private final int height;
    private final Cell[][] grid;

    public Maze(int id, String name, int width, int height, List<Cell> cells) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
        this.grid = new Cell[height][width];
        for (Cell c : cells) {
            grid[c.getY()][c.getX()] = c;
        }
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public Cell getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return grid[y][x];
    }
}