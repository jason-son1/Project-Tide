package com.tide.rpg.combat;

import org.bukkit.entity.EntityType;

import java.util.Map;

/** Korean display names for vanilla EntityType, used as a fallback wherever a mob has no custom name. */
public final class MobNameKo {

    private static final Map<EntityType, String> NAMES = Map.ofEntries(
            Map.entry(EntityType.ZOMBIE, "좀비"),
            Map.entry(EntityType.HUSK, "허스크"),
            Map.entry(EntityType.DROWNED, "익사자"),
            Map.entry(EntityType.SKELETON, "스켈레톤"),
            Map.entry(EntityType.STRAY, "스트레이"),
            Map.entry(EntityType.WITHER_SKELETON, "위더 스켈레톤"),
            Map.entry(EntityType.SPIDER, "거미"),
            Map.entry(EntityType.CAVE_SPIDER, "동굴 거미"),
            Map.entry(EntityType.CREEPER, "크리퍼"),
            Map.entry(EntityType.ENDERMAN, "엔더맨"),
            Map.entry(EntityType.WITCH, "마녀"),
            Map.entry(EntityType.PILLAGER, "약탈자"),
            Map.entry(EntityType.VINDICATOR, "방랑 악당"),
            Map.entry(EntityType.EVOKER, "선동가"),
            Map.entry(EntityType.RAVAGER, "라바저"),
            Map.entry(EntityType.GUARDIAN, "가디언"),
            Map.entry(EntityType.ELDER_GUARDIAN, "엘더 가디언"),
            Map.entry(EntityType.PHANTOM, "팬텀"),
            Map.entry(EntityType.SLIME, "슬라임"),
            Map.entry(EntityType.MAGMA_CUBE, "마그마 큐브"),
            Map.entry(EntityType.GHAST, "가스트"),
            Map.entry(EntityType.BLAZE, "블레이즈"),
            Map.entry(EntityType.SHULKER, "셜커"),
            Map.entry(EntityType.SILVERFISH, "은빛개구리"),
            Map.entry(EntityType.PIGLIN, "피글린"),
            Map.entry(EntityType.PIGLIN_BRUTE, "피글린 야만족"),
            Map.entry(EntityType.HOGLIN, "호글린"),
            Map.entry(EntityType.ZOGLIN, "조글린"),
            Map.entry(EntityType.WITHER, "위더"),
            Map.entry(EntityType.ENDER_DRAGON, "엔더 드래곤"),
            Map.entry(EntityType.WARDEN, "워든"),
            Map.entry(EntityType.BREEZE, "브리즈"),
            Map.entry(EntityType.COW, "소"),
            Map.entry(EntityType.PIG, "돼지"),
            Map.entry(EntityType.SHEEP, "양"),
            Map.entry(EntityType.CHICKEN, "닭"),
            Map.entry(EntityType.HORSE, "말"),
            Map.entry(EntityType.WOLF, "늑대"),
            Map.entry(EntityType.CAT, "고양이"),
            Map.entry(EntityType.RABBIT, "토끼"),
            Map.entry(EntityType.VILLAGER, "주민"),
            Map.entry(EntityType.IRON_GOLEM, "철 골렘"),
            Map.entry(EntityType.SNOW_GOLEM, "눈 골렘"),
            Map.entry(EntityType.BAT, "박쥐"),
            Map.entry(EntityType.SQUID, "오징어"),
            Map.entry(EntityType.GLOW_SQUID, "발광 오징어"),
            Map.entry(EntityType.DOLPHIN, "돌고래"),
            Map.entry(EntityType.TURTLE, "거북"),
            Map.entry(EntityType.AXOLOTL, "아홀로틀"),
            Map.entry(EntityType.FROG, "개구리"),
            Map.entry(EntityType.ALLAY, "알레이")
    );

    private MobNameKo() {
    }

    /** @return Korean name if known, otherwise the English EntityType name as a last resort. */
    public static String of(EntityType type) {
        return NAMES.getOrDefault(type, type.name().replace("_", " "));
    }
}
