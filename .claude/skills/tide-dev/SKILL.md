---
name: tide-dev
description: 'The Tide v2 개발을 위한 전용 스킬. PDC 규칙을 강제하고 GS 기반 스탯 밸런싱을 수행합니다.'
commands:
  - /tide-code
  - /tide-balance
---

# The Tide v2 Development Guide

당신은 이제 'The Tide v2' 서버의 코어 개발자 스킬을 활성화했습니다. 코드 생성 및 수정 시 아래 규칙을 절대적으로 준수하세요.

## 1. PDC-Strict Java Code Generator (Skill 1)
- **Lore 파싱 금지**: 아이템의 스탯이나 강함을 읽을 때 `item.getItemMeta().getLore()`를 문자열로 파싱하는 코드는 절대 금지합니다.
- **PDC 사용 필수**: 모든 RPG 데이터(gs, reinforce, socket_count 등)는 `PersistentDataContainer`에 바이너리로 저장 및 조회해야 합니다.
- **이벤트 중심(Event-Driven)**: 매 틱마다 엔티티나 플레이어를 스캔하는 `runTaskTimer` 루프 대신, `CreatureSpawnEvent`, `EntityDamageByEntityEvent` 등의 이벤트 리스너 구조를 사용하세요.

## 2. GS-Balanced Stat Scaler (Skill 5)
- 아이템 티어(Tier)에 따른 Gear Score(GS) 매핑 테이블을 엄격히 준수하세요.
  - T1: 100~199 (야생 기본)
  - T2: 200~399 (딥 마인 초입)
  - T3: 400~699 (딥 마인 심층)
  - T4: 700~999 (심연)
  - T5: 1000+ (종결 장비)
- 몹 스탯 생성 시 HP/Damage Multiplier 가이드라인:
  - 일반 정예: 기본 스탯의 1.5배~2.5배
  - 네메시스 각성: 기본 스탯의 3.0배 이상 + 포션 이펙트(Strength II) 부여

## 3. 워크플로우 핫키 및 자동화 프로세스

사용자가 콘텐츠 생성을 요청하거나 버그 수정을 요청하면 반드시 다음 순서(파이프라인)대로 작업을 완수하세요.

1. **설정 생성/수정 후**: `.claude/helpers/validate_schema.py`를 실행하여 YAML 스키마 유효성을 검사하세요. (Skill 2)
2. **검증 통과 시**: 파일을 해당 플러그인 디렉토리로 이동시키고, `.claude/helpers/rcon_reload.py`를 실행하여 서버에 즉시 핫 리로드를 명령하세요. (Skill 3)
3. **리로딩 직후**: `.claude/helpers/check_logs.py`를 실행하여 콘솔에 파싱 에러나 `NullPointerException`이 올라왔는지 확인하고, 만약 에러가 발견되면 즉시 자가 치유(Self-Healing) 수정을 시도하세요. (Skill 4)
