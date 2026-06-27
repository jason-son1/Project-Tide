package com.tide.mobs.quest;

import com.tide.core.reload.Reloadable;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Loads all {@link QuestTemplate} definitions from
 * {@code &lt;dataFolder&gt;/quests/*.yml} and provides weighted-random
 * selection for daily and weekly bounty generation.
 *
 * <p>Implements {@link Reloadable} so that {@code /tide reload} picks up
 * any new or changed YAML files at runtime.
 */
public final class QuestRegistry implements Reloadable {

    private final JavaPlugin plugin;
    private final Logger log;

    private final List<QuestTemplate> dailyPool  = new ArrayList<>();
    private final List<QuestTemplate> weeklyPool = new ArrayList<>();

    public QuestRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log    = plugin.getLogger();
    }

    // ── Reloadable ────────────────────────────────────────────────────────────

    @Override
    public int reload() {
        dailyPool.clear();
        weeklyPool.clear();

        File questDir = new File(plugin.getDataFolder(), "quests");
        if (!questDir.exists() || !questDir.isDirectory()) {
            log.warning("[QuestRegistry] quests/ 디렉토리가 없습니다. 퀘스트를 로드하지 못했습니다.");
            return 0;
        }

        File[] files = questDir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            log.warning("[QuestRegistry] quests/*.yml 파일이 없습니다.");
            return 0;
        }

        int loaded = 0;
        for (File file : files) {
            QuestTemplate template = QuestTemplate.fromFile(file);
            if (template == null) {
                log.warning("[QuestRegistry] 파싱 실패: " + file.getName());
                continue;
            }
            if (template.isAvailableForDaily())  dailyPool.add(template);
            if (template.isAvailableForWeekly()) weeklyPool.add(template);
            loaded++;
        }
        log.info("[QuestRegistry] 퀘스트 " + loaded + "개 로드 완료 (일일 풀: " + dailyPool.size() + ", 주간 풀: " + weeklyPool.size() + ")");
        return loaded;
    }

    // ── Pool access ───────────────────────────────────────────────────────────

    /** Returns an unmodifiable view of all templates eligible for daily slots. */
    public List<QuestTemplate> dailyPool() { return Collections.unmodifiableList(dailyPool); }

    /** Returns an unmodifiable view of all templates eligible for weekly slots. */
    public List<QuestTemplate> weeklyPool() { return Collections.unmodifiableList(weeklyPool); }

    /**
     * Draw {@code count} distinct templates from the daily pool using weighted
     * random sampling (without replacement). If the pool is smaller than
     * {@code count}, all templates are returned.
     */
    public List<QuestTemplate> drawDaily(int count) {
        return weightedDraw(dailyPool, count);
    }

    /**
     * Draw one template from the weekly pool using weighted random sampling.
     * Returns null if the pool is empty.
     */
    public QuestTemplate drawWeekly() {
        List<QuestTemplate> drawn = weightedDraw(weeklyPool, 1);
        return drawn.isEmpty() ? null : drawn.get(0);
    }

    // ── Weighted random draw ─────────────────────────────────────────────────

    private static List<QuestTemplate> weightedDraw(List<QuestTemplate> pool, int count) {
        if (pool.isEmpty()) return List.of();
        count = Math.min(count, pool.size());

        // Copy so we can remove drawn items (without-replacement)
        List<QuestTemplate> remaining = new ArrayList<>(pool);
        List<QuestTemplate> result    = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int totalWeight = remaining.stream().mapToInt(QuestTemplate::getWeight).sum();
            if (totalWeight <= 0) break;
            int roll = ThreadLocalRandom.current().nextInt(totalWeight);
            int cumulative = 0;
            QuestTemplate chosen = null;
            for (QuestTemplate t : remaining) {
                cumulative += t.getWeight();
                if (roll < cumulative) { chosen = t; break; }
            }
            if (chosen == null) chosen = remaining.get(remaining.size() - 1);
            result.add(chosen);
            remaining.remove(chosen);
        }
        return result;
    }
}
