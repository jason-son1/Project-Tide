# The Tide v2 — 전체 기능 목록 정리

> 이 문서는 구현된 기능을 플레이어·서버 제작자 관점에서 설명문으로 바꾸기 위한 **기능 원문 목록**이다.
> 3개 플러그인 Jar (TideCore / TideRPG / TideMobs) + 웹 도구 2종으로 구성된다.

---

## 목차

1. [TideCore — 핵심 엔진](#1-tidecore--핵심-엔진)
2. [TideRPG — RPG 시스템](#2-tiderpg--rpg-시스템)
3. [TideMobs — 몹 시스템](#3-tidemobs--몹-시스템)
4. [관리자 도구 — 웹 서버 & 외부 도구](#4-관리자-도구--웹-서버--외부-도구)
5. [플레이어 명령어 목록](#5-플레이어-명령어-목록)
6. [관리자 명령어 목록](#6-관리자-명령어-목록)
7. [설정 파일 구조 & 스키마](#7-설정-파일-구조--스키마)

---

## 1. TideCore — 핵심 엔진

### 1-1. 조수(Tide) 스케줄러

**상태 종류 (TideState)**

| 상태 | 표시명 | 설명 |
|------|--------|------|
| HIGH_TIDE | 🌊 밀물 | 기본 상태. BossBar 파란색. |
| LOW_TIDE | 💨 썰물 | 밀물과 교대. BossBar 보라색. |
| SPRING_TIDE | 🌟 사리 | 확률 또는 주 1회 예약 발생. BossBar 노란색. |
| BLOOD_MOON | 🩸 블러드문 | 썰물+야간 조건에서 확률 발생. BossBar 빨간색. |
| BLOOD_TIDE | ☠️ 블러드 사리 | 사리+블러드문 동시 트리거. BossBar 빨간색. |

**동작 방식**
- 매 1초(20틱)마다 카운트다운. 0에 도달하면 다음 상태로 전환.
- 전환 시 `TideChangeEvent` 커스텀 이벤트 발행 → TideRPG·TideMobs가 이벤트를 수신하여 각자 처리 (드롭률 배율 변경, 몹 스폰 확률 변경 등).
- 모든 플레이어 화면 상단에 BossBar로 현재 상태와 "다음 변동까지 HH:MM:SS" 실시간 표시.
- 사리: `config.yml`의 `tide.spring-tide-chance` (기본 5%) 확률 + `tide.scheduled-spring-day` 요일에 반드시 발생.
- 블러드문: 야간(`time >= 13000 && time <= 23000`) + `tide.blood-moon-chance` (기본 8%) 확률.
- 블러드 사리: 사리와 블러드문 조건이 동시에 충족될 때.
- `/tide admin` 또는 `/tide set` 명령어로 강제 전환 가능. 사리는 5분 한정으로 강제 발동 가능.
- `tide.cycle-duration-minutes` 값으로 사이클 길이 핫 리로드 가능.

---

### 1-2. 이중 화폐 & 경제 시스템

**화폐 종류**

| 화폐 | 단위 | 용도 |
|------|------|------|
| 조개 (Clam) | Long | 일반 거래, 강화 비용, 사망 페널티 |
| 진주 (Pearl) | Long | 보호권 구매 등 프리미엄 소비 |
| 평판 (Rep) | Int | 네메시스 처치·퀘스트 완료로 증가 |

**평판 등급 (RepTier)**

| 등급 | 기준 |
|------|------|
| BRONZE | 기본 |
| SILVER | 일정 Rep 이상 |
| GOLD | 더 높은 Rep |
| TIDE_MASTER | 최고 등급 |

**저장 방식**
- SQLite (`/plugins/TideCore/data/economy.db`, 테이블: `player_economy`).
- SQLite 초기화 실패 시 YAML 파일(`/plugins/TideCore/data/players.yml`)로 자동 폴백.
- 서버 접속 시 플레이어 데이터 캐시 로드. 퇴장·서버 종료 시 동기 저장.
- 인게임 중 캐시 기반 조회 (메인 스레드 블로킹 없음).

**EconomyAPI (타 Jar 연동 인터페이스)**
- `getClam(UUID)` / `addClam(UUID, amount)` / `takeClam(UUID, amount)` → 잔액 부족 시 false
- `getPearl(UUID)` / `addPearl(UUID, amount)` / `takePearl(UUID, amount)`
- `addRep(UUID, amount)` / `getRepTier(UUID)`
- `isHardMode(UUID)` / 기타 조회 메서드
- Bukkit ServicesManager에 등록 → TideRPG·TideMobs가 직접 클래스 참조 없이 호출.

**인플레이션 모니터링**
- Admin GUI에서 서버 온라인 플레이어 전체 보유 조개 총량 실시간 확인 가능.
- `economy.inflation-alert-threshold` 임계값 초과 시 Admin GUI의 인플레이션 슬롯 색 변경(초록→노랑→빨강).

---

### 1-3. 소프트 하드코어 사망 — "조류에 휩쓸림"

**일반 사망 처리 순서**
1. 보유 조개의 `death_penalty.clam-loss-percent` (기본 10%) 차감.
2. 인벤토리 드롭 취소 → 사망 위치에 `WreckageGrave`(유실물 비석) 생성.
   - 외형: ArmorStand. 이름: `§b[유실물 비석] §7남은 시간: MM:SS`.
   - 아이템 목록은 서버 메모리에서 비석 UUID에 매핑 보관.
3. 부활 후 `Weakness II` 60초 디버프 부여.
4. 스코어보드 HUD에 비석 거리·남은 시간·좌표 표시 (비석 나침반 모드 전환).

**비석 만료**
- 생성 후 `death_penalty.grave-duration-seconds` (기본 600초 = 10분) 경과 시 ArmorStand 자동 제거, 아이템 삭제.

**비석 회수**
- 비석 ArmorStand에 우클릭 → 본인 UUID 일치 시 아이템 전부 인벤토리 반환, 비석 제거.
- 타인 비석: 메시지만 출력 ("다른 모험가의 유산입니다.").
- 회수 또는 소멸 시 스코어보드 HUD가 일반 모드(조개·진주·평판 표시)로 복귀.

**딥 마인 사망 (별도 처리)**
- `DeepMineListener`가 LOWEST 우선순위로 먼저 처리 → 딥 마인 전용 로직 적용.
- `tide_deepmine_death` 메타데이터 플래그로 일반 비석 생성 건너뜀.

**하드 모드 (선택)**
- `/hardcore` 명령어로 토글. `PlayerEconomy.hardMode` 필드에 저장.
- 하드 모드 활성화 상태에서 사망 시: 착용 장비 + 메인핸드 중 최고 강화 단계 아이템의 `tide:reinforce` PDC값 -1 감소.
- "최고 강화 장비의 강화 단계가 1 하락했습니다." 메시지 출력.

---

### 1-4. 스코어보드 HUD (EconomyScoreboardHud)

**일반 모드 표시 항목**
- 🌊 The Tide (제목)
- 현재 조수 상태
- 보유 조개 / 진주 / 평판 등급

**비석 나침반 모드 (비석 생성 시 자동 전환)**
- 비석까지의 거리 (m 단위)
- 남은 시간 (MM:SS)
- 비석 좌표 (X, Y, Z)
- 비석 소멸 또는 회수 시 일반 모드로 자동 복귀.

**적용 시점**
- 서버 접속(PlayerJoinEvent) 시 자동 설정.
- 이미 접속 중인 플레이어는 플러그인 활성화 시 일괄 적용.

---

### 1-5. Admin GUI (`/tide admin`)

**54칸 인벤토리 레이아웃**

| 구역 | 슬롯 | 기능 |
|------|------|------|
| 조수 제어 | 9~14 | 밀물/썰물/사리(5분)/블러드문 강제 전환, 현재 상태·카운트다운 표시 |
| 경제 모니터링 | 18~20 | 온라인 총 조개 / 총 진주 / 인플레이션 상태 |
| 리로드 버튼 | 27~30 | 아이템·접두사·룬·전체 리로드 |
| 플레이어 목록 | 36~44 | 온라인 플레이어 머리 표시 (클릭 시 clam/pearl/rep 조회 가능) |

---

### 1-6. 핫 리로드 (`/tide reload`)

- `/tide reload` — 등록된 모든 리로드 대상 실행.
- `/tide reload config` — `TideCore/config.yml` 재로드 (조수 주기·경제 설정 즉시 반영).
- `/tide reload items` — TideRPG의 `items/` 디렉토리 전체 재스캔.
- `/tide reload runes` — TideRPG의 `runes/` 디렉토리 전체 재스캔.
- `/tide reload mobs` — TideMobs의 `mobs/` 디렉토리 전체 재스캔.
- `/tide reload affixes` — TideMobs의 `affixes/` 디렉토리 전체 재스캔.
- 각 리로드는 메인 스레드에서 실행. 결과(성공 N건, 실패 M건) 콘솔 출력.
- `ReloadManager`에 `Reloadable` 인터페이스를 구현한 Registry를 등록하는 구조.
- TideRPG·TideMobs도 각자의 Registry를 TideCore의 ReloadManager에 등록.

---

## 2. TideRPG — RPG 시스템

### 2-1. PDC 기반 장비 스탯 시스템

**PDC 키 목록** (네임스페이스: `tide`)

| 키 | 타입 | 설명 |
|----|------|------|
| `tide:item_id` | STRING | 아이템 고유 ID (예: `iron_sword_t1`) |
| `tide:gs` | INTEGER | Gear Score |
| `tide:reinforce` | INTEGER | 강화 단계 (+0 ~ +10) |
| `tide:socket_count` | INTEGER | 소켓 수 (0 ~ 3) |
| `tide:socket_1` ~ `tide:socket_3` | STRING | 장착된 룬 ("lifesteal:2" 형식) |
| `tide:cmd` | INTEGER | CustomModelData |

**원칙**
- 모든 RPG 수치는 PDC에만 저장. Lore는 PDC 값을 읽어 렌더링만 함. Lore 문자열 파싱 절대 금지.
- `ItemFactory.create(itemId)`: `items/` 디렉토리의 YAML을 로드 → ItemStack 생성 → PDC 기록 → Lore 렌더링 후 반환.
- `LoreRenderer.render(ItemStack)`: PDC 읽어서 § 컬러코드 Lore 재생성.
- `ItemRegistry`: 서버 시작 시 `items/` 전체 로드. `reload()` 메서드 제공.

**샘플 아이템 (8종 번들 포함)**

| ID | 설명 |
|----|------|
| `iron_sword_t1` | Tier 1 검 샘플 |
| `flame_sword_t1` | 불꽃 속성 검 샘플 |
| `leather_armor_t1` | Tier 1 갑옷 샘플 |
| `reinforce_stone` | 강화석 (강화 재료) |
| `protection_scroll` | 보호권 (강화 실패 시 하락 방지) |
| `soul_fragment` | 영혼 파편 (보스 소환 재료) |
| `nemesis_token` | 네메시스의 징표 (네메시스 처치 보상) |
| `tide_bell` | 조수 종 (특수 아이템) |

---

### 2-2. 스타포스 강화 시스템 (`/forge` — 강화 탭)

**강화 확률 테이블**

| 단계 | 성공률 | 실패 시 |
|------|--------|---------|
| +1 | 100% | 유지 |
| +2 | 90% | 유지 |
| +3 | 80% | +2로 하락 |
| +4 | 70% | +3으로 하락 |
| +5 | 60% | +4로 하락 |
| +6 | 50% | +5로 하락 |
| +7 | 40% | +6으로 하락 |
| +8 | 35% | +7로 하락 |
| +9 | 30% | +8로 하락 |
| +10 | 25% | +9로 하락 (보호권 사용 시 유지) |

- **파괴 없음** — rage quit 방지 설계.
- 강화 비용: 조개 소모 (단계별 차등, `forge_config.yml`에서 설정).
- 성공/실패 시 파티클·사운드 출력.
- 강화 단계에 따른 데미지/방어력 보너스: 아이템 YAML의 `reinforce_bonus` 필드 참조.

**GUI 슬롯 배치**
- 슬롯 11: 강화석 배치.
- 슬롯 13: 강화할 장비 배치.
- 슬롯 15: 보호권 배치 (선택).
- 슬롯 29: 현재 강화 정보 (단계·다음 성공률·비용) 표시.
- 슬롯 31: [강화 시도] 버튼.

---

### 2-3. 룬 시스템

**룬 장착 탭 (`/forge` — 룬 장착)**
- 슬롯 13: 장비 배치.
- 슬롯 10·11·12: 소켓 1·2·3에 룬 배치.
- 슬롯 31: [장착] 버튼 → PDC `tide:socket_N` 값에 룬 ID 기록, Lore 갱신.

**룬 리롤 탭 (`/forge` — 룬 리롤)**
- 슬롯 13: 장비 배치.
- 슬롯 22: [리롤] 버튼 → 진주 N개 소모, 현재 소켓의 룬을 랜덤 재배정.
- 슬롯 24: 현재 소켓 상태 표시.
- 슬롯 26: 리롤 풀 안내 (무작위 등급 1~2 룬).

**룬 융합 탭 (`/forge` — 룬 융합)**
- 슬롯 10·11·12: 동일한 룬 3개 배치.
- 슬롯 22: [융합] 버튼 → 재료 3개 소모 + 조개 비용 → 상위 등급 룬 생성.
- 슬롯 24: 결과 룬 미리보기.
- 융합 레시피는 룬 YAML의 `fusion` 필드에서 정의.

**샘플 룬 (3종 번들 포함)**

| ID | 이름 | 등급 |
|----|------|------|
| `rune_lifesteal_1` | 흡혈의 룬 I | 1 |
| `rune_lifesteal_2` | 흡혈의 룬 II | 2 |
| `rune_lightning_1` | 번개의 룬 I | 1 |

---

### 2-4. 전투 리스너 — 룬 효과

**`CombatListener` (EntityDamageByEntityEvent 처리 흐름)**

1. 공격자가 Player인지 확인.
2. 메인핸드 아이템 PDC에서 `tide:socket_1 ~ 3` 순회.
3. "룬타입:등급" 파싱 → `RuneEffectDispatcher.dispatch()` 호출.

**공격 시 발동 룬 효과**

| 룬 | 효과 | 계산식 |
|----|------|--------|
| lifesteal (흡혈의 룬) | 피해량의 일부를 공격자 회복 | `finalDamage * (0.08 * grade)` |
| lightning (번개의 룬) | 확률로 번개 + 추가 피해 | 발동 확률 `0.10 * grade` |
| slow (둔화의 룬) | 피격자에게 SLOW 포션 효과 | 지속 `40 + (grade * 20)` ticks |
| berserk (광폭의 룬, 저주) | 공격 +30% 데미지, 수신 +15% 데미지 | DamageModifier 적용 |

**피격 시 발동 룬 효과 (DefensiveListener)**

| 룬 | 효과 | 계산식 |
|----|------|--------|
| shield (방벽의 룬) | 피격 시 흡수막 부여 | `5.0 * grade` 흡수막 |

**세트 보너스 (SetBonus)**
- 소켓 1~3 모두 동일한 룬 타입 장착 시 추가 보너스 효과 발동.

---

### 2-5. GearScore 계산기 (GearScoreCalculator)

- 투구·흉갑·레깅스·부츠·메인핸드 아이템의 PDC `tide:gs` 합산.
- 강화 단계 보너스 포함: `gs + (reinforce * gs_per_star)`.
- `gs_per_star` 값은 `config.yml`에서 설정 (기본값: 5).
- GS 기반 지역 입장 경고, 보스 제단 접근 경고, Admin GUI 플레이어 정보 표시에 활용.

---

### 2-6. GS 기반 지역 경고 (ZoneGuardListener)

- `PlayerMoveEvent` (청크 경계 체크로 최적화) → 플레이어가 GS 게이트 구역 진입 확인.
- 구역 설정은 `zones/` 디렉토리의 YAML 파일 (`ZoneDefinition` 스키마).
- 권장 GS 미만: `§e주의` 타이틀 + 현재 GS / 권장 GS 표시.
- 경고 GS 미만: `§c⚠ 위험` 타이틀 + 표시.
- 샘플 구역 YAML 2종 번들: `deep_mine`, `boss_arena`.

---

### 2-7. 딥 마인 인스턴스 (DeepMine)

**특징**
- 별도 월드 복사 없이 기존 월드의 특정 직육면체 영역을 "인스턴스"로 사용.
- `config.yml`의 `deepmine.bounds` (min/max X·Y·Z)로 영역 정의.
- 주기적(기본 30분, `deepmine.reset-interval-minutes`) 리셋: 영역 내 블록을 광석 풀에서 무작위 재배치.
  - 광석 풀: COAL·IRON·GOLD·REDSTONE·LAPIS·DIAMOND·EMERALD (가중치 있음).
  - 블록 쓰기를 틱당 4000블록씩 분산하여 랙 스파이크 방지.

**딥 마인 사망**
- 딥 마인 내 사망 시 일반 비석 생성 없음 → 대신 아이템 일부 소실 후 입구로 추방.
- `DeepMineListener`가 `PlayerDeathEvent`를 LOWEST 우선순위로 선처리.

**명령어**
- `/deepmine reset` — 관리자가 즉시 리셋 실행.
- `/deepmine status` — 현재 리셋 타이머 상태 확인.
- `/deepmine tp` — 딥 마인 입구로 순간이동.

---

### 2-8. 낚시 QTE 미니게임 (FishingQteListener)

- `FishingHoleRegistry`에 등록된 "낚시 포인트" 구역 내에서만 발동.
- 물고기가 미끼를 무는 순간(`PlayerFishEvent.State.BITE`) 액션바에 `§b🎣 지금 우클릭하세요!` 표시.
- 반응 시간에 따른 등급 판정:
  - PERFECT: 600ms 이내 → 최대 보상 (진주 지급 + Luck 포션 효과).
  - GOOD: 1500ms 이내 → 일반 보상 (조개 지급).
  - MISS: 1500ms 초과 → 실패.
- 샘플 낚시 포인트 YAML 1종 번들: `mythic_fishing_hole`.

---

### 2-9. 가상 상점 (`/shop`)

- `ShopGUI` (커스텀 인벤토리) 열기.
- `ShopListener` (InventoryClickEvent): 아이템 클릭 시 조개 잔액 확인 → 차감 → 아이템 지급.
- 상점 항목은 `ShopEntry` 목록으로 정의.
- 이중 화폐(조개/진주) 양쪽 모두 지원.

---

### 2-10. 전체 판매 (`/sellall`)

**처리 흐름**
1. `/sellall` 입력 → 인벤토리 전체 순회.
2. PDC `tide:item_id` 있는 아이템: 해당 YAML의 `sell_price` 참조.
3. PDC `tide:gs` 있는 장비류: 자동 제외 (실수 방지).
4. PDC `tide:item_id` 없는 바닐라 아이템: `VanillaPriceTable`에서 가격 조회.
5. 판매 예정 목록 채팅 출력 + "확인: `/sellall confirm` / 취소: `/sellall cancel`".
6. `/sellall confirm` → 아이템 제거 + 조개 지급.

---

### 2-11. 특수 아이템 리스너

**TideBellListener**
- `tide_bell` 아이템 사용 시 특정 이벤트 발동 (범위 내 몹에게 효과 등).

**TideVaultListener**
- 금고 아이템 사용 시 저장 공간 열기 또는 아이템 교환 처리.

---

## 3. TideMobs — 몹 시스템

### 3-1. 접두사(Affix) 정예 몹 시스템

**스폰 흐름 (EliteSpawnListener)**

1. 자연 스폰(`CreatureSpawnEvent`, 자연 스폰 이유만 처리).
2. 현재 TideState에 따라 정예 출현 확률 결정:
   - HIGH_TIDE: 5%
   - LOW_TIDE: 12%
   - SPRING_TIDE: 20%
   - BLOOD_MOON: 25%
3. 확률 통과 시 `AffixRegistry`에서 랜덤 접두사 1~2개 선택.
4. `EliteProcessor.apply()`:
   - 체력 배율 적용 (`hp_multiplier` 합산).
   - PDC에 `tide:affixes` 기록 ("접두사1,접두사2").
   - PDC에 `tide:elite = 1` 기록.
   - 이름 변경: `§c[접두사] §f몹이름`.
   - 발광(Glowing) 효과 적용.
   - 드롭 테이블 재설정 (정예 전용 드롭 추가).

**접두사별 전투 효과 (AffixCombatListener)**

| 접두사 | 영문 ID | 효과 | 발동 조건 |
|--------|---------|------|-----------|
| 염화의 | flame | 주변 3×3에 2초 FIRE 블록 생성 | 피격 시 30% 확률 |
| 신속의 | haste | 이동속도 +50%, 3블록 앞 순간이동 | 이동속도 상시, 이동 시 10% 확률 |
| 폭심의 | explosive | 사망 시 주변 폭발 (power=3, 불 없음) | 사망 시 |
| 분열의 | split | HP 50% 이하 시 동일 몹 1마리 추가 스폰 (최대 2회) | 피격 시 HP 확인 |
| 강철의 | iron | 수신 데미지 40% 감소 | 항상 |
| 재생의 | regen | 1초마다 최대 HP의 2% 회복 (스폰 시 BukkitRunnable 등록) | 항상 |
| 가시의 | thorns | 수신 데미지의 30%를 공격자에게 반사 | 항상 |
| 보호막의 | shield | 스폰 시 HP 20%에 해당하는 흡수막 부여. 소멸 후 15초 쿨다운 후 재생성. | 스폰 시, 재생성 |

**정예 드롭 (EliteDropListener)**
- 정예 몹 사망 시 아이템 YAML의 `drop_sources` 중 `mob: elite_any` 항목 확률로 추가 드롭.
- `ItemFactory`를 통해 PDC 완비된 아이템 생성 후 드롭.

---

### 3-2. 커스텀 몹 YAML 시스템 (MobRegistry + CustomMobSpawnListener)

- `mobs/` 디렉토리의 YAML 파일을 로드하여 커스텀 몹 등록.
- `CustomMobSpawnListener`: 자연 스폰 시 해당 위치·조수 상태·바이옴 조건과 일치하는 커스텀 몹 YAML이 있으면 스탯·접두사 덮어쓰기.
- 커스텀 몹도 정예화 적용 가능.

---

### 3-3. 네메시스(Nemesis) 시스템

**각성 조건**

| 트리거 | 설명 |
|--------|------|
| 플레이어 사망 | 정예 몹(`tide:affixes` PDC 보유)에게 사망 시 |
| 빈사 탈출 | 플레이어가 해당 몹을 상대하다 도망간 것으로 판정될 때 |

**각성 처리**
- 엔티티 이름 변경: `§4[네메시스] §c접두사들 §f몹이름 §8<플레이어명의 복수자>`.
- Strength II + Speed I 포션 효과 부여.
- 위치에 파티클 (FLAME + SMOKE_LARGE) 연출.
- 플레이어에게 타이틀: `§c[네메시스 각성]`.
- `kill_count`++: 같은 플레이어를 다시 죽일수록 기록 누적.

**추격 AI (NemesisTracker — 20틱 주기)**
- 활성 NemesisRecord 순회.
- 타겟 플레이어와 같은 월드 + 150블록 이내 → 타겟팅 설정.
- 플레이어 로그아웃 시 추격 일시 중지.
- 재접속 후 같은 월드 복귀 시 자동 재개.

**네메시스 처치 보상**
- 타겟 본인이 처치 시: 일반 드롭 × 3 + `nemesis_token` (네메시스의 징표) 드롭.
- 평판(rep) +50.
- 타이틀: `§a[복수 완료]`.
- `NemesisRecord.is_active = false`.

**데이터 저장**
- SQLite (`/plugins/TideMobs/data/nemesis.db`). 서버 재시작 후에도 네메시스 유지.
- SQLite 실패 시 YAML 폴백.
- NemesisManager를 ServicesManager에 등록 → 웹 서버 API로 조회 가능.

---

### 3-4. 현상금 보드 (`/bounty`)

**일일 퀘스트 (매일 자정 기준 갱신)**
- 플레이어별로 3개 무작위 생성.
- 퀘스트 종류: 특정 접두사 정예 몹 N마리 처치 / 일반 몹 N마리 처치 (BountyType).
- 처치 목표 수: 3~7마리.
- 완료 보상: 조개 100~250 + 평판 5~15.

**주간 퀘스트 (ISO 주 기준 갱신)**
- 플레이어별로 1개 무작위 생성.
- 처치 목표 수: 15~25마리.
- 완료 보상: 조개 800 + 평판 50.

**퀘스트 갱신 방식**
- 인메모리 상태. 날짜(일일) 또는 ISO 주(주간) 비교로 자동 갱신.
- `BountyKillListener`: 몹 처치 시 퀘스트 카운트 +1, 완료 시 보상 자동 지급.
- `BountyBoardGUI`: `/bounty` 명령어로 열기. 진행 상태 시각화.

---

### 3-5. 보스 시스템

**보스 소환 흐름**
1. 보스 제단 블록에 접근 (`AltarInteractListener`).
2. `AltarRegistry`에서 해당 제단 정의(`SoulAltar`) 로드.
3. 제단 조건 확인 (예: `soul_fragment` 아이템 N개 보유).
4. `BossFightManager.summon()` 호출.
   - 소환자 주변 30블록 내 파티 인원수 계산.
   - 보스 HP: `BASE_HP(200) * (1 + 0.5 * (파티 인원 - 1))` — 파티 스케일링.
   - 보스 이름·PDC 기록 (종류: `void_knight_altar` 기준 WITHER_SKELETON).
5. 이미 활성 보스가 있는 제단은 중복 소환 불가.

**보스 전투 패턴**
- 1초 주기 tick으로 페이즈·분노 타이머 관리.
- HP 50% 이하 → 페이즈 2 진입 (강화 패턴).
- 소환 후 300초(5분) 경과 → 분노 상태 (추가 강화).
- `BossCombatListener`: 보스 특수 피격·공격 로직 처리.

**보스 처치 보상**
- `EconomyAPI`를 통해 참여자에게 조개·진주 보상 지급.

---

## 4. 관리자 도구 — 웹 서버 & 외부 도구

### 4-1. 내장 웹 서버 (TideWebServer)

> TideCore.jar 안에 포함. 플러그인 활성화 시 자동 기동.

**기동 설정 (`/plugins/TideCore/config.yml`)**
```yaml
web-server:
  enabled: true
  port: 8080
```

**API 엔드포인트**

| 메서드 | 경로 | 기능 |
|--------|------|------|
| GET | `/api/configs` | 등록된 설정 파일 전체 목록 (item/rune/mob/affix/altar/global) |
| GET | `/api/configs/{category}/{id}` | 특정 YAML 파일 내용 읽기 |
| POST | `/api/configs/{category}/{id}` | 특정 YAML 파일 저장 + 자동 핫 리로드 트리거 |
| GET | `/api/logs` | 서버 `logs/latest.log` 마지막 100줄 반환 |
| POST | `/api/reload/{target}` | 원격으로 `/tide reload {target}` 실행 |
| GET | `/api/database/economy` | 전체 플레이어 경제 데이터 목록 |
| POST | `/api/database/economy` | 특정 플레이어 clam/pearl/rep/hardMode 수정 |
| GET | `/api/database/nemesis` | 활성 네메시스 레코드 목록 |
| GET | `/` (정적) | React 웹 앱 서빙 (JAR 내 `/web/` 리소스 또는 파일시스템 우선) |

**CORS**: 모든 Origin 허용 (`Access-Control-Allow-Origin: *`).

**설정 카테고리 매핑**

| category 값 | 실제 디렉토리 |
|-------------|--------------|
| item | TideRPG/items/ |
| rune | TideRPG/runes/ |
| mob | TideMobs/mobs/ |
| affix | TideMobs/affixes/ |
| altar | TideMobs/altars/ |
| global/core | TideCore/config.yml |
| global/rpg | TideRPG/config.yml |

---

### 4-2. Web Customizer (React + FastAPI)

> `tools/web-customizer/` 디렉토리. 별도 실행 필요 (개발/운영 보조 도구).

**프론트엔드 탭 구성 (React + Vite)**

| 탭 | 파일 | 기능 |
|----|------|------|
| 몹 생성기 | `MobTab.jsx` | 기반 몹 선택, 접두사 선택, 스탯 슬라이더, 스폰 조건, 드롭 설정, YAML 미리보기, 서버 배포 |
| 아이템 생성기 | `ItemTab.jsx` | 이름·재질·CMD 입력, GS 슬라이더, 기본 스탯, 강화 보너스, 소켓 수, 드롭 소스, YAML 미리보기 |
| 룬 생성기 | `RuneTab.jsx` | 룬 타입·등급 선택, 효과값 슬라이더, 융합 레시피 설정, YAML 미리보기 |
| 설정 편집기 | `ConfigTab.jsx` | 내장 웹서버 API로 기존 설정 YAML 조회·편집·저장 |
| DB 뷰어 | `DatabaseTab.jsx` | 경제 데이터 조회·수정, 네메시스 레코드 조회 |
| YAML 미리보기 | `YamlPreviewPanel.jsx` | 생성된 YAML 실시간 렌더링 |

**백엔드 API (FastAPI)**

| 메서드 | 경로 | 기능 |
|--------|------|------|
| POST | `/api/generate/{content_type}` | 폼 데이터 또는 AI 자연어로 YAML 생성 |
| POST | `/api/deploy` | 생성된 YAML을 DEPLOY_ROOT 디렉토리에 저장 + RCON으로 리로드 |
| GET | `/api/registry/{content_type}` | DEPLOY_ROOT 디렉토리의 등록 목록 반환 |

**RCON 연동 (선택)**
- 환경변수 `RCON_HOST`, `RCON_PASSWORD`, `RCON_PORT` 설정 시 활성화.
- `/api/deploy` 호출 후 `mcrcon` 라이브러리로 `/tide reload {target}` 전송.

---

### 4-3. AI 콘텐츠 생성기 (generate_content.py)

> `tools/ai-generator/` 디렉토리. CLI 도구.

**사용법**
```bash
python generate_content.py "공격력이 높고 피격 시 화염 장판을 까는 좀비 만들어줘"
→ output/mobs/{id}.yml 자동 생성
```

**지원 콘텐츠 타입**
- 몹 (시스템 프롬프트: `prompts/system_mob.txt`)
- 아이템 (`prompts/system_item.txt`)
- 룬 (`prompts/system_rune.txt`)

**처리 흐름**
1. 시스템 프롬프트 로드 (스키마 규칙 포함).
2. Claude API (`claude-sonnet-4-6`) 호출.
3. YAML 응답 파싱 + 유효성 검사 (필수 키 확인).
4. `output/{type}/` 디렉토리에 YAML 저장.
5. 결과 파일을 서버 `/plugins/TideXxx/{type}/`에 복사 → `/tide reload {type}`.

**AI 시스템 프롬프트 규칙**
- 출력은 유효한 YAML 코드만.
- 아이템 ID: 영문 소문자 + 언더스코어.
- 접두사: 8종 중에서만 선택.
- CMD 번호 대역: 아이템 1000번대, 몹 2000번대, 룬 3000번대.
- GS 범위: T1=100~199, T2=200~399, T3=400~699, T4=700~999, T5=1000+.
- 확률값: 0.00~1.00.

---

## 5. 플레이어 명령어 목록

| 명령어 | 설명 |
|--------|------|
| `/clam` | 보유 조개 잔액 확인 |
| `/pearl` | 보유 진주 잔액 확인 |
| `/hardcore` | 하드 모드 토글 (활성화 시 사망 시 장비 강화 단계 하락) |
| `/shop` | 가상 상점 GUI 열기 |
| `/forge` | 통합 대장간 GUI 열기 (강화·룬 장착·리롤·융합) |
| `/sellall` | 인벤토리 잡템 일괄 판매 미리보기 |
| `/sellall confirm` | 판매 확정 |
| `/sellall cancel` | 판매 취소 |
| `/bounty` | 현상금 보드 GUI 열기 |
| `/deepmine tp` | 딥 마인 입구로 순간이동 |
| `/deepmine status` | 딥 마인 리셋 타이머 상태 확인 |

---

## 6. 관리자 명령어 목록

| 명령어 | 설명 |
|--------|------|
| `/tide admin` | 관리자 GUI 열기 |
| `/tide reload` | 전체 핫 리로드 |
| `/tide reload config` | TideCore config.yml 재로드 |
| `/tide reload items` | 아이템 YAML 전체 재로드 |
| `/tide reload runes` | 룬 YAML 전체 재로드 |
| `/tide reload mobs` | 몹 YAML 전체 재로드 |
| `/tide reload affixes` | 접두사 YAML 전체 재로드 |
| `/deepmine reset` | 딥 마인 즉시 리셋 |

---

## 7. 설정 파일 구조 & 스키마

### TideCore/config.yml 주요 항목

```yaml
tide:
  cycle-duration-minutes: 120     # 밀물/썰물 한 사이클 길이
  spring-tide-chance: 5           # 사리 발생 확률 (%)
  blood-moon-chance: 8            # 블러드문 발생 확률 (%)
  scheduled-spring-day: SUNDAY    # 주 1회 예약 사리 요일

economy:
  clam-sink-reinforce-base: 100
  clam-sink-reinforce-multiplier: 1.5
  pearl-protection-scroll-cost: 3
  inflation-alert-threshold: 1000000

death_penalty:
  clam-loss-percent: 10
  grave-duration-seconds: 600
  debuff-duration-seconds: 60

drop_rate_multiplier:
  high_tide: 2.0
  low_tide_wild: 0.3
  low_tide_instance: 1.5
  spring_tide: 3.0

web-server:
  enabled: true
  port: 8080
```

### 아이템 YAML 스키마 (TideRPG/items/{id}.yml)

```yaml
id: iron_sword_t1
display_name: "§6[파도의 검]"
material: IRON_SWORD
custom_model_data: 1001
gear_score: 150
tier: 1
base_stats:
  damage: 18
  defense: 0
reinforce_bonus:
  damage_per_star: 3
  defense_per_star: 0
socket_count: 2
socket_max: 3
sell_price: 10
drop_sources:
  - mob: zombie_t1
    chance: 0.05
lore_template:
  - "§7등급: §fTier {tier}"
  - "§6전투력(GS): §f{gs}"
  - "§7강화: §a+{reinforce}"
  - "§7소켓: {socket_display}"
```

### 룬 YAML 스키마 (TideRPG/runes/{id}.yml)

```yaml
id: rune_lifesteal_2
display_name: "§c흡혈의 룬 II"
type: lifesteal
grade: 2
material: NETHER_BRICK
custom_model_data: 3002
effect:
  type: lifesteal
  value: 0.16
fusion:
  input_id: rune_lifesteal_1
  input_count: 3
  cost_clam: 200
  output_id: rune_lifesteal_2
```

### 몹 YAML 스키마 (TideMobs/mobs/{id}.yml)

```yaml
id: zombie_flame_t1
display_name: "§c[염화의] §f좀비"
base_mob: ZOMBIE
custom_model_data: 2001
stats:
  hp_multiplier: 2.5
  damage_multiplier: 1.8
  movement_speed: 0.28
affixes:
  - flame
spawn:
  worlds: [world]
  biomes: [PLAINS, FOREST, DESERT]
  tide_states: [LOW_TIDE, SPRING_TIDE, BLOOD_MOON]
  weight: 10
drops:
  - item_id: iron_sword_t1
    chance: 0.08
  - currency: clam
    amount: [20, 50]
  - currency: pearl
    chance: 0.05
    amount: 1
lore_extra:
  - "§8— 불꽃의 기운이 서려있다 —"
```

### 접두사 YAML 스키마 (TideMobs/affixes/{id}.yml)

```yaml
id: flame
display_name: "§c염화의"
hp_multiplier: 1.5
damage_multiplier: 1.3
spawn_particle: FLAME
```

### 보스 제단 YAML 스키마 (TideMobs/altars/{id}.yml)

```yaml
id: void_knight_altar
display_name: "공허의 기사"
summon_x: 0
summon_y: 64
summon_z: 0
required_item: soul_fragment
required_count: 5
reward_clam: 1000
reward_pearl: 5
reward_rep: 100
```

### 구역 YAML 스키마 (TideRPG/zones/{id}.yml)

```yaml
id: deep_mine
display_name: "딥 마인"
world: world
min_x: -100
min_y: -60
min_z: -100
max_x: 100
max_y: 0
max_z: 100
recommended_gs: 300
warn_gs: 150
```

---

## 8. 3-Jar 모듈 의존 구조

```
TideCore.jar  (독립 — 의존 없음)
  └─ EconomyAPI (ServicesManager 등록)
  └─ TideStateProvider (ServicesManager 등록)
  └─ ReloadManager (ServicesManager 등록)
  └─ 내장 웹서버 (포트 8080)

TideRPG.jar  (TideCore 필수)
  └─ EconomyAPI 조회
  └─ ReloadManager 조회 → items/runes/zones 등록
  └─ ItemFactory (ServicesManager 등록)
  └─ GearScoreCalculator (ServicesManager 등록)

TideMobs.jar  (TideCore 필수, TideRPG 선택)
  └─ EconomyAPI 조회
  └─ TideStateProvider 조회
  └─ ItemFactory 조회 (TideRPG에서)
  └─ ReloadManager 조회 → affixes/altars/mobs 등록
  └─ NemesisManager (ServicesManager 등록)
```

**로드 순서**: TideCore → TideRPG → TideMobs (plugin.yml `depend` 설정)

---

## 9. 데이터 저장 구조

```
/plugins/
  TideCore/
    config.yml
    data/
      economy.db     ← 플레이어 경제 데이터 (SQLite, YAML 폴백)
      players.yml    ← SQLite 초기화 실패 시 폴백
    web/             ← React 빌드 결과물 (정적 파일)

  TideRPG/
    config.yml
    items/           ← 아이템 YAML
    runes/           ← 룬 YAML
    zones/           ← 구역 YAML
    fishingholes/    ← 낚시 포인트 YAML

  TideMobs/
    mobs/            ← 커스텀 몹 YAML
    affixes/         ← 접두사 YAML
    altars/          ← 보스 제단 YAML
    data/
      nemesis.db     ← 네메시스 데이터 (SQLite, YAML 폴백)
      nemesis.yml    ← SQLite 초기화 실패 시 폴백
```

---

## 8. 변경 이력 (Changelog)

### v0.1.0 → v0.1.1 (2026-06-26)

---

#### 🔧 빌드 & 안정성

**[수정] sqlite-jdbc relocation 제거 — `UnsatisfiedLinkError` 해결**

| 항목 | 내용 |
|------|------|
| 수정 파일 | `tide-core/pom.xml`, `tide-mobs/pom.xml` |
| 원인 | `maven-shade-plugin`이 Java 클래스 경로는 재배치하지만 JAR 내부 네이티브 `.dll` 리소스 경로 문자열은 재배치하지 않음 → sqlite-jdbc JNI 네이티브 라이브러리 로딩 실패 |
| 증상 | TideCore 활성화 시 크래시 → TideRPG/TideMobs도 연쇄 `NoClassDefFoundError` |
| 수정 | pom.xml `<relocations>` 블록 완전 제거. sqlite-jdbc를 `org.sqlite` 원래 패키지 그대로 shading. |

**[수정] `EconomyManager` / `NemesisManager` — `catch (Exception)` → `catch (Throwable)`**

- `UnsatisfiedLinkError`는 `Error` 계열이라 `catch (Exception)` 블록에 잡히지 않아 YAML 폴백이 동작하지 않던 문제 수정.
- `catch (Throwable)`로 변경 시 JVM 레벨 Error 포함 모든 예외를 잡아 YAML 폴백으로 자동 전환.

---

#### 🌐 TideCore — 내장 웹 서버 (TideWebServer.java)

**[신규] `DatabaseHandler` — `/api/database/*` 엔드포인트 구현**

| 엔드포인트 | 기능 |
|------------|------|
| `GET /api/database/economy` | `EconomyManager.getAllPlayers()` 순회 → uuid, name, clam, pearl, rep, hardMode JSON 배열 반환 |
| `POST /api/database/economy` | Body `{uuid, clam?, pearl?, rep?, hardMode?}` → null 필드 건너뜀 → `updatePlayerEconomy()` 메인 스레드 예약 |
| `GET /api/database/nemesis` | ServicesManager + 리플렉션으로 TideMobs `NemesisManager.getAllRecords()` 호출 → 레코드 JSON 배열 반환 |

- **크로스 Jar 리플렉션 패턴**: TideCore가 TideMobs에 컴파일 의존성 없이 동작. TideMobs 미로드 시 빈 배열 반환 (에러 없음).

**[신규] `StaticHandler` — React 앱 내장 서빙 + SPA 라우팅**

- 정적 파일 서빙 우선순위: `plugins/TideCore/web/` 파일시스템 → JAR 내부 `/web/` 리소스 폴백.
- 경로에 해당하는 파일이 없으면 `index.html` 반환 → React Router 클라이언트 사이드 라우팅 지원.
- MIME 타입 자동 판별: `.html` / `.css` / `.js` / `.json` / `.png` / `.jpg` / `.svg` / `.ico`.
- 모든 응답에 CORS 헤더 포함. OPTIONS preflight 자동 204 처리.

**[개선] `ConfigsHandler` — POST 저장 후 카테고리별 자동 핫 리로드**

- YAML 저장 직후 카테고리-리로드 대상 매핑에 따라 `ReloadManager.reload()` 메인 스레드 예약:

| category | 자동 리로드 대상 |
|----------|----------------|
| item | items |
| rune | runes |
| mob / altar | mobs |
| affix | affixes |
| global/core | config |
| global/rpg | items |

**[신규] `EconomyManager` — 웹 API 연동 메서드 추가**

```java
// 웹 대시보드 전체 조회용
Map<UUID, PlayerEconomy> getAllPlayers()

// 웹 대시보드 수정용 (null 필드 = 변경 없음)
void updatePlayerEconomy(UUID uuid, Long clam, Long pearl, Integer rep, Boolean hardMode)

// Admin GUI 인플레이션 모니터용 (온라인 플레이어 캐시 기준)
long getOnlineClamTotal()
```

---

#### 🎮 TideRPG — 특수 아이템

**[신규] `tide_bell` — 조종의 종 (YAML + 리스너)**

```yaml
id: tide_bell
display_name: "§4§l[조종의 종]"
material: BELL
custom_model_data: 1008
tier: 3
sell_price: 500
lore_template:
  - "§7— 우클릭 시 블러드문을 강제 트리거합니다 —"
```

- **TideBellListener 동작**: `tide:item_id = tide_bell` PDC 보유 아이템 우클릭 시 `TideScheduler.forceState(TideState.BLOOD_MOON)` 호출 → `TideChangeEvent` 발행 → 전체 서버 조수 상태 즉시 블러드문 전환.
- GS 0 (전투 장비 아님), Tier 3 (희귀 특수 아이템), 판매가 500 조개.

---

#### 🧟 TideMobs — 커스텀 몹 시스템

**[개선] `CustomMobSpawnListener` — 스폰 로직 완전 구현**

이전: 스폰 조건 체크만 있는 스텁 수준  
이후: 완전 동작

- **스폰 조건 3중 필터**: 월드 이름 / 바이옴 / 현재 TideState 모두 일치 필요. YAML에서 빈 리스트이면 "모두 허용".
- **기본 변환 확률 15%**: 자연/스포너 스폰 중 15% 확률에서만 커스텀 몹 변환 시도.
- **가중치 기반 선택**: 후보 풀의 weight 합산 후 비례 확률로 1종 선택.
- **transform() 처리 내용**:
  - PDC `tide:custom_mob_id` 기록.
  - `custom_model_data > 0` 이면 PDC `tide:cmd` 기록 + 인간형 몹(Zombie/Skeleton)에 CMD 적용 헬멧 장착 → 리소스팩 모델 교체 지원.
  - `display_name` 설정 + 항상 보이도록 지정.
  - `hp_multiplier`, `damage_multiplier`, `movement_speed` Attribute 적용.
  - YAML `affixes` 지정 시 `EliteProcessor.apply()` 재사용 → 정예 태그 + 접두사 효과 동시 적용.
- **커스텀 드롭 처리**:
  - 바닐라 드롭 전체 제거 후 YAML `drops` 처리.
  - `item_id` → `ItemFactory.create()` → PDC 완비 아이템 드롭.
  - `currency: clam/pearl` + `amount: [min, max]` 또는 고정값 → `EconomyAPI`로 직접 지급 + 채팅 메시지.
  - `chance` 0.0~1.0 확률 적용.

**[신규] `MobKeys` — `tide:custom_mob_id` PDC 키 추가**

커스텀 몹 ID를 엔티티 PDC에 기록하는 키. 사망 이벤트에서 커스텀 몹 여부 식별에 사용.

---

#### 🖥 Web Customizer (tools/web-customizer)

**[신규] `DatabaseTab.jsx` — DB 대시보드 탭**

내장 웹 서버 API와 통신하는 React 컴포넌트.

**경제(Economy) 서브탭**
- `GET /api/database/economy` → 플레이어 목록 테이블.
- 컬럼: 이름, UUID, 조개(Clam), 진주(Pearl), 평판(Rep), 하드모드.
- 이름/UUID 실시간 검색 필터.
- [편집] 버튼 → 인라인 편집: clam/pearl/rep 숫자 입력 + hardMode 체크박스 → [저장] → `POST /api/database/economy`.

**네메시스(Nemesis) 서브탭**
- `GET /api/database/nemesis` → 활성 네메시스 레코드 목록.
- 컬럼: mobUuid, playerUuid, 원래 몹 이름, 접두사 목록, 처치 횟수, 활성 여부.
- 이름/UUID 실시간 검색 필터. 조회 전용.

**[개선] `api.js` — 신규 API 함수 추가**

```javascript
fetchEconomy()       // GET /api/database/economy
updateEconomy(body)  // POST /api/database/economy  
fetchNemesis()       // GET /api/database/nemesis
```

**[개선] `App.jsx`** — "🗄 데이터베이스" 탭 5번째로 등록.

**[개선] `style.css`** — DatabaseTab 테이블·인라인 편집·서브탭 스타일 추가.

**[개선] 생성기 탭 (MobTab / ItemTab / RuneTab)** — 배포 버튼 로직 통일, 에러 핸들링 개선.

---

#### 📦 JAR 내장 React 빌드 번들

`tide-core/src/main/resources/web/` 경로로 React 빌드 결과물이 Git에 포함됨:

```
web/
  index.html
  assets/
    index-CJObzpqz.js   ← React 앱 메인 번들
    index-DQQjlmxv.js   ← 추가 번들
    index-DRauM0HC.css  ← 스타일시트
```

- TideCore.jar Maven 빌드 시 해당 파일이 JAR 안 `/web/` 경로로 패키징됨.
- `StaticHandler`가 `getResourceAsStream("/web/...")` 으로 로드.
- **별도 파일시스템 배포 없이 TideCore.jar 하나만으로 웹 대시보드 완전 서빙 가능.**
