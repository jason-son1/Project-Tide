package com.tide.core.guide;

import org.bukkit.Material;

public enum GuideCategory {
    TIDE("tide", "조수 시스템", Material.PRISMARINE_CRYSTALS, "밀물·썰물·사리·블러드문이 서버 전체에 미치는 영향"),
    FORGE("forge", "장비 강화", Material.ANVIL, "강화, 소켓, 룬 장착으로 장비를 단련하는 방법"),
    MOB("mob", "몬스터와 네메시스", Material.WITHER_SKELETON_SKULL, "정예 몹과 복수자 네메시스 시스템"),
    BOUNTY("bounty", "현상금과 계약", Material.WRITABLE_BOOK, "현상금 임무와 대행 계약 시스템"),
    DEATH("death", "사망과 경제", Material.CHEST, "비석, 소프트 하드코어, 화폐와 평판"),
    EXTENSION("extension", "조수 확장 시스템", Material.TRIDENT, "조류, 공명, 오버드라이브 등 심화 콘텐츠");

    private final String id;
    private final String displayName;
    private final Material icon;
    private final String description;

    GuideCategory(String id, String displayName, Material icon, String description) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public static GuideCategory byId(String id) {
        if (id == null) {
            return null;
        }
        for (GuideCategory category : values()) {
            if (category.id.equalsIgnoreCase(id)) {
                return category;
            }
        }
        return null;
    }
}
