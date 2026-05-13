package ru.mazegame.gateway;

import ru.mazegame.domain.Cell;
import ru.mazegame.domain.Maze;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2MazeGateway implements MazeGateway {
    private final DataSource dataSource;

    public H2MazeGateway(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Maze> findMazeById(int id) {
        String sql = "SELECT id, name, width, height FROM maze WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                List<Cell> cells = findCellsByMazeId(id);
                Maze maze = new Maze(rs.getInt("id"), rs.getString("name"),
                        rs.getInt("width"), rs.getInt("height"), cells);
                return Optional.of(maze);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки лабиринта", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Cell> findCellsByMazeId(int mazeId) {
        List<Cell> cells = new ArrayList<>();
        String sql = "SELECT x, y, type FROM cell WHERE maze_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mazeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Cell.Type type = Cell.Type.valueOf(rs.getString("type"));
                cells.add(new Cell(rs.getInt("x"), rs.getInt("y"), type));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки ячеек", e);
        }
        return cells;
    }

    @Override
    public void createMaze(Maze maze, List<Cell> cells) {
        String insertMaze = "INSERT INTO maze (name, width, height) VALUES (?, ?, ?)";
        String insertCell = "INSERT INTO cell (maze_id, x, y, type) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtMaze = conn.prepareStatement(insertMaze, Statement.RETURN_GENERATED_KEYS)) {
                stmtMaze.setString(1, maze.getName());
                stmtMaze.setInt(2, maze.getWidth());
                stmtMaze.setInt(3, maze.getHeight());
                stmtMaze.executeUpdate();
                ResultSet keys = stmtMaze.getGeneratedKeys();
                if (keys.next()) {
                    int mazeId = keys.getInt(1);
                    try (PreparedStatement stmtCell = conn.prepareStatement(insertCell)) {
                        for (Cell cell : cells) {
                            stmtCell.setInt(1, mazeId);
                            stmtCell.setInt(2, cell.getX());
                            stmtCell.setInt(3, cell.getY());
                            stmtCell.setString(4, cell.getType().name());
                            stmtCell.addBatch();
                        }
                        stmtCell.executeBatch();
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания лабиринта", e);
        }
    }
}