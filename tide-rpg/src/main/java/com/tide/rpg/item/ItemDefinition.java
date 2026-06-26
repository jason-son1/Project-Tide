package com.tide.rpg.item;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

/** Immutable parse of one items/<id>.yml file, per the standard item schema. */
public final class ItemDefinition {

    private final String id;
    private final String displayName;
    private final Material material;
    private final int customModelData;
    private final int gearScore;
    private final int tier;
    private final int baseDamage;
    private final int baseDefense;
    private final double damagePerStar;
    private final double defensePerStar;
    private final int socketCount;
    private final int socketMax;
    private final int sellPrice;
    private final List<String> loreTemplate;
    private final String resonance;
    private final int anchor;
    private final int oxygenCapacity;

    private ItemDefinition(String id, String displayName, Material material, int customModelData,
                            int gearScore, int tier, int baseDamage, int baseDefense,
                            double damagePerStar, double defensePerStar, int socketCount,
                            int socketMax, int sellPrice, List<String> loreTemplate,
                            String resonance, int anchor, int oxygenCapacity) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.gearScore = gearScore;
        this.tier = tier;
        this.baseDamage = baseDamage;
        this.baseDefense = baseDefense;
        this.damagePerStar = damagePerStar;
        this.defensePerStar = defensePerStar;
        this.socketCount = socketCount;
        this.socketMax = socketMax;
        this.sellPrice = sellPrice;
        this.loreTemplate = loreTemplate;
        this.resonance = resonance;
        this.anchor = anchor;
        this.oxygenCapacity = oxygenCapacity;
    }

    public static ItemDefinition parse(YamlConfiguration yaml) {
        String id = yaml.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("필수 필드 누락: id");
        }
        String materialName = yaml.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("알 수 없는 material: " + materialName);
        }
        return new ItemDefinition(
                id,
                yaml.getString("display_name", id),
                material,
                yaml.getInt("custom_model_data", 0),
                yaml.getInt("gear_score", 100),
                yaml.getInt("tier", 1),
                yaml.getInt("base_stats.damage", 0),
                yaml.getInt("base_stats.defense", 0),
                yaml.getDouble("reinforce_bonus.damage_per_star", 0),
                yaml.getDouble("reinforce_bonus.defense_per_star", 0),
                yaml.getInt("socket_count", 0),
                yaml.getInt("socket_max", 0),
                yaml.getInt("sell_price", 0),
                yaml.getStringList("lore_template"),
                yaml.getString("resonance", null),
                yaml.getInt("anchor", 0),
                yaml.getInt("oxygen_capacity", 0)
        );
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public int getGearScore() {
        return gearScore;
    }

    public int getTier() {
        return tier;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public int getBaseDefense() {
        return baseDefense;
    }

    public double getDamagePerStar() {
        return damagePerStar;
    }

    public double getDefensePerStar() {
        return defensePerStar;
    }

    public int getSocketCount() {
        return socketCount;
    }

    public int getSocketMax() {
        return socketMax;
    }

    public int getSellPrice() {
        return sellPrice;
    }

    public List<String> getLoreTemplate() {
        return loreTemplate;
    }

    public String getResonance() {
        return resonance;
    }

    public int getAnchor() {
        return anchor;
    }

    public int getOxygenCapacity() {
        return oxygenCapacity;
    }
}
