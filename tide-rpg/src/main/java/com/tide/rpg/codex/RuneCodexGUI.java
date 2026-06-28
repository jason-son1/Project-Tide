package com.tide.rpg.codex;

import com.tide.rpg.rune.RuneDefinition;
import com.tide.rpg.rune.RuneItemFactory;
import com.tide.rpg.rune.RuneRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Rune codex: every rune's type/grade and its exact combat effect in one screen. Previously a
 * rune's lore only said "타입: lifesteal / 등급: 1" with no indication of what that actually does,
 * so players had no way to compare runes before spending fusion materials on one.
 */
public final class RuneCodexGUI {

    public static final String TITLE = "§8[§d룬 도감§8]";

    private final RuneRegistry runeRegistry;
    private final RuneItemFactory runeItemFactory;

    public RuneCodexGUI(RuneRegistry runeRegistry, RuneItemFactory runeItemFactory) {
        this.runeRegistry = runeRegistry;
        this.runeItemFactory = runeItemFactory;
    }

    public void open(Player player) {
        RuneCodexHolder holder = new RuneCodexHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, TITLE);
        holder.setInventory(inv);

        ItemStack topFiller = makeFiller(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 1; i < 9; i++) inv.setItem(i, topFiller);
        ItemStack bottomFiller = makeFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 45; i < 54; i++) inv.setItem(i, bottomFiller);
        inv.setItem(0, backButton());

        List<RuneDefinition> sorted = new ArrayList<>(runeRegistry.getAll().values());
        sorted.sort(Comparator.comparing(RuneDefinition::getType).thenComparingInt(RuneDefinition::getGrade));

        int slot = 9;
        for (RuneDefinition def : sorted) {
            if (slot > 44) break;
            inv.setItem(slot, buildDisplayItem(def));
            slot++;
        }

        player.openInventory(inv);
    }

    private ItemStack buildDisplayItem(RuneDefinition def) {
        ItemStack display;
        try {
            display = runeItemFactory.create(def.getId());
        } catch (Exception e) {
            display = new ItemStack(def.getMaterial());
        }
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            return display;
        }
        List<String> lore = new ArrayList<>();
        lore.add("§7타입: §f" + def.getType() + "  §7등급: §f" + def.getGrade());
        lore.add("");
        lore.addAll(effectLore(def));
        if (def.getFusionInputId() != null) {
            lore.add("");
            lore.add("§8▶ 융합 재료: §7" + def.getFusionInputId() + " x" + def.getFusionInputCount()
                    + " + 조개 " + def.getFusionCostClam());
        }
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    /** Mirrors the exact formulas in RuneEffectDispatcher/DefensiveListener so the codex never
     *  drifts out of sync with what the rune actually does in combat. */
    private List<String> effectLore(RuneDefinition def) {
        int grade = Math.max(1, def.getGrade());
        List<String> lines = new ArrayList<>();
        switch (def.getType().toLowerCase()) {
            case "lifesteal" -> {
                lines.add("§7공격 시 가한 피해량의 §c" + (8 * grade) + "%§7를 체력으로 흡수합니다.");
                lines.add("§8(블러드문/블러드 사리: 피해량과 무관하게 고정 §c" + String.format("%.1f", 2.0 * grade) + "§8 회복으로 각성)");
            }
            case "lightning" -> {
                lines.add("§7공격 시 §e" + (10 * grade) + "%§7 확률로 번개를 내려쳐 §c4§7의 추가 피해를 줍니다.");
                lines.add("§8(사리/블러드 사리: 확률 2배 + 주변 적 최대 2명에게 §c2§8 연쇄 피해)");
            }
            case "slow" -> {
                double seconds = (40 + grade * 20) / 20.0;
                lines.add("§7공격 시 대상에게 §9슬로우니스 " + toRoman(grade) + "§7 효과를 §f" + seconds + "초§7간 부여합니다.");
            }
            case "berserk" -> {
                lines.add("§7무기 장착 시 가하는 피해량 §c+30%§7, 받는 피해량 §c+15%§7가 항상 적용됩니다.");
                lines.add("§8(등급과 무관하게 동일한 고정 효과입니다)");
            }
            case "shield" -> lines.add("§7방어구 장착 시 피격마다 흡수(Absorption) §a" + (5 * grade) + "§7만큼 부여합니다.");
            default -> lines.add("§7효과 정보가 등록되지 않았습니다.");
        }
        return lines;
    }

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c« 아이템 도감으로 돌아가기");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeFiller(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
}
