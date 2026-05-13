package ru.mazegame.database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS maze (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    width INT NOT NULL,
                    height INT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cell (
                    maze_id INT NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    type VARCHAR(10) NOT NULL,
                    PRIMARY KEY (maze_id, x, y),
                    FOREIGN KEY (maze_id) REFERENCES maze(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) NOT NULL,
                    maze_id INT NOT NULL,
                    current_x INT NOT NULL,
                    current_y INT NOT NULL,
                    health INT NOT NULL DEFAULT 100,
                    max_health INT NOT NULL DEFAULT 100,
                    FOREIGN KEY (maze_id) REFERENCES maze(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS enemy (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) NOT NULL,
                    enemy_type VARCHAR(20) NOT NULL,
                    health INT NOT NULL,
                    attack INT NOT NULL,
                    defense INT NOT NULL,
                    maze_id INT NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    FOREIGN KEY (maze_id) REFERENCES maze(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ground_item (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) NOT NULL,
                    type VARCHAR(20) NOT NULL,
                    attack_bonus INT DEFAULT 0,
                    defense_bonus INT DEFAULT 0,
                    heal_amount INT DEFAULT 0,
                    maze_id INT NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    FOREIGN KEY (maze_id) REFERENCES maze(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory_item (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_id INT NOT NULL,
                    item_name VARCHAR(50) NOT NULL,
                    item_type VARCHAR(20) NOT NULL,
                    attack_bonus INT DEFAULT 0,
                    defense_bonus INT DEFAULT 0,
                    heal_amount INT DEFAULT 0,
                    equipped BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (player_id) REFERENCES player(id)
                )
            """);
        }
    }
}