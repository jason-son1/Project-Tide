package com.tide.core.tide;

import com.tide.core.TideCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drives the tide cycle with a 1-second countdown (no entity scanning — just
 * a timer + a BossBar refresh). State transitions fire {@link TideChangeEvent}
 * so TideRPG/TideMobs react independently without a direct call into this class.
 */
public final class TideScheduler implements TideStateProvider {

    private final TideCorePlugin plugin;
    private final BossBar bossBar;

    private TideState currentState = TideState.HIGH_TIDE;
    private TideState lastBaseState = TideState.LOW_TIDE; // so the first roll alternates into HIGH_TIDE
    private long secondsRemaining;
    private LocalDate lastScheduledSpringDate;
    private BukkitTask task;

    public TideScheduler(TideCorePlugin plugin) {
        this.plugin = plugin;
        this.bossBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
    }

    public void start() {
        secondsRemaining = cycleDurationMinutes() * 60L;
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
        updateBossBarText();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        bossBar.removeAll();
    }

    public void addPlayerToBossBar(Player player) {
        bossBar.addPlayer(player);
    }

    private void tick() {
        secondsRemaining--;
        if (secondsRemaining <= 0) {
            transition();
            secondsRemaining = cycleDurationMinutes() * 60L;
        }
        updateBossBarText();
    }

    private void transition() {
        TideState previous = currentState;
        currentState = rollNextState();
        if (currentState == TideState.HIGH_TIDE || currentState == TideState.LOW_TIDE) {
            lastBaseState = currentState;
        }
        Bukkit.getPluginManager().callEvent(new TideChangeEvent(previous, currentState));
    }

    private TideState rollNextState() {
        double springChance = plugin.getConfig().getDouble("tide.spring-tide-chance", 5.0) / 100.0;
        double bloodMoonChance = plugin.getConfig().getDouble("tide.blood-moon-chance", 8.0) / 100.0;

        boolean springRoll = ThreadLocalRandom.current().nextDouble() < springChance;
        boolean scheduledSpring = isScheduledSpringDayDue();
        if (scheduledSpring) {
            springRoll = true;
            lastScheduledSpringDate = LocalDate.now();
        }

        boolean bloodRoll = isNight() && ThreadLocalRandom.current().nextDouble() < bloodMoonChance;

        if (springRoll && bloodRoll) {
            return TideState.BLOOD_TIDE;
        }
        if (springRoll) {
            return TideState.SPRING_TIDE;
        }
        if (bloodRoll) {
            return TideState.BLOOD_MOON;
        }
        return lastBaseState == TideState.HIGH_TIDE ? TideState.LOW_TIDE : TideState.HIGH_TIDE;
    }

    private boolean isScheduledSpringDayDue() {
        String dayName = plugin.getConfig().getString("tide.scheduled-spring-day", "SUNDAY");
        DayOfWeek scheduledDay;
        try {
            scheduledDay = DayOfWeek.valueOf(dayName.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return today.getDayOfWeek() == scheduledDay && !today.equals(lastScheduledSpringDate);
    }

    private boolean isNight() {
        World world = Bukkit.getWorlds().get(0);
        long time = world.getTime();
        return time >= 13000 && time <= 23000;
    }

    private long cycleDurationMinutes() {
        return Math.max(1, plugin.getConfig().getLong("tide.cycle-duration-minutes", 120));
    }

    private void updateBossBarText() {
        long hours = secondsRemaining / 3600;
        long minutes = (secondsRemaining % 3600) / 60;
        long seconds = secondsRemaining % 60;
        bossBar.setTitle(String.format("%s §7— 다음 변동까지 §f%02d:%02d:%02d",
                currentState.getDisplayName(), hours, minutes, seconds));
        bossBar.setColor(barColorFor(currentState));
    }

    private BarColor barColorFor(TideState state) {
        return switch (state) {
            case HIGH_TIDE -> BarColor.BLUE;
            case LOW_TIDE -> BarColor.PURPLE;
            case SPRING_TIDE -> BarColor.YELLOW;
            case BLOOD_MOON -> BarColor.RED;
            case BLOOD_TIDE -> BarColor.RED;
        };
    }

    /** Forces an immediate transition, used by /tide admin and /tide set. */
    public void forceState(TideState state) {
        forceState(state, cycleDurationMinutes() * 60L);
    }

    /** Same as {@link #forceState(TideState)} but with a custom duration — used for the
     *  admin GUI's "사리 발동 (5분 한정)" button. */
    public void forceState(TideState state, long durationSeconds) {
        TideState previous = currentState;
        currentState = state;
        if (state == TideState.HIGH_TIDE || state == TideState.LOW_TIDE) {
            lastBaseState = state;
        }
        secondsRemaining = durationSeconds;
        updateBossBarText();
        Bukkit.getPluginManager().callEvent(new TideChangeEvent(previous, currentState));
    }

    @Override
    public TideState getCurrentState() {
        return currentState;
    }

    @Override
    public long getSecondsUntilNextChange() {
        return secondsRemaining;
    }
}
