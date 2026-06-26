package com.tide.rpg.item;

import com.tide.rpg.TideKeys;
import com.tide.rpg.rune.RuneDefinition;
import com.tide.rpg.rune.RuneRegistry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * "데이터는 PDC에, Lore는 오직 시각 표현용으로만" — this class only ever reads
 * PDC values to rebuild Lore text. Nothing in the plugin should ever parse
 * Lore strings back into numbers.
 */
public final class LoreRenderer {

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V"};

    private final ItemRegistry itemRegistry;
    private final RuneRegistry runeRegistry;

    public LoreRenderer(ItemRegistry itemRegistry, RuneRegistry runeRegistry) {
        this.itemRegistry = itemRegistry;
        this.runeRegistry = runeRegistry;
    }

    public void render(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String itemId = pdc.get(TideKeys.ITEM_ID, org.bukkit.persistence.PersistentDataType.STRING);
        if (itemId == null) {
            return;
        }

        ItemDefinition definition = itemRegistry.get(itemId);
        int gs = pdc.getOrDefault(TideKeys.GS, org.bukkit.persistence.PersistentDataType.INTEGER,
                definition != null ? definition.getGearScore() : 0);
        int reinforce = pdc.getOrDefault(TideKeys.REINFORCE, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
        int socketCount = pdc.getOrDefault(TideKeys.SOCKET_COUNT, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
        int tier = definition != null ? definition.getTier() : 1;

        String socketDisplay = buildSocketDisplay(pdc, socketCount);

        List<String> lore = new ArrayList<>();
        List<String> template = definition != null ? definition.getLoreTemplate() : List.of();
        if (template.isEmpty()) {
            lore.add("§7등급: §fTier " + tier);
            lore.add("§6전투력(GS): §f" + gs);
            lore.add("§7강화: §a+" + reinforce);
            lore.add("§7소켓: " + socketDisplay);
        } else {
            for (String line : template) {
                lore.add(line
                        .replace("{tier}", String.valueOf(tier))
                        .replace("{gs}", String.valueOf(gs))
                        .replace("{reinforce}", String.valueOf(reinforce))
                        .replace("{socket_display}", socketDisplay));
            }
        }

        meta.setLore(lore);
        itemStack.setItemMeta(meta);
    }

    private String buildSocketDisplay(PersistentDataContainer pdc, int socketCount) {
        if (socketCount <= 0) {
            return "§7없음";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= socketCount; i++) {
            String raw = pdc.get(TideKeys.socket(i), org.bukkit.persistence.PersistentDataType.STRING);
            builder.append(describeSocket(raw)).append(' ');
        }
        return builder.toString().trim();
    }

    private String describeSocket(String raw) {
        if (raw == null || raw.isBlank()) {
            return "§7[비어있음]";
        }
        String[] parts = raw.split(":");
        if (parts.length != 2) {
            return "§7[비어있음]";
        }
        String type = parts[0];
        int grade;
        try {
            grade = Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            return "§7[비어있음]";
        }
        RuneDefinition runeDefinition = runeRegistry.findByTypeAndGrade(type, grade).orElse(null);
        String roman = grade >= 1 && grade < ROMAN.length ? ROMAN[grade] : String.valueOf(grade);
        String name = runeDefinition != null ? runeDefinition.getDisplayName() : "§f" + type + " " + roman;
        return "[" + name + "§r]";
    }
}
