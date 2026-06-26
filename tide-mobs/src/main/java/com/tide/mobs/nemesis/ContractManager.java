package com.tide.mobs.nemesis;

import com.tide.core.economy.EconomyAPI;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounty Brokerage (3-3): the upfront clam is paid out the moment a mercenary
 * accepts (commitment fee); the pearl reward only pays out on a confirmed
 * kill, handled by NemesisRewardListener via {@link #findByNemesisMob}.
 */
public final class ContractManager {

    private final NemesisManager nemesisManager;
    private final EconomyAPI economyAPI;
    private final Map<UUID, ContractRecord> contracts = new ConcurrentHashMap<>();

    public ContractManager(NemesisManager nemesisManager, EconomyAPI economyAPI) {
        this.nemesisManager = nemesisManager;
        this.economyAPI = economyAPI;
    }

    public ContractRecord post(Player poster, long upfrontClam, long rewardPearl) {
        NemesisRecord nemesis = nemesisManager.getActiveFor(poster.getUniqueId());
        if (nemesis == null) {
            poster.sendMessage("§c현재 활성화된 네메시스가 없습니다.");
            return null;
        }
        for (ContractRecord existing : contracts.values()) {
            if (existing.getNemesisMobUuid().equals(nemesis.getMobUuid())) {
                poster.sendMessage("§c이미 이 네메시스에 대한 계약이 등록되어 있습니다.");
                return null;
            }
        }
        if (upfrontClam > 0 && !economyAPI.takeClam(poster.getUniqueId(), upfrontClam)) {
            poster.sendMessage("§c선불금으로 지불할 조개가 부족합니다.");
            return null;
        }

        ContractRecord contract = new ContractRecord(UUID.randomUUID(), poster.getUniqueId(),
                nemesis.getMobUuid(), nemesis.getOriginalName(), upfrontClam, rewardPearl);
        contracts.put(contract.getContractId(), contract);
        poster.sendMessage("§a현상금 계약을 등록했습니다. §7(선불금 " + upfrontClam + " 조개 지불, 성공보수 " + rewardPearl + " 진주)");
        return contract;
    }

    public boolean accept(Player mercenary, UUID contractId) {
        ContractRecord contract = contracts.get(contractId);
        if (contract == null || contract.isAccepted()) {
            return false;
        }
        if (contract.getPosterUuid().equals(mercenary.getUniqueId())) {
            mercenary.sendMessage("§c자신의 계약은 수락할 수 없습니다.");
            return false;
        }
        contract.accept(mercenary.getUniqueId());
        if (contract.getUpfrontClam() > 0) {
            economyAPI.addClam(mercenary.getUniqueId(), contract.getUpfrontClam());
        }
        mercenary.sendMessage("§a현상금 계약을 수락했습니다. §f" + contract.getNemesisName()
                + " §a을(를) 처치하면 진주 " + contract.getRewardPearl() + "개를 받습니다.");
        return true;
    }

    public List<ContractRecord> openContracts() {
        return contracts.values().stream().filter(c -> !c.isAccepted()).toList();
    }

    public ContractRecord findByNemesisMob(UUID mobUuid) {
        for (ContractRecord contract : contracts.values()) {
            if (contract.getNemesisMobUuid().equals(mobUuid)) {
                return contract;
            }
        }
        return null;
    }

    /** Called once the bound nemesis is actually dead, win or lose, to clear the board. */
    public void resolve(UUID mobUuid, boolean mercenarySucceeded) {
        ContractRecord contract = findByNemesisMob(mobUuid);
        if (contract == null) {
            return;
        }
        contracts.remove(contract.getContractId());
        if (mercenarySucceeded && contract.getMercenaryUuid() != null && contract.getRewardPearl() > 0) {
            economyAPI.addPearl(contract.getMercenaryUuid(), contract.getRewardPearl());
            var mercenary = org.bukkit.Bukkit.getPlayer(contract.getMercenaryUuid());
            if (mercenary != null) {
                mercenary.sendMessage("§d현상금 계약 성공보수로 진주 " + contract.getRewardPearl() + "개를 받았습니다.");
            }
        }
    }
}
