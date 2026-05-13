package ru.mazegame.ui;

import ru.mazegame.domain.*;
import ru.mazegame.service.InventoryService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BattleDialog extends JDialog {
    private final Player player;
    private final Enemy enemy;
    private final InventoryService inventoryService;  // без шлюзов
    private final List<InventoryItem> playerSkills;

    private BattleResult result = BattleResult.IN_PROGRESS;

    private JLabel playerHpLabel;
    private JLabel enemyHpLabel;
    private JTextArea logArea;
    private JButton attackButton;
    private JComboBox<String> actionCombo;

    public enum BattleResult { IN_PROGRESS, PLAYER_WON, PLAYER_DIED }

    public BattleDialog(JFrame parent, Player player, Enemy enemy,
                        List<InventoryItem> playerSkills, InventoryService inventoryService) {
        super(parent, "Бой с " + enemy.getName(), true);
        this.player = player;
        this.enemy = enemy;
        this.inventoryService = inventoryService;
        this.playerSkills = playerSkills;

        setLayout(new BorderLayout());
        setResizable(false);

        JPanel infoPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        playerHpLabel = new JLabel("Ваше HP: " + player.getHealth() + "/" + player.getMaxHealth());
        enemyHpLabel = new JLabel(enemy.getName() + " HP: " + enemy.getHealth() + "/" + enemy.getMaxHealth());
        infoPanel.add(new JLabel("Вы:"));
        infoPanel.add(playerHpLabel);
        infoPanel.add(new JLabel("Враг:"));
        infoPanel.add(enemyHpLabel);
        add(infoPanel, BorderLayout.NORTH);

        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout());
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Обычная атака");
        for (InventoryItem skill : playerSkills) {
            model.addElement(skill.getName() + " (SKILL)");
        }
        actionCombo = new JComboBox<>(model);
        attackButton = new JButton("Выполнить");
        controlPanel.add(actionCombo);
        controlPanel.add(attackButton);
        add(controlPanel, BorderLayout.SOUTH);

        attackButton.addActionListener(e -> performAction());

        pack();
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void performAction() {
        String selected = (String) actionCombo.getSelectedItem();
        int playerAttack = 5 + inventoryService.getEquippedAttackBonus(player);

        if (selected.equals("Обычная атака")) {
            int damage = Math.max(0, playerAttack - enemy.getDefense());
            enemy.takeDamage(damage);
            logArea.append("Вы атакуете и наносите " + damage + " урона.\n");
        } else {
            String skillName = selected.replace(" (SKILL)", "");
            InventoryItem usedSkill = playerSkills.stream()
                    .filter(s -> s.getName().equals(skillName))
                    .findFirst().orElse(null);
            if (usedSkill != null) {
                int baseDamage = playerAttack + usedSkill.getAttackBonus();
                int damage = Math.max(0, baseDamage - enemy.getDefense());
                enemy.takeDamage(damage);
                logArea.append("Вы применяете " + skillName + " и наносите " + damage + " урона.\n");
                if (usedSkill.getHealAmount() > 0) {
                    player.heal(usedSkill.getHealAmount());
                    logArea.append("Вы восстанавливаете " + usedSkill.getHealAmount() + " здоровья.\n");
                }
                inventoryService.removeItem(usedSkill.getId());
                playerSkills.remove(usedSkill);
                DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) actionCombo.getModel();
                model.removeElement(selected);
            }
        }

        if (!enemy.isAlive()) {
            logArea.append("Вы победили " + enemy.getName() + "!\n");
            if (enemy.getEnemyType() == Enemy.EnemyType.BOSS) {
                InventoryItem skill = new InventoryItem("Смертельный удар", ItemType.SKILL, 10, 0, 20, false);
                inventoryService.addItem(skill, player.getId());
                logArea.append("Вы получаете свиток умения: " + skill.getName() + "!\n");

                InventoryItem key = new InventoryItem("Ключ выхода", ItemType.KEY, 0, 0, 0, false);
                inventoryService.addItem(key, player.getId());
                logArea.append("Босс роняет Ключ выхода!\n");
            }
            result = BattleResult.PLAYER_WON;
            dispose();
            return;
        }

        // Ход врага
        int defenseBonus = inventoryService.getEquippedDefenseBonus(player);
        int baseDamage = enemy.getAttack();
        if (enemy.getEnemyType() == Enemy.EnemyType.BOSS && Math.random() < 0.4) {
            baseDamage *= 2;
            logArea.append("Босс проводит сокрушительную атаку!\n");
        } else if (enemy.getEnemyType() == Enemy.EnemyType.GOBLIN && Math.random() < 0.3) {
            baseDamage += 2;
            logArea.append("Гоблин ловко наносит удар!\n");
        }
        int damage = Math.max(0, baseDamage - defenseBonus);
        player.takeDamage(damage);
        logArea.append(enemy.getName() + " атакует, нанося " + damage + " урона.\n");

        if (!player.isAlive()) {
            logArea.append("Вы погибли...\n");
            result = BattleResult.PLAYER_DIED;
            dispose();
            return;
        }
        updateInfo();
    }

    private void updateInfo() {
        playerHpLabel.setText("Ваше HP: " + player.getHealth() + "/" + player.getMaxHealth());
        enemyHpLabel.setText(enemy.getName() + " HP: " + enemy.getHealth() + "/" + enemy.getMaxHealth());
        attackButton.setEnabled(player.isAlive() && enemy.isAlive());
        actionCombo.setEnabled(player.isAlive() && enemy.isAlive());
    }

    public BattleResult getBattleResult() {
        return result;
    }
}