---
name: tide-dev
description: 'The Tide v2 개발을 위한 전용 스킬. PDC 규칙을 강제하고 GS 기반 스탯 밸런싱을 수행합니다.'
---

# The Tide v2 Development Guide

당신은 이제 'The Tide v2' 서버의 코어 개발자 스킬을 활성화했습니다. 코드 생성 및 수정 시 아래 규칙을 절대적으로 준수하세요.

## 0. Build & Test Commands
- 빌드 커맨드: `mvn clean package` (또는 사용하시는 빌드 툴 명령어)
- 파일 검증 커맨드: `python .claude/helpers/validate_schema.py`

## 1. PDC-Strict Java Code Generator (Skill 1)
- **Lore 파싱 금지**: 아이템의 스탯이나 강함을 읽을 때 `item.getItemMeta().getLore()`를 문자열로 파싱하는 코드는 절대 금지합니다.
- **PDC 사용 필수**: 모든 RPG 데이터(gs, reinforce, socket_count 등)는 `PersistentDataContainer`에 바이너리로 저장 및 조회해야 합니다.
- **이벤트 중심(Event-Driven)**: 매 틱마다 엔티티나 플레이어를 스캔하는 `runTaskTimer` 루프 대신, `CreatureSpawnEvent`, `EntityDamageByEntityEvent` 등의 이벤트 리스너 구조를 사용하세요.

## 2. GS-Balanced Stat Scaler (Skill 5)
- 아이템 티어(Tier)에 따른 Gear Score(GS) 매핑 테이블을 엄격히 준수하세요.
  - T1: 100~199 (야생 기본)
  - T2: 200~399 (딥 마인 초입)
  - T3: 400~699 (딥 마인 심층)
- 몹 스탯 생성 시 HP/Damage Multiplier 가이드라인:
  - 일반 정예: 기본 스탯의 1.5배~2.5배
  - 네메시스 각성: 기본 스탯의 3.0배 이상 + 포션 이펙트(Strength II) 부여