package ru.mazegame.domain;

public class Cell {
    public enum Type { WALL, PATH, EXIT }   

    private final int x;
    private final int y;
    private final Type type;

    public Cell(int x, int y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public Type getType() { return type; }

    public boolean isWall() {
        return type == Type.WALL;   
    }
}