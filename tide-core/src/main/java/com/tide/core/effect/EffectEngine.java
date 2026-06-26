package com.tide.core.effect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

public final class EffectEngine {

    private final JavaPlugin plugin;
    private final Map<String, TideEffect> cachedEffects = new HashMap<>();

    public EffectEngine(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        cachedEffects.clear();
        ConfigurationSection effectsSection = plugin.getConfig().getConfigurationSection("effects");
        if (effectsSection == null) {
            return;
        }

        for (String key : effectsSection.getKeys(false)) {
            ConfigurationSection sec = effectsSection.getConfigurationSection(key);
            if (sec == null) continue;

            TideEffect effect = new TideEffect();
            effect.title = sec.getString("title");
            effect.subtitle = sec.getString("subtitle");
            effect.titleFadeIn = sec.getInt("title-fade-in-ticks", 10);
            effect.titleStay = sec.getInt("title-stay-ticks", 40);
            effect.titleFadeOut = sec.getInt("title-fade-out-ticks", 10);
            effect.actionBar = sec.getString("action-bar");

            // Load sounds
            List<?> soundList = sec.getList("sounds");
            if (soundList != null) {
                for (Object rawObj : soundList) {
                    if (rawObj instanceof Map<?, ?> map) {
                        String name = (String) map.get("name");
                        double vol = map.containsKey("volume") ? ((Number) map.get("volume")).doubleValue() : 1.0;
                        double pitch = map.containsKey("pitch") ? ((Number) map.get("pitch")).doubleValue() : 1.0;
                        if (name != null) {
                            effect.sounds.add(new TideSound(name, (float) vol, (float) pitch));
                        }
                    }
                }
            }

            // Load particles
            List<?> particleList = sec.getList("particles");
            if (particleList != null) {
                for (Object rawObj : particleList) {
                    if (rawObj instanceof Map<?, ?> map) {
                        String type = (String) map.get("type");
                        int count = map.containsKey("count") ? ((Number) map.get("count")).intValue() : 10;
                        double sx = map.containsKey("spread-x") ? ((Number) map.get("spread-x")).doubleValue() : 0.0;
                        double sy = map.containsKey("spread-y") ? ((Number) map.get("spread-y")).doubleValue() : 0.0;
                        double sz = map.containsKey("spread-z") ? ((Number) map.get("spread-z")).doubleValue() : 0.0;
                        double speed = map.containsKey("speed") ? ((Number) map.get("speed")).doubleValue() : 0.0;
                        if (type != null) {
                            effect.particles.add(new TideParticle(type, count, sx, sy, sz, speed));
                        }
                    }
                }
            }

            cachedEffects.put(key, effect);
        }
        plugin.getLogger().info("Loaded " + cachedEffects.size() + " custom effects.");
    }

    public void playEffect(Player player, String effectKey) {
        TideEffect effect = cachedEffects.get(effectKey);
        if (effect == null) return;

        // Play Title
        if (effect.title != null || effect.subtitle != null) {
            String titleStr = effect.title != null ? translateColor(effect.title) : "";
            String subtitleStr = effect.subtitle != null ? translateColor(effect.subtitle) : "";
            player.sendTitle(titleStr, subtitleStr, effect.titleFadeIn, effect.titleStay, effect.titleFadeOut);
        }

        // Play ActionBar
        if (effect.actionBar != null) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(translateColor(effect.actionBar)));
        }

        Location loc = player.getLocation();

        // Play Sounds
        for (TideSound sound : effect.sounds) {
            playSound(player, loc, sound);
        }

        // Play Particles
        for (TideParticle particle : effect.particles) {
            playParticle(player, loc, particle);
        }
    }

    public void playEffect(Location loc, String effectKey) {
        TideEffect effect = cachedEffects.get(effectKey);
        if (effect == null) return;

        // Play Sounds at Location for nearby players
        for (TideSound sound : effect.sounds) {
            for (Player player : loc.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(loc) < 2500) { // Within 50 blocks
                    playSound(player, loc, sound);
                }
            }
        }

        // Play Particles
        for (TideParticle particle : effect.particles) {
            spawnParticleAtLocation(loc, particle);
        }
    }

    private void playSound(Player player, Location loc, TideSound sound) {
        try {
            Sound bukkitSound = null;
            try {
                bukkitSound = Sound.valueOf(sound.name.toUpperCase().replace(".", "_"));
            } catch (IllegalArgumentException ignored) {}

            if (bukkitSound != null) {
                player.playSound(loc, bukkitSound, sound.volume, sound.pitch);
            } else {
                player.playSound(loc, sound.name.toLowerCase(), sound.volume, sound.pitch);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to play sound: " + sound.name, e);
        }
    }

    private void playParticle(Player player, Location loc, TideParticle particle) {
        try {
            Particle bukkitParticle = Particle.valueOf(particle.type.toUpperCase());
            player.spawnParticle(bukkitParticle, loc, particle.count, particle.spreadX, particle.spreadY, particle.spreadZ, particle.speed);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown particle type: " + particle.type);
        }
    }

    private void spawnParticleAtLocation(Location loc, TideParticle particle) {
        try {
            Particle bukkitParticle = Particle.valueOf(particle.type.toUpperCase());
            loc.getWorld().spawnParticle(bukkitParticle, loc, particle.count, particle.spreadX, particle.spreadY, particle.spreadZ, particle.speed);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown particle type: " + particle.type);
        }
    }

    private String translateColor(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    private static class TideEffect {
        String title;
        String subtitle;
        int titleFadeIn;
        int titleStay;
        int titleFadeOut;
        String actionBar;
        final List<TideSound> sounds = new ArrayList<>();
        final List<TideParticle> particles = new ArrayList<>();
    }

    private static class TideSound {
        final String name;
        final float volume;
        final float pitch;

        TideSound(String name, float volume, float pitch) {
            this.name = name;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    private static class TideParticle {
        final String type;
        final int count;
        final double spreadX;
        final double spreadY;
        final double spreadZ;
        final double speed;

        TideParticle(String type, int count, double spreadX, double spreadY, double spreadZ, double speed) {
            this.type = type;
            this.count = count;
            this.spreadX = spreadX;
            this.spreadY = spreadY;
            this.spreadZ = spreadZ;
            this.speed = speed;
        }
    }
}
