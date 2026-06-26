#!/usr/bin/env python3
import sys
import os
import yaml
from pathlib import Path

# Schema definitions (required keys and expected types)
SCHEMAS = {
    "item": {
        "required": ["id", "material", "gear_score", "tier", "base_stats"],
        "types": {
            "id": str,
            "material": str,
            "gear_score": int,
            "tier": int,
            "base_stats": dict
        }
    },
    "rune": {
        "required": ["id", "type", "grade", "material", "effect"],
        "types": {
            "id": str,
            "type": str,
            "grade": int,
            "material": str,
            "effect": dict
        }
    },
    "mob": {
        "required": ["id", "base_mob", "stats", "affixes", "spawn", "drops"],
        "types": {
            "id": str,
            "base_mob": str,
            "stats": dict,
            "affixes": list,
            "spawn": dict,
            "drops": list
        }
    },
    "affix": {
        "required": ["id", "display_name", "hp_multiplier", "damage_multiplier"],
        "types": {
            "id": str,
            "display_name": str,
            "hp_multiplier": (int, float),
            "damage_multiplier": (int, float)
        }
    }
}

def guess_type_by_path(filepath: Path) -> str:
    path_str = str(filepath).replace("\\", "/")
    if "/items/" in path_str:
        return "item"
    elif "/runes/" in path_str:
        return "rune"
    elif "/mobs/" in path_str:
        return "mob"
    elif "/affixes/" in path_str:
        return "affix"
    
    # Try guess by content if path is ambiguous
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f)
            if not isinstance(data, dict):
                return "unknown"
            if "gear_score" in data:
                return "item"
            if "grade" in data and "effect" in data:
                return "rune"
            if "base_mob" in data:
                return "mob"
            if "display_name" in data and "hp_multiplier" in data:
                return "affix"
    except Exception:
        pass
    
    return "unknown"

def validate_file(filepath: Path) -> bool:
    print(f"[*] 검사 중: {filepath}")
    
    if not filepath.exists():
        print(f"[ERROR] 파일이 존재하지 않습니다: {filepath}")
        return False
        
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f)
    except yaml.YAMLError as e:
        print(f"[ERROR] YAML 문법 오류가 있습니다:")
        print(f"  {e}")
        return False
    except Exception as e:
        print(f"[ERROR] 파일을 읽는 중 예외 발생: {e}")
        return False

    if data is None:
        print("[ERROR] 파일이 비어 있습니다.")
        return False

    if not isinstance(data, dict):
        print(f"[ERROR] YAML 최상위 구조가 매핑(dict/object)이 아닙니다. 받은 타입: {type(data).__name__}")
        return False

    cfg_type = guess_type_by_path(filepath)
    if cfg_type == "unknown":
        # Global config or unknown, just verify it parses
        print("[INFO] 카테고리를 추정할 수 없어 기본 YAML 구문 검사만 진행했습니다.")
        return True

    schema = SCHEMAS[cfg_type]
    
    # Check required fields
    for field in schema["required"]:
        if field not in data:
            print(f"[ERROR] [{cfg_type.upper()} 스키마 위반] 필수 필드가 누락되었습니다: '{field}'")
            return False
            
    # Check types
    for field, expected_type in schema["types"].items():
        if field in data:
            val = data[field]
            if not isinstance(val, expected_type):
                # special check for tuple (multiple allowed types like int/float)
                if isinstance(expected_type, tuple):
                    if type(val) not in expected_type:
                        print(f"[ERROR] [{cfg_type.upper()} 스키마 위반] 필드 '{field}'의 타입이 잘못되었습니다. 예상: {expected_type}, 실제: {type(val).__name__}")
                        return False
                else:
                    print(f"[ERROR] [{cfg_type.upper()} 스키마 위반] 필드 '{field}'의 타입이 잘못되었습니다. 예상: {expected_type.__name__}, 실제: {type(val).__name__}")
                    return False
                    
    # Custom semantic validations
    if cfg_type == "item":
        gs = data.get("gear_score", 0)
        tier = data.get("tier", 1)
        # Verify GS corresponds to tier
        # T1: 100~199, T2: 200~399, T3: 400~699, T4: 700~999, T5: 1000+
        if tier == 1 and not (100 <= gs <= 199):
            print(f"[WARNING] [밸런스 경고] Tier 1 아이템의 전투력(GS)이 권장 범위(100~199)를 벗어납니다. 현재: {gs}")
        elif tier == 2 and not (200 <= gs <= 399):
            print(f"[WARNING] [밸런스 경고] Tier 2 아이템의 전투력(GS)이 권장 범위(200~399)를 벗어납니다. 현재: {gs}")
        elif tier == 3 and not (400 <= gs <= 699):
            print(f"[WARNING] [밸런스 경고] Tier 3 아이템의 전투력(GS)이 권장 범위(400~699)를 벗어납니다. 현재: {gs}")

    elif cfg_type == "mob":
        affixes = data.get("affixes", [])
        allowed_affixes = {"염화의", "신속의", "폭심의", "분열의", "강철의", "재생의", "가시의", "보호막의"}
        for affix in affixes:
            if affix not in allowed_affixes:
                print(f"[ERROR] [Mobs 스키마 위반] 알 수 없는 접두사(Affix)가 지정되었습니다: '{affix}'. 허용되는 목록: {list(allowed_affixes)}")
                return False

    print(f"[+] 검증 통과! 스키마 타입: {cfg_type.upper()}")
    return True

def main():
    if len(sys.argv) < 2:
        print("사용법: python validate_schema.py <검사할_파일_경로>")
        sys.exit(1)
        
    filepath = Path(sys.argv[1])
    success = validate_file(filepath)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
