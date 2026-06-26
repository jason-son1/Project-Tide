package com.tide.core.guide;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.List;

public final class GuideEntry {

    private final String id;
    private final GuideCategory category;
    private final int order;
    private final Material icon;
    private final String title;
    private final String summary;
    private final List<String> pages;
    private final List<String> commands;

    private GuideEntry(String id, GuideCategory category, int order, Material icon, String title,
                        String summary, List<String> pages, List<String> commands) {
        this.id = id;
        this.category = category;
        this.order = order;
        this.icon = icon;
        this.title = title;
        this.summary = summary;
        this.pages = pages;
        this.commands = commands;
    }

    public static GuideEntry parse(YamlConfiguration yaml) {
        String id = yaml.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id가 비어 있습니다.");
        }
        GuideCategory category = GuideCategory.byId(yaml.getString("category"));
        if (category == null) {
            throw new IllegalArgumentException("알 수 없는 카테고리: " + yaml.getString("category"));
        }
        int order = yaml.getInt("order", 0);
        Material icon = Material.matchMaterial(yaml.getString("icon", "BOOK"));
        if (icon == null) {
            icon = Material.BOOK;
        }
        String title = yaml.getString("title", id);
        String summary = yaml.getString("summary", "");
        List<String> pages = yaml.getStringList("pages");
        List<String> commands = yaml.getStringList("commands");
        return new GuideEntry(id, category, order, icon, title, summary,
                pages.isEmpty() ? Collections.singletonList("§7(작성된 설명이 없습니다)") : pages,
                commands);
    }

    public String getId() {
        return id;
    }

    public GuideCategory getCategory() {
        return category;
    }

    public int getOrder() {
        return order;
    }

    public Material getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getPages() {
        return pages;
    }

    public List<String> getCommands() {
        return commands;
    }
}
