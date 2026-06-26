#!/usr/bin/env python3
"""AI content generator for The Tide v2 (3파트 — Automated Generation System).

Turns a natural-language request into a YAML file matching the standard
mob/item/rune schema (see ../../Goal/3대 시스템 아키텍처 실현 계획.pandoc.md
section 3-1), ready to drop into the matching plugin's data directory and
pick up with `/tide reload`.

Usage:
    pip install -r requirements.txt
    export ANTHROPIC_API_KEY=sk-ant-...
    python generate_content.py mob "피격 시 분열하는 레벨 20 스켈레톤 궁수, 화살 많이 드롭"
    python generate_content.py item "공격력이 높은 T2 네더라이트 대검"
    python generate_content.py rune "피해의 16%를 흡혈하는 2등급 룬"

Deploying the result to a running server:
    cp output/mobs/<id>.yml  <server>/plugins/TideMobs/affixes-or-mobs/
    cp output/items/<id>.yml <server>/plugins/TideRPG/items/
    cp output/runes/<id>.yml <server>/plugins/TideRPG/runes/
    /tide reload <items|runes|affixes>
"""

import argparse
import os
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).parent
PROMPTS_DIR = ROOT / "prompts"
OUTPUT_DIR = ROOT / "output"

REQUIRED_KEYS = {
    "mob": ["id", "base_mob", "stats", "affixes", "drops", "spawn"],
    "item": ["id", "material", "gear_score", "tier", "base_stats"],
    "rune": ["id", "type", "grade", "effect"],
}

OUTPUT_SUBDIR = {
    "mob": "mobs",
    "item": "items",
    "rune": "runes",
}

DEFAULT_MODEL = os.environ.get("ANTHROPIC_MODEL", "claude-sonnet-4-6")


def generate(content_type: str, user_request: str, model: str = DEFAULT_MODEL) -> str | None:
    import anthropic  # lazy import so --help works without the dependency installed

    system_prompt_path = PROMPTS_DIR / f"system_{content_type}.txt"
    system_prompt = system_prompt_path.read_text(encoding="utf-8")

    client = anthropic.Anthropic()
    message = client.messages.create(
        model=model,
        max_tokens=2000,
        system=system_prompt,
        messages=[{"role": "user", "content": user_request}],
    )
    yaml_text = "".join(block.text for block in message.content if hasattr(block, "text"))
    yaml_text = _strip_code_fence(yaml_text)

    try:
        data = yaml.safe_load(yaml_text)
        if not isinstance(data, dict):
            raise ValueError("최상위 YAML 구조가 매핑(dict)이 아닙니다.")
        for key in REQUIRED_KEYS[content_type]:
            if key not in data:
                raise ValueError(f"필수 필드 누락: {key}")
    except Exception as exc:  # noqa: BLE001 - surface any parse/validation failure to the operator
        print(f"[오류] YAML 유효성 검사 실패: {exc}", file=sys.stderr)
        print("----- 원본 응답 -----", file=sys.stderr)
        print(yaml_text, file=sys.stderr)
        return None

    output_dir = OUTPUT_DIR / OUTPUT_SUBDIR[content_type]
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"{data['id']}.yml"
    output_path.write_text(yaml_text, encoding="utf-8")

    print(f"[완료] 파일 생성: {output_path}")
    return str(output_path)


def _strip_code_fence(text: str) -> str:
    text = text.strip()
    if text.startswith("```"):
        lines = text.splitlines()
        lines = lines[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        text = "\n".join(lines)
    return text.strip() + "\n"


def main() -> None:
    parser = argparse.ArgumentParser(description="The Tide v2 AI content generator")
    parser.add_argument("type", choices=["mob", "item", "rune"], help="생성할 컨텐츠 종류")
    parser.add_argument("request", nargs="+", help="자연어 요청 (공백으로 구분된 여러 단어 가능)")
    parser.add_argument("--model", default=DEFAULT_MODEL, help=f"사용할 Claude 모델 (기본값: {DEFAULT_MODEL})")
    args = parser.parse_args()

    if "ANTHROPIC_API_KEY" not in os.environ:
        print("[오류] ANTHROPIC_API_KEY 환경변수가 설정되지 않았습니다.", file=sys.stderr)
        sys.exit(1)

    result = generate(args.type, " ".join(args.request), model=args.model)
    sys.exit(0 if result else 1)


if __name__ == "__main__":
    main()
