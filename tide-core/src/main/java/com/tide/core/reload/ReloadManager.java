package com.tide.core.reload;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lets TideRPG/TideMobs register their YAML-backed registries (items, mobs,
 * runes, affixes...) under a name so `/tide reload [name]` can hot-reload
 * them without a server restart, per the 2파트 Config & Interaction Layer spec.
 */
public final class ReloadManager {

    private final Map<String, Reloadable> reloadables = new LinkedHashMap<>();

    public void register(String name, Reloadable reloadable) {
        reloadables.put(name.toLowerCase(), reloadable);
    }

    public void unregister(String name) {
        reloadables.remove(name.toLowerCase());
    }

    /** @return human-readable per-target result lines */
    public Map<String, Integer> reloadAll() {
        Map<String, Integer> results = new LinkedHashMap<>();
        for (Map.Entry<String, Reloadable> entry : reloadables.entrySet()) {
            results.put(entry.getKey(), entry.getValue().reload());
        }
        return results;
    }

    public Integer reload(String name) {
        Reloadable reloadable = reloadables.get(name.toLowerCase());
        return reloadable == null ? null : reloadable.reload();
    }

    public boolean has(String name) {
        return reloadables.containsKey(name.toLowerCase());
    }
}
