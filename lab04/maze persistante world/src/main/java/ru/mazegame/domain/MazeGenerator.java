package ru.mazegame.domain;

import java.util.*;

public class MazeGenerator {

    private static final double ENEMY_CHANCE = 0.05;
    private static final double ITEM_CHANCE = 0.08;

    public static MazeData generate(int width, int height) {
        Cell.Type[][] grid = new Cell.Type[height][width];
        for (int y = 0; y < height; y++) {
            Arrays.fill(grid[y], Cell.Type.WALL);
        }

        int startX = 1, startY = 1;
        grid[startY][startX] = Cell.Type.PATH;

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{startX, startY});
        int[][] dirs = {{0, -2}, {2, 0}, {0, 2}, {-2, 0}};
        Random rand = new Random();

        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            int cx = current[0], cy = current[1];
            List<int[]> neighbors = new ArrayList<>();

            for (int[] dir : dirs) {
                int nx = cx + dir[0], ny = cy + dir[1];
                if (nx > 0 && nx < width - 1 && ny > 0 && ny < height - 1
                        && grid[ny][nx] == Cell.Type.WALL) {
                    neighbors.add(new int[]{nx, ny, dir[0] / 2, dir[1] / 2});
                }
            }

            if (!neighbors.isEmpty()) {
                int[] chosen = neighbors.get(rand.nextInt(neighbors.size()));
                int nx = chosen[0], ny = chosen[1];
                int wallX = cx + chosen[2], wallY = cy + chosen[3];
                grid[wallY][wallX] = Cell.Type.PATH;
                grid[ny][nx] = Cell.Type.PATH;
                stack.push(new int[]{nx, ny});
            } else {
                stack.pop();
            }
        }

        grid[1][1] = Cell.Type.PATH;

        for (int y = 2; y < height - 2; y++) {
            for (int x = 2; x < width - 2; x++) {
                if (grid[y][x] == Cell.Type.WALL && rand.nextDouble() < 0.05) {
                    if (hasAdjacentPath(grid, x, y)) {
                        grid[y][x] = Cell.Type.PATH;
                    }
                }
            }
        }

        List<int[]> pathCandidates = new ArrayList<>();
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if ((x != 1 || y != 1) && grid[y][x] == Cell.Type.PATH) {
                    pathCandidates.add(new int[]{x, y});
                }
            }
        }
        int[] exitCell = pathCandidates.get(rand.nextInt(pathCandidates.size()));
        grid[exitCell[1]][exitCell[0]] = Cell.Type.EXIT;
        final int exitX = exitCell[0];
        final int exitY = exitCell[1];

        List<Cell> cells = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells.add(new Cell(x, y, grid[y][x]));
            }
        }

        List<Enemy> enemies = new ArrayList<>();
        List<GroundItem> items = new ArrayList<>();

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (x == exitX && y == exitY) continue; 
                if (grid[y][x] != Cell.Type.PATH) continue;

                if (rand.nextDouble() < ENEMY_CHANCE) {
                    enemies.add(createRandomEnemy(rand, x, y));
                } else if (rand.nextDouble() < ITEM_CHANCE) {
                    items.add(createRandomItem(rand, x, y));
                }
            }
        }

        ensureBossExists(enemies, grid, width, height, rand, exitX, exitY);

        return new MazeData(cells, enemies, items);
    }

    private static boolean hasAdjacentPath(Cell.Type[][] grid, int x, int y) {
        int[][] dirs = {{0,-1},{0,1},{-1,0},{1,0}};
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < grid[0].length && ny >= 0 && ny < grid.length
                    && (grid[ny][nx] == Cell.Type.PATH || grid[ny][nx] == Cell.Type.EXIT)) {
                return true;
            }
        }
        return false;
    }

    private static Enemy createRandomEnemy(Random rand, int x, int y) {
        double roll = rand.nextDouble();
        Enemy.EnemyType type;
        String name;
        int health, attack, defense;

        if (roll < 0.02) {
            type = Enemy.EnemyType.BOSS;
            name = "Демонический лорд";
            health = 80 + rand.nextInt(40);
            attack = 10 + rand.nextInt(8);
            defense = 3 + rand.nextInt(5);
        } else if (roll < 0.3) {
            type = Enemy.EnemyType.RAT;
            name = "Крыса";
            health = 10 + rand.nextInt(10);
            attack = 2 + rand.nextInt(3);
            defense = rand.nextInt(2);
        } else if (roll < 0.6) {
            type = Enemy.EnemyType.SKELETON;
            name = "Скелет";
            health = 20 + rand.nextInt(10);
            attack = 4 + rand.nextInt(3);
            defense = 1 + rand.nextInt(3);
        } else {
            type = Enemy.EnemyType.GOBLIN;
            name = "Гоблин";
            health = 15 + rand.nextInt(10);
            attack = 3 + rand.nextInt(4);
            defense = 1 + rand.nextInt(2);
        }
        return new Enemy(0, name, type, health, attack, defense, x, y);
    }

    private static GroundItem createRandomItem(Random rand, int x, int y) {
        ItemType[] types = {ItemType.WEAPON, ItemType.ARMOR, ItemType.POTION};
        ItemType type = types[rand.nextInt(types.length)];
        String name;
        int attackBonus = 0, defenseBonus = 0, healAmount = 0;

        switch (type) {
            case WEAPON -> {
                name = "Меч";
                attackBonus = 3 + rand.nextInt(5);
            }
            case ARMOR -> {
                name = "Кольчуга";
                defenseBonus = 2 + rand.nextInt(4);
            }
            case POTION -> {
                name = "Зелье здоровья";
                healAmount = 10 + rand.nextInt(15);
            }
            default -> throw new IllegalStateException();
        }
        return new GroundItem(0, name, type, attackBonus, defenseBonus, healAmount, x, y);
    }

    private static void ensureBossExists(List<Enemy> enemies, Cell.Type[][] grid, int width, int height,
                                         Random rand, int exitX, int exitY) {
        boolean hasBoss = enemies.stream().anyMatch(e -> e.getEnemyType() == Enemy.EnemyType.BOSS);
        if (hasBoss) return;

        if (!enemies.isEmpty()) {
            int index = rand.nextInt(enemies.size());
            Enemy old = enemies.get(index);
            Enemy boss = new Enemy(0, "Демонический лорд", Enemy.EnemyType.BOSS,
                    80 + rand.nextInt(40), 10 + rand.nextInt(8), 3 + rand.nextInt(5),
                    old.getX(), old.getY());
            enemies.set(index, boss);
        } else {
            List<int[]> pathCells = new ArrayList<>();
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    if (x == exitX && y == exitY) continue; // не ставим на выход
                    if (grid[y][x] == Cell.Type.PATH) {
                        pathCells.add(new int[]{x, y});
                    }
                }
            }
            if (!pathCells.isEmpty()) {
                int[] cell = pathCells.get(rand.nextInt(pathCells.size()));
                Enemy boss = new Enemy(0, "Демонический лорд", Enemy.EnemyType.BOSS,
                        80 + rand.nextInt(40), 10 + rand.nextInt(8), 3 + rand.nextInt(5),
                        cell[0], cell[1]);
                enemies.add(boss);
            }
        }
    }

    public static class MazeData {
        private final List<Cell> cells;
        private final List<Enemy> enemies;
        private final List<GroundItem> items;

        public MazeData(List<Cell> cells, List<Enemy> enemies, List<GroundItem> items) {
            this.cells = cells;
            this.enemies = enemies;
            this.items = items;
        }

        public List<Cell> getCells() { return cells; }
        public List<Enemy> getEnemies() { return enemies; }
        public List<GroundItem> getItems() { return items; }
    }
}