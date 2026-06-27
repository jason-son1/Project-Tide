package com.tide.rpg.forge;

import com.tide.core.TideCorePlugin;
import com.tide.core.economy.EconomyAPI;
import com.tide.core.guide.GuideGUI;
import com.tide.core.guide.GuideCategory;
import com.tide.rpg.TideKeys;
import com.tide.rpg.item.LoreRenderer;
import com.tide.rpg.rune.RuneDefinition;
import com.tide.rpg.rune.RuneItemFactory;
import com.tide.rpg.rune.RuneRegistry;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ForgeListener implements Listener {

    private final JavaPlugin plugin;
    private final EconomyAPI economyAPI;
    private final LoreRenderer loreRenderer;
    private final RuneRegistry runeRegistry;
    private final RuneItemFactory runeItemFactory;
    private final ForgeGUI forgeGUI;

    public ForgeListener(JavaPlugin plugin, EconomyAPI economyAPI, LoreRenderer loreRenderer,
                          RuneRegistry runeRegistry, RuneItemFactory runeItemFactory, ForgeGUI forgeGUI) {
        this.plugin = plugin;
        this.economyAPI = economyAPI;
        this.loreRenderer = loreRenderer;
        this.runeRegistry = runeRegistry;
        this.runeItemFactory = runeItemFactory;
        this.forgeGUI = forgeGUI;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ForgeHolder holder)) {
            return;
        }
        
        Inventory inventory = event.getInventory();
        int slot = event.getRawSlot();
        ForgeTab tab = holder.getTab();
        Player player = (Player) event.getWhoClicked();

        // Shift 클릭 및 핫바 키 스왑, 더블 클릭은 버그 방지를 위해 전면 취소
        if (event.getClick().isShiftClick() 
                || event.getAction().name().contains("HOTBAR") 
                || event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        // 플레이어 본인 인벤토리(아래쪽) 내의 클릭은 기본 허용
        if (slot >= inventory.getSize()) {
            return; 
        }

        // 대장간 인벤토리(상단 54칸) 내부 클릭 처리
        if (!ForgeGUI.placementSlots(tab).contains(slot)) {
            event.setCancelled(true);
            
            switch (slot) {
                case ForgeGUI.TAB_REINFORCE_BUTTON -> switchTab(player, holder, inventory, ForgeTab.REINFORCE);
                case ForgeGUI.TAB_SOCKET_BUTTON -> switchTab(player, holder, inventory, ForgeTab.SOCKET);
                case ForgeGUI.TAB_REROLL_BUTTON -> switchTab(player, holder, inventory, ForgeTab.REROLL);
                case ForgeGUI.TAB_FUSION_BUTTON -> switchTab(player, holder, inventory, ForgeTab.FUSION);
                case ForgeGUI.GUIDE_BUTTON_SLOT -> {
                    TideCorePlugin corePlugin = TideCorePlugin.getInstance();
                    if (corePlugin != null && corePlugin.getGuideRegistry() != null) {
                        new GuideGUI(corePlugin.getGuideRegistry()).openEntries(player, GuideCategory.FORGE);
                    } else {
                        player.sendMessage("§c가이드를 로드할 수 없습니다.");
                    }
                }
                default -> {
                    if (tab == ForgeTab.REINFORCE && slot == ForgeGUI.REINFORCE_ATTEMPT_BUTTON) {
                        handleReinforce(player, inventory);
                    } else if (tab == ForgeTab.SOCKET && slot == ForgeGUI.SOCKET_ATTACH_BUTTON) {
                        handleSocketAttach(player, inventory);
                    } else if (tab == ForgeTab.REROLL && slot == ForgeGUI.REROLL_BUTTON) {
                        handleReroll(player, inventory);
                    } else if (tab == ForgeTab.FUSION && slot == ForgeGUI.FUSION_BUTTON) {
                        handleFusion(player, inventory);
                    }
                }
            }
            return;
        }

        // 배치 슬롯 클릭 처리 - 무조건 이벤트를 취소하고 수동 제어하여 마커 복사 및 아이템 누락 버그 방지
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // A. 플레이어가 맨손으로 배치 슬롯을 클릭했을 때 (아이템 꺼내기)
        if (cursorItem == null || cursorItem.getType().isAir()) {
            if (clickedItem != null && !clickedItem.getType().isAir() && !isMarker(clickedItem)) {
                player.setItemOnCursor(clickedItem);
                restorePlaceholder(inventory, tab, slot);
                updateForgeInfo(inventory, tab);
                player.updateInventory();
            }
        }
        // B. 플레이어가 아이템을 쥐고 배치 슬롯을 클릭했을 때 (아이템 넣기 또는 교체)
        else {
            if (!isValidForSlot(tab, slot, cursorItem)) {
                player.sendMessage("§c여기에 배치할 수 없는 아이템입니다.");
                return;
            }

            if (clickedItem == null || clickedItem.getType().isAir() || isMarker(clickedItem)) {
                // 슬롯이 비었거나 마커인 경우: 들고 있는 아이템 배치
                inventory.setItem(slot, cursorItem);
                player.setItemOnCursor(null);
            } else {
                // 기존의 유효한 아이템과 스왑
                ItemStack temp = clickedItem;
                inventory.setItem(slot, cursorItem);
                player.setItemOnCursor(temp);
            }
            updateForgeInfo(inventory, tab);
            player.updateInventory();
        }
    }

    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ForgeHolder) {
            // 대장간 GUI 내에서의 드래그 방지
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ForgeHolder holder)) {
            return;
        }
        returnPlacedItems(event.getPlayer(), event.getInventory(), holder.getTab());
    }

    private void switchTab(Player player, ForgeHolder holder, Inventory inventory, ForgeTab newTab) {
        returnPlacedItems(player, inventory, holder.getTab());
        holder.setTab(newTab);
        forgeGUI.render(inventory, newTab);
    }

    private void returnPlacedItems(org.bukkit.entity.HumanEntity player, Inventory inventory, ForgeTab tab) {
        for (int slot : ForgeGUI.placementSlots(tab)) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !isMarker(item)) {
                java.util.Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
                for (ItemStack remainingItem : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), remainingItem);
                }
            }
            inventory.setItem(slot, null);
        }
    }

    private boolean isMarker(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        var pdc = meta.getPersistentDataContainer();
        return pdc.get(TideKeys.ITEM_ID, PersistentDataType.STRING) == null
                && pdc.get(TideKeys.RUNE_ID, PersistentDataType.STRING) == null
                && !pdc.has(TideKeys.GS, PersistentDataType.INTEGER);
    }

    // ---------- Reinforce ----------

    private void handleReinforce(Player player, Inventory inventory) {
        ItemStack gear = inventory.getItem(ForgeGUI.REINFORCE_GEAR_SLOT);
        if (gear == null || itemIdOf(gear) == null) {
            player.sendMessage("§c강화할 장비를 배치하세요.");
            return;
        }
        ItemStack stone = inventory.getItem(ForgeGUI.REINFORCE_STONE_SLOT);
        if (stone == null || !"reinforce_stone".equals(itemIdOf(stone))) {
            player.sendMessage("§c강화석이 필요합니다.");
            return;
        }

        ItemMeta gearMeta = gear.getItemMeta();
        var pdc = gearMeta.getPersistentDataContainer();
        int currentLevel = pdc.getOrDefault(TideKeys.REINFORCE, PersistentDataType.INTEGER, 0);
        if (currentLevel >= 10) {
            player.sendMessage("§c이미 최대 강화 단계입니다.");
            return;
        }
        int nextLevel = currentLevel + 1;

        long cost = Math.round(plugin.getConfig().getDouble("forge.reinforce-cost-base", 100)
                * Math.pow(plugin.getConfig().getDouble("forge.reinforce-cost-multiplier", 1.5), currentLevel));
        if (!economyAPI.takeClam(player.getUniqueId(), cost)) {
            player.sendMessage("§c조개가 부족합니다. (필요: " + cost + ")");
            return;
        }

        consumeOne(inventory, ForgeGUI.REINFORCE_STONE_SLOT, stone);

        int successRate = plugin.getConfig().getInt("forge.reinforce-success-rate." + nextLevel, 25);
        boolean success = ThreadLocalRandom.current().nextInt(100) < successRate;

        if (success) {
            pdc.set(TideKeys.REINFORCE, PersistentDataType.INTEGER, nextLevel);
            player.sendMessage("§a강화 성공! §f+" + currentLevel + " §a→ §f+" + nextLevel);
            com.tide.core.effect.EffectEngine effectEngine = org.bukkit.Bukkit.getServicesManager().load(com.tide.core.effect.EffectEngine.class);
            if (effectEngine != null) {
                effectEngine.playEffect(player, nextLevel >= 7 ? "forge_success_high" : "forge_success_low");
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.4f);
            }
        } else {
            ItemStack scroll = inventory.getItem(ForgeGUI.REINFORCE_SCROLL_SLOT);
            boolean hasScroll = scroll != null && "protection_scroll".equals(itemIdOf(scroll));
            if (nextLevel <= 2) {
                player.sendMessage("§e강화 실패. §7(단계 유지: +" + currentLevel + ")");
            } else if (hasScroll) {
                consumeOne(inventory, ForgeGUI.REINFORCE_SCROLL_SLOT, scroll);
                player.sendMessage("§e강화 실패했지만 보호권으로 단계를 유지했습니다. §7(+" + currentLevel + ")");
            } else {
                int downgraded = Math.max(0, currentLevel - 1);
                pdc.set(TideKeys.REINFORCE, PersistentDataType.INTEGER, downgraded);
                player.sendMessage("§c강화 실패! §f+" + currentLevel + " §c→ §f+" + downgraded);
            }
            com.tide.core.effect.EffectEngine effectEngine = org.bukkit.Bukkit.getServicesManager().load(com.tide.core.effect.EffectEngine.class);
            if (effectEngine != null) {
                effectEngine.playEffect(player, "forge_fail");
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }

        gear.setItemMeta(gearMeta);
        loreRenderer.render(gear);
        inventory.setItem(ForgeGUI.REINFORCE_GEAR_SLOT, gear);
    }

    // ---------- Socket attach ----------

    private void handleSocketAttach(Player player, Inventory inventory) {
        ItemStack gear = inventory.getItem(ForgeGUI.SOCKET_GEAR_SLOT);
        if (gear == null || itemIdOf(gear) == null) {
            player.sendMessage("§c장착할 장비를 배치하세요.");
            return;
        }
        ItemMeta gearMeta = gear.getItemMeta();
        var pdc = gearMeta.getPersistentDataContainer();
        int socketCount = pdc.getOrDefault(TideKeys.SOCKET_COUNT, PersistentDataType.INTEGER, 0);

        int[] socketSlots = {ForgeGUI.SOCKET_1_SLOT, ForgeGUI.SOCKET_2_SLOT, ForgeGUI.SOCKET_3_SLOT};
        boolean attachedAny = false;
        for (int i = 0; i < socketCount && i < socketSlots.length; i++) {
            ItemStack runeItem = inventory.getItem(socketSlots[i]);
            String runeId = runeItemFactory.readRuneId(runeItem);
            if (runeId == null) {
                continue;
            }
            RuneDefinition runeDefinition = runeRegistry.getAll().get(runeId);
            if (runeDefinition == null) {
                continue;
            }
            pdc.set(TideKeys.socket(i + 1), PersistentDataType.STRING,
                    runeDefinition.getType() + ":" + runeDefinition.getGrade());
            consumeOne(inventory, socketSlots[i], runeItem);
            attachedAny = true;
        }

        if (!attachedAny) {
            player.sendMessage("§c소켓에 장착할 룬을 놓으세요.");
            return;
        }

        gear.setItemMeta(gearMeta);
        loreRenderer.render(gear);
        inventory.setItem(ForgeGUI.SOCKET_GEAR_SLOT, gear);
        player.sendMessage("§a룬을 장착했습니다.");
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
    }

    // ---------- Reroll ----------

    private void handleReroll(Player player, Inventory inventory) {
        ItemStack gear = inventory.getItem(ForgeGUI.REROLL_GEAR_SLOT);
        if (gear == null || itemIdOf(gear) == null) {
            player.sendMessage("§c리롤할 장비를 배치하세요.");
            return;
        }
        ItemMeta gearMeta = gear.getItemMeta();
        var pdc = gearMeta.getPersistentDataContainer();
        int socketCount = pdc.getOrDefault(TideKeys.SOCKET_COUNT, PersistentDataType.INTEGER, 0);

        List<Integer> occupied = new ArrayList<>();
        for (int i = 1; i <= socketCount; i++) {
            String raw = pdc.get(TideKeys.socket(i), PersistentDataType.STRING);
            if (raw != null && !raw.isBlank()) {
                occupied.add(i);
            }
        }
        if (occupied.isEmpty()) {
            player.sendMessage("§c리롤할 룬이 장착된 소켓이 없습니다.");
            return;
        }

        long pearlCost = plugin.getConfig().getLong("reroll.pearl-cost", 5);
        if (!economyAPI.takePearl(player.getUniqueId(), pearlCost)) {
            player.sendMessage("§c진주가 부족합니다. (필요: " + pearlCost + ")");
            return;
        }

        List<RuneDefinition> pool = new ArrayList<>(runeRegistry.getAll().values());
        if (pool.isEmpty()) {
            player.sendMessage("§c리롤 가능한 룬 풀이 비어 있습니다.");
            return;
        }
        int targetSocket = occupied.get(ThreadLocalRandom.current().nextInt(occupied.size()));
        RuneDefinition newRune = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        pdc.set(TideKeys.socket(targetSocket), PersistentDataType.STRING,
                newRune.getType() + ":" + newRune.getGrade());

        gear.setItemMeta(gearMeta);
        loreRenderer.render(gear);
        inventory.setItem(ForgeGUI.REROLL_GEAR_SLOT, gear);
        player.sendMessage("§b소켓 " + targetSocket + "번이 §f" + newRune.getDisplayName() + " §b로 리롤되었습니다.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    // ---------- Fusion ----------

    private void handleFusion(Player player, Inventory inventory) {
        ItemStack a = inventory.getItem(ForgeGUI.FUSION_MATERIAL_1_SLOT);
        ItemStack b = inventory.getItem(ForgeGUI.FUSION_MATERIAL_2_SLOT);
        ItemStack c = inventory.getItem(ForgeGUI.FUSION_MATERIAL_3_SLOT);
        String idA = runeItemFactory.readRuneId(a);
        String idB = runeItemFactory.readRuneId(b);
        String idC = runeItemFactory.readRuneId(c);

        if (idA == null || !idA.equals(idB) || !idA.equals(idC)) {
            player.sendMessage("§c동일한 룬 3개를 모두 놓아야 합니다.");
            return;
        }

        RuneDefinition recipe = runeRegistry.findFusionRecipeFor(idA).orElse(null);
        if (recipe == null) {
            player.sendMessage("§c이 룬은 더 이상 융합할 수 없습니다.");
            return;
        }
        if (!economyAPI.takeClam(player.getUniqueId(), recipe.getFusionCostClam())) {
            player.sendMessage("§c조개가 부족합니다. (필요: " + recipe.getFusionCostClam() + ")");
            return;
        }

        inventory.setItem(ForgeGUI.FUSION_MATERIAL_1_SLOT, null);
        inventory.setItem(ForgeGUI.FUSION_MATERIAL_2_SLOT, null);
        inventory.setItem(ForgeGUI.FUSION_MATERIAL_3_SLOT, null);

        ItemStack result = runeItemFactory.create(recipe.getId());
        player.getInventory().addItem(result);
        player.sendMessage("§a룬 융합 성공! §f" + recipe.getDisplayName() + " §a획득.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    // ---------- helpers ----------

    private String itemIdOf(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(TideKeys.ITEM_ID, PersistentDataType.STRING);
    }

    private void consumeOne(Inventory inventory, int slot, ItemStack stack) {
        if (stack.getAmount() <= 1) {
            inventory.setItem(slot, null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
            inventory.setItem(slot, stack);
        }
    }

    private void restorePlaceholder(Inventory inventory, ForgeTab tab, int slot) {
        if (tab == ForgeTab.REINFORCE) {
            if (slot == ForgeGUI.REINFORCE_STONE_SLOT) {
                inventory.setItem(slot, createMarker(Material.QUARTZ, "§7강화석을 놓으세요"));
            } else if (slot == ForgeGUI.REINFORCE_GEAR_SLOT) {
                inventory.setItem(slot, createMarker(Material.IRON_SWORD, "§7강화할 장비를 놓으세요"));
            } else if (slot == ForgeGUI.REINFORCE_SCROLL_SLOT) {
                inventory.setItem(slot, createMarker(Material.PAPER, "§7보호권 (선택)"));
            }
        } else if (tab == ForgeTab.SOCKET) {
            if (slot == ForgeGUI.SOCKET_GEAR_SLOT) {
                inventory.setItem(slot, createMarker(Material.IRON_SWORD, "§7장비를 놓으세요"));
            } else if (slot == ForgeGUI.SOCKET_1_SLOT) {
                inventory.setItem(slot, createMarker(Material.GRAY_STAINED_GLASS_PANE, "§7소켓 1 - 룬을 놓으세요"));
            } else if (slot == ForgeGUI.SOCKET_2_SLOT) {
                inventory.setItem(slot, createMarker(Material.GRAY_STAINED_GLASS_PANE, "§7소켓 2 - 룬을 놓으세요"));
            } else if (slot == ForgeGUI.SOCKET_3_SLOT) {
                inventory.setItem(slot, createMarker(Material.GRAY_STAINED_GLASS_PANE, "§7소켓 3 - 룬을 놓으세요"));
            }
        } else if (tab == ForgeTab.REROLL) {
            if (slot == ForgeGUI.REROLL_GEAR_SLOT) {
                inventory.setItem(slot, createMarker(Material.IRON_SWORD, "§7장비를 놓으세요"));
            }
        } else if (tab == ForgeTab.FUSION) {
            if (slot == ForgeGUI.FUSION_MATERIAL_1_SLOT) {
                inventory.setItem(slot, createMarker(Material.AMETHYST_SHARD, "§7재료 룬 1"));
            } else if (slot == ForgeGUI.FUSION_MATERIAL_2_SLOT) {
                inventory.setItem(slot, createMarker(Material.AMETHYST_SHARD, "§7재료 룬 2"));
            } else if (slot == ForgeGUI.FUSION_MATERIAL_3_SLOT) {
                inventory.setItem(slot, createMarker(Material.AMETHYST_SHARD, "§7재료 룬 3"));
            }
        }
    }

    private ItemStack createMarker(Material material, String name) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private boolean isValidForSlot(ForgeTab tab, int slot, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (isMarker(item)) {
            return false;
        }

        switch (tab) {
            case REINFORCE -> {
                if (slot == ForgeGUI.REINFORCE_GEAR_SLOT) {
                    return itemIdOf(item) != null;
                }
                if (slot == ForgeGUI.REINFORCE_STONE_SLOT) {
                    return "reinforce_stone".equals(itemIdOf(item));
                }
                if (slot == ForgeGUI.REINFORCE_SCROLL_SLOT) {
                    return "protection_scroll".equals(itemIdOf(item));
                }
            }
            case SOCKET -> {
                if (slot == ForgeGUI.SOCKET_GEAR_SLOT) {
                    return itemIdOf(item) != null;
                }
                if (slot == ForgeGUI.SOCKET_1_SLOT || slot == ForgeGUI.SOCKET_2_SLOT || slot == ForgeGUI.SOCKET_3_SLOT) {
                    return runeItemFactory.readRuneId(item) != null;
                }
            }
            case REROLL -> {
                if (slot == ForgeGUI.REROLL_GEAR_SLOT) {
                    return itemIdOf(item) != null;
                }
            }
            case FUSION -> {
                if (slot == ForgeGUI.FUSION_MATERIAL_1_SLOT || slot == ForgeGUI.FUSION_MATERIAL_2_SLOT || slot == ForgeGUI.FUSION_MATERIAL_3_SLOT) {
                    return runeItemFactory.readRuneId(item) != null;
                }
            }
        }
        return false;
    }

    private void updateForgeInfo(Inventory inventory, ForgeTab tab) {
        if (tab == ForgeTab.REINFORCE) {
            ItemStack gear = inventory.getItem(ForgeGUI.REINFORCE_GEAR_SLOT);
            if (gear != null && !isMarker(gear)) {
                ItemMeta meta = gear.getItemMeta();
                if (meta != null) {
                    var pdc = meta.getPersistentDataContainer();
                    int currentLevel = pdc.getOrDefault(TideKeys.REINFORCE, PersistentDataType.INTEGER, 0);
                    if (currentLevel < 10) {
                        int nextLevel = currentLevel + 1;
                        int successRate = plugin.getConfig().getInt("forge.reinforce-success-rate." + nextLevel, 25);
                        long cost = Math.round(plugin.getConfig().getDouble("forge.reinforce-cost-base", 100)
                                * Math.pow(plugin.getConfig().getDouble("forge.reinforce-cost-multiplier", 1.5), currentLevel));
                        
                        ItemStack infoBook = new ItemStack(Material.BOOK);
                        ItemMeta infoMeta = infoBook.getItemMeta();
                        if (infoMeta != null) {
                            infoMeta.setDisplayName("§e⚒ 강화 예측 정보");
                            infoMeta.setLore(List.of(
                                "§7현재 단계: §f+" + currentLevel,
                                "§7다음 단계: §a+" + nextLevel,
                                "§7성공 확률: §e" + successRate + "%",
                                "§7필요 비용: §6" + cost + " 조개"
                            ));
                            infoBook.setItemMeta(infoMeta);
                            inventory.setItem(ForgeGUI.REINFORCE_INFO_SLOT, infoBook);
                        }
                    } else {
                        ItemStack infoBook = new ItemStack(Material.BOOK);
                        ItemMeta infoMeta = infoBook.getItemMeta();
                        if (infoMeta != null) {
                            infoMeta.setDisplayName("§e⚒ 강화 예측 정보");
                            infoMeta.setLore(List.of("§a최대 강화 단계에 도달했습니다."));
                            infoBook.setItemMeta(infoMeta);
                            inventory.setItem(ForgeGUI.REINFORCE_INFO_SLOT, infoBook);
                        }
                    }
                }
            } else {
                ItemStack infoBook = new ItemStack(Material.BOOK);
                ItemMeta infoMeta = infoBook.getItemMeta();
                if (infoMeta != null) {
                    infoMeta.setDisplayName("§e강화 정보");
                    infoMeta.setLore(List.of("§7장비를 배치하면 표시됩니다"));
                    infoBook.setItemMeta(infoMeta);
                    inventory.setItem(ForgeGUI.REINFORCE_INFO_SLOT, infoBook);
                }
            }
        }
    }
}
