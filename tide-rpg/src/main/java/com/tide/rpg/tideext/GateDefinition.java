package com.tide.rpg.tideext;

import com.tide.core.tide.TideState;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tidal Gates (1-3): a set of blocks that switch between a closed material and AIR
 *  depending on whether the current TideState is in this gate's openStates. */
public final class GateDefinition {

    public record BlockPos(int x, int y, int z) {
    }

    private final String id;
    private final String world;
    private final List<BlockPos> blocks;
    private final Material closedMaterial;
    private final Set<TideState> openStates;

    private GateDefinition(String id, String world, List<BlockPos> blocks, Material closedMaterial, Set<TideState> openStates) {
        this.id = id;
        this.world = world;
        this.blocks = blocks;
        this.closedMaterial = closedMaterial;
        this.openStates = openStates;
    }

    public static GateDefinition parse(YamlConfiguration yaml) {
        String id = yaml.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("필수 필드 누락: id");
        }
        List<BlockPos> blocks = new ArrayList<>();
        List<Map<?, ?>> rawBlocks = yaml.getMapList("blocks");
        for (Map<?, ?> raw : rawBlocks) {
            int x = ((Number) raw.get("x")).intValue();
            int y = ((Number) raw.get("y")).intValue();
            int z = ((Number) raw.get("z")).intValue();
            blocks.add(new BlockPos(x, y, z));
        }
        Material closedMaterial = Material.matchMaterial(yaml.getString("closed_material", "IRON_BARS"));
        if (closedMaterial == null) {
            closedMaterial = Material.IRON_BARS;
        }
        Set<TideState> openStates = EnumSet.noneOf(TideState.class);
        for (String stateName : yaml.getStringList("open_states")) {
            try {
                openStates.add(TideState.valueOf(stateName.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return new GateDefinition(id, yaml.getString("world", "world"), blocks, closedMaterial, openStates);
    }

    public String getId() {
        return id;
    }

    public String getWorld() {
        return world;
    }

    public List<BlockPos> getBlocks() {
        return blocks;
    }

    public Material getClosedMaterial() {
        return closedMaterial;
    }

    public boolean isOpenDuring(TideState state) {
        return openStates.contains(state);
    }
}
