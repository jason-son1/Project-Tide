package com.tide.rpg.rune;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

/** Parse of one runes/<id>.yml file. */
public final class RuneDefinition {

    private final String id;
    private final String displayName;
    private final String type;
    private final int grade;
    private final Material material;
    private final int customModelData;
    private final String fusionInputId;
    private final int fusionInputCount;
    private final long fusionCostClam;
    private final String fusionOutputId;

    private RuneDefinition(String id, String displayName, String type, int grade, Material material,
                            int customModelData, String fusionInputId, int fusionInputCount,
                            long fusionCostClam, String fusionOutputId) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.grade = grade;
        this.material = material;
        this.customModelData = customModelData;
        this.fusionInputId = fusionInputId;
        this.fusionInputCount = fusionInputCount;
        this.fusionCostClam = fusionCostClam;
        this.fusionOutputId = fusionOutputId;
    }

    public static RuneDefinition parse(YamlConfiguration yaml) {
        String id = yaml.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("필수 필드 누락: id");
        }
        Material material = Material.matchMaterial(yaml.getString("material", "AMETHYST_SHARD"));
        if (material == null) {
            material = Material.AMETHYST_SHARD;
        }
        return new RuneDefinition(
                id,
                yaml.getString("display_name", id),
                yaml.getString("type", "unknown"),
                yaml.getInt("grade", 1),
                material,
                yaml.getInt("custom_model_data", 0),
                yaml.getString("fusion.input_id", null),
                yaml.getInt("fusion.input_count", 3),
                yaml.getLong("fusion.cost_clam", 0),
                yaml.getString("fusion.output_id", null)
        );
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getType() {
        return type;
    }

    public int getGrade() {
        return grade;
    }

    public Material getMaterial() {
        return material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getFusionInputId() {
        return fusionInputId;
    }

    public int getFusionInputCount() {
        return fusionInputCount;
    }

    public long getFusionCostClam() {
        return fusionCostClam;
    }

    public String getFusionOutputId() {
        return fusionOutputId;
    }
}
