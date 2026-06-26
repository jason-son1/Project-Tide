package com.tide.rpg.combat;

import com.tide.rpg.TideKeys;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** "세트 보너스: 같은 룬 3개 장착 시 추가 효과" — all 3 sockets sharing one rune type. */
public final class SetBonus {

    public static final double DAMAGE_BONUS = 0.10;

    private SetBonus() {
    }

    public static boolean isActive(PersistentDataContainer pdc) {
        int socketCount = pdc.getOrDefault(TideKeys.SOCKET_COUNT, PersistentDataType.INTEGER, 0);
        if (socketCount < 3) {
            return false;
        }
        String firstType = null;
        for (int i = 1; i <= 3; i++) {
            String raw = pdc.get(TideKeys.socket(i), PersistentDataType.STRING);
            if (raw == null || raw.isBlank()) {
                return false;
            }
            String type = raw.split(":")[0];
            if (firstType == null) {
                firstType = type;
            } else if (!firstType.equalsIgnoreCase(type)) {
                return false;
            }
        }
        return true;
    }
}
