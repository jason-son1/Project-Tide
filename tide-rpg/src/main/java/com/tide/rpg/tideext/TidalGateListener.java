package com.tide.rpg.tideext;

import com.tide.core.tide.TideChangeEvent;
import com.tide.core.tide.TideState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class TidalGateListener implements Listener {

    private final GateRegistry gateRegistry;

    public TidalGateListener(GateRegistry gateRegistry) {
        this.gateRegistry = gateRegistry;
    }

    @EventHandler
    public void onTideChange(TideChangeEvent event) {
        applyAll(event.getNewState());
    }

    /** Also called once at plugin startup so gates start in the correct state. */
    public void applyAll(TideState state) {
        for (GateDefinition gate : gateRegistry.all()) {
            World world = Bukkit.getWorld(gate.getWorld());
            if (world == null) {
                continue;
            }
            Material target = gate.isOpenDuring(state) ? Material.AIR : gate.getClosedMaterial();
            for (GateDefinition.BlockPos pos : gate.getBlocks()) {
                world.getBlockAt(pos.x(), pos.y(), pos.z()).setType(target);
            }
        }
    }
}
