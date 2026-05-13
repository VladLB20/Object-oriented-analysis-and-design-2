package ru.mazegame.ui;

import ru.mazegame.domain.*;
import ru.mazegame.gateway.EnemyGateway;
import ru.mazegame.gateway.GroundItemGateway;
import ru.mazegame.gateway.PlayerGateway;
import ru.mazegame.service.CombatService;
import ru.mazegame.service.InventoryService;
import ru.mazegame.service.MazeWorldService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MazeGameGUI extends JFrame {

    private final MazeWorldService worldService;
    private final CombatService combatService;
    private final InventoryService inventoryService;
    private final EnemyGateway enemyGateway;
    private final PlayerGateway playerGateway;
    private final GroundItemGateway groundItemGateway;

    private MazeWorldService.WorldState worldState;
    private Maze maze;
    private List<Enemy> enemies;
    private List<GroundItem> groundItems;
    private Player player;

    private static final int CELL_SIZE = 25;
    private MazePanel panel;
    private JScrollPane scrollPane;

    public MazeGameGUI(MazeWorldService worldService,
                       CombatService combatService,
                       InventoryService inventoryService,
                       EnemyGateway enemyGateway,
                       PlayerGateway playerGateway,
                       GroundItemGateway groundItemGateway) {
        this.worldService = worldService;
        this.combatService = combatService;
        this.inventoryService = inventoryService;
        this.enemyGateway = enemyGateway;
        this.playerGateway = playerGateway;
        this.groundItemGateway = groundItemGateway;

        this.worldState = worldService.loadOrCreateWorld();
        this.maze = worldState.getMaze();
        this.enemies = worldState.getEnemies();
        this.groundItems = worldState.getItems();
        this.player = worldService.loadOrCreatePlayer(maze);

        setTitle("Лабиринт с постоянным миром [Roguelike]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new MazePanel();
        panel.setPreferredSize(new Dimension(maze.getWidth() * CELL_SIZE,
                                             maze.getHeight() * CELL_SIZE));

        scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        add(scrollPane);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        SwingUtilities.invokeLater(this::scrollToPlayer);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP, KeyEvent.VK_W -> doMove(0, -1);
                    case KeyEvent.VK_DOWN, KeyEvent.VK_S -> doMove(0, 1);
                    case KeyEvent.VK_LEFT, KeyEvent.VK_A -> doMove(-1, 0);
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> doMove(1, 0);
                    case KeyEvent.VK_I -> openInventoryDialog();
                }
            }
        });
        setFocusable(true);
    }

    private void doMove(int dx, int dy) {
        if (!player.isAlive()) return;

        MazeWorldService.MoveResult result =
                worldService.movePlayer(player, maze, enemies, groundItems, dx, dy);

        if (!result.isSuccess() && result.getEncounteredEnemy().isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            if (result.getMessage() != null) {
                JOptionPane.showMessageDialog(this, result.getMessage());
            }
            return;
        }

        if (result.getEncounteredEnemy().isPresent()) {
            Enemy enemy = result.getEncounteredEnemy().get();

            List<InventoryItem> skills = inventoryService.getInventory(player).stream()
                    .filter(i -> i.getType() == ItemType.SKILL)
                    .collect(Collectors.toList());

            BattleDialog dialog = new BattleDialog(this, player, enemy, skills, inventoryService);
            dialog.setVisible(true);

            BattleDialog.BattleResult battleResult = dialog.getBattleResult();
            if (battleResult == BattleDialog.BattleResult.PLAYER_WON) {
                enemies.remove(enemy);
                enemyGateway.removeEnemy(enemy.getId());

                player.moveTo(enemy.getX(), enemy.getY());
                playerGateway.updatePlayerPosition(player);
            }
            else if (battleResult == BattleDialog.BattleResult.PLAYER_DIED) {
                JOptionPane.showMessageDialog(this, "Вы погибли...");
                restartWorld();
            }
            panel.repaint();
            scrollToPlayer();
            return;
        }

        int newX = player.getX(); 
        int newY = player.getY();
        if (maze.getCell(newX, newY).getType() == Cell.Type.EXIT) {
            boolean hasKey = inventoryService.getInventory(player).stream()
                    .anyMatch(i -> i.getType() == ItemType.KEY);
            if (hasKey) {
                JOptionPane.showMessageDialog(this, "Вы выбрались из лабиринта! Победа!");
                System.exit(0);
            } else {
                JOptionPane.showMessageDialog(this, "Выход заперт. Нужен Ключ выхода, который носит босс.");
            }
        }

        if (result.getMessage() != null && !result.getMessage().isEmpty()) {
            JOptionPane.showMessageDialog(this, result.getMessage());
        }

        panel.repaint();
        scrollToPlayer();
    }

    private void restartWorld() {
        worldState = worldService.loadOrCreateWorld();
        maze = worldState.getMaze();
        enemies = worldState.getEnemies();
        groundItems = worldState.getItems();
        player = worldService.loadOrCreatePlayer(maze);
        panel.repaint();
        scrollToPlayer();
    }

    private void scrollToPlayer() {
        int centerX = player.getX() * CELL_SIZE + CELL_SIZE / 2;
        int centerY = player.getY() * CELL_SIZE + CELL_SIZE / 2;
        JViewport viewport = scrollPane.getViewport();
        Rectangle viewRect = viewport.getViewRect();
        int newX = Math.max(0, centerX - viewRect.width / 2);
        int newY = Math.max(0, centerY - viewRect.height / 2);
        newX = Math.min(newX, panel.getWidth() - viewRect.width);
        newY = Math.min(newY, panel.getHeight() - viewRect.height);
        viewport.setViewPosition(new Point(newX, newY));
    }

    private void openInventoryDialog() {
        List<InventoryItem> inventory = inventoryService.getInventory(player);
        JDialog dialog = new JDialog(this, "Инвентарь", true);
        dialog.setLayout(new BorderLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (InventoryItem item : inventory) {
            String status = item.isEquipped() ? " [Экипировано]" : "";
            listModel.addElement(item.getName() + " (" + item.getType() + ")" + status);
        }
        JList<String> itemList = new JList<>(listModel);
        dialog.add(new JScrollPane(itemList), BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton equipBtn = new JButton("Надеть");
        JButton unequipBtn = new JButton("Снять");
        JButton useBtn = new JButton("Использовать");
        buttons.add(equipBtn);
        buttons.add(unequipBtn);
        buttons.add(useBtn);
        dialog.add(buttons, BorderLayout.SOUTH);

        equipBtn.addActionListener(e -> {
            int idx = itemList.getSelectedIndex();
            if (idx >= 0) {
                InventoryItem selected = inventory.get(idx);
                if (selected.getType() == ItemType.KEY) {
                    JOptionPane.showMessageDialog(dialog, "Ключ нельзя надеть.");
                } else {
                    inventoryService.equip(selected);
                    dialog.dispose();
                    openInventoryDialog(); 
                }
            }
        });

        unequipBtn.addActionListener(e -> {
            int idx = itemList.getSelectedIndex();
            if (idx >= 0) {
                InventoryItem selected = inventory.get(idx);
                if (selected.getType() == ItemType.KEY) {
                    JOptionPane.showMessageDialog(dialog, "Ключ нельзя снять.");
                } else {
                    inventoryService.unequip(selected);
                    dialog.dispose();
                    openInventoryDialog();
                }
            }
        });

        useBtn.addActionListener(e -> {
            int idx = itemList.getSelectedIndex();
            if (idx >= 0) {
                InventoryItem selected = inventory.get(idx);
                switch (selected.getType()) {
                    case POTION:
                        inventoryService.usePotion(selected, player);
                        dialog.dispose();
                        openInventoryDialog();
                        break;
                    case SKILL:
                        JOptionPane.showMessageDialog(dialog, "Умения используются в бою.");
                        break;
                    case KEY:
                        JOptionPane.showMessageDialog(dialog, "Ключ используется, чтобы открыть выход.");
                        break;
                    default:
                        JOptionPane.showMessageDialog(dialog, "Нельзя использовать этот предмет здесь.");
                }
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private class MazePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            for (int y = 0; y < maze.getHeight(); y++) {
                for (int x = 0; x < maze.getWidth(); x++) {
                    Cell cell = maze.getCell(x, y);
                    if (cell.getType() == Cell.Type.EXIT) {
                        // Выход
                        g.setColor(Color.GREEN.darker());
                        g.fillRect(x * CELL_SIZE + 2, y * CELL_SIZE + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                        g.setColor(Color.WHITE);
                        g.drawString("E", x * CELL_SIZE + CELL_SIZE/2 - 3, y * CELL_SIZE + CELL_SIZE/2 + 5);
                    } else if (cell.isWall()) {
                        g.setColor(Color.DARK_GRAY);
                        g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    } else {
                        g.setColor(Color.WHITE);
                        g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    }
                    g.setColor(Color.BLACK);
                    g.drawRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }

            for (Enemy enemy : enemies) {
                int x = enemy.getX() * CELL_SIZE;
                int y = enemy.getY() * CELL_SIZE;
                switch (enemy.getEnemyType()) {
                    case RAT:
                        g.setColor(Color.GRAY);
                        g.fillOval(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
                        break;
                    case SKELETON:
                        g.setColor(Color.WHITE);
                        g.fillOval(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
                        g.setColor(Color.BLACK);
                        g.drawLine(x + 10, y + 10, x + CELL_SIZE - 10, y + CELL_SIZE - 10);
                        g.drawLine(x + CELL_SIZE - 10, y + 10, x + 10, y + CELL_SIZE - 10);
                        break;
                    case GOBLIN:
                        g.setColor(Color.GREEN.darker());
                        g.fillOval(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
                        break;
                    case BOSS:
                        g.setColor(Color.RED.darker());
                        g.fillOval(x + 3, y + 3, CELL_SIZE - 6, CELL_SIZE - 6);
                        g.setColor(Color.YELLOW);
                        g.drawString("B", x + CELL_SIZE/2 - 3, y + CELL_SIZE/2 + 5);
                        break;
                }
                g.setColor(Color.RED);
                g.fillRect(x + 2, y - 6, CELL_SIZE - 4, 3);
                g.setColor(Color.GREEN);
                int hpWidth = (int) ((CELL_SIZE - 4) * ((double) enemy.getHealth() / enemy.getMaxHealth()));
                g.fillRect(x + 2, y - 6, hpWidth, 3);
            }

            for (GroundItem item : groundItems) {
                g.setColor(Color.YELLOW);
                g.fillRect(item.getX() * CELL_SIZE + 8, item.getY() * CELL_SIZE + 8,
                           CELL_SIZE - 16, CELL_SIZE - 16);
                g.setColor(Color.BLACK);
                String label = switch (item.getType()) {
                    case WEAPON -> "W";
                    case ARMOR -> "A";
                    case POTION -> "P";
                    default -> "?";
                };
                g.drawString(label, item.getX() * CELL_SIZE + 14, item.getY() * CELL_SIZE + 22);
            }

            if (player.isAlive()) {
                g.setColor(Color.BLUE);
                g.fillOval(player.getX() * CELL_SIZE + 3, player.getY() * CELL_SIZE + 3,
                           CELL_SIZE - 6, CELL_SIZE - 6);
            }

            g.setColor(Color.RED);
            g.fillRect(10, 10, 200, 15);
            g.setColor(Color.GREEN);
            int healthWidth = (int) (200 * ((double) player.getHealth() / player.getMaxHealth()));
            g.fillRect(10, 10, healthWidth, 15);
            g.setColor(Color.BLACK);
            g.drawRect(10, 10, 200, 15);
            g.drawString("HP: " + player.getHealth() + "/" + player.getMaxHealth(), 15, 23);
        }
    }
}