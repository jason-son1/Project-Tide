"""FastAPI backend for the Web Customizer (3파트 — Automated Generation System).

Two ways to produce a YAML file:
  - form_data: built locally, no AI call, instant.
  - request_text: routed through Claude using the same prompts the CLI
    tool (../ai-generator) uses, so both paths emit byte-identical schemas.

Deploying writes the file straight into a plugin's data directory and
(optionally) fires the matching `/tide reload` over RCON.
"""

import os
import sys
from pathlib import Path

import yaml
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from schemas import DeployRequest, DeployResponse, GenerateRequest, GenerateResponse

ROOT = Path(__file__).resolve().parents[1]
AI_GENERATOR_DIR = ROOT / "ai-generator"
sys.path.insert(0, str(AI_GENERATOR_DIR))

DEPLOY_ROOT = Path(os.environ.get("TIDE_DEPLOY_ROOT", ROOT / "deployed"))

PLUGIN_SUBDIR = {
    "item": ("TideRPG", "items"),
    "rune": ("TideRPG", "runes"),
    "mob": ("TideMobs", "mobs"),
    "affix": ("TideMobs", "affixes"),
    "altar": ("TideMobs", "altars"),
}

RELOAD_TARGET = {
    "item": "items",
    "rune": "runes",
    "mob": "mobs",
    "affix": "affixes",
    "altar": "altars",
}

FORM_DEFAULTS = {
    "item": {
        "material": "STONE",
        "custom_model_data": 1000,
        "gear_score": 100,
        "tier": 1,
        "base_stats": {"damage": 0, "defense": 0},
        "reinforce_bonus": {"damage_per_star": 0, "defense_per_star": 0},
        "socket_count": 0,
        "socket_max": 0,
        "sell_price": 0,
        "lore_template": [
            "§7등급: §fTier {tier}",
            "§6전투력(GS): §f{gs}",
            "§7강화: §a+{reinforce}",
            "§7소켓: {socket_display}",
        ],
    },
    "rune": {
        "type": "lifesteal",
        "grade": 1,
        "material": "AMETHYST_SHARD",
        "custom_model_data": 3000,
        "effect": {"type": "lifesteal", "value": 0.08},
    },
    "mob": {
        "base_mob": "ZOMBIE",
        "custom_model_data": 2000,
        "stats": {"hp_multiplier": 1.5, "damage_multiplier": 1.5, "movement_speed": 0.23},
        "affixes": [],
        "spawn": {"worlds": ["world"], "biomes": ["PLAINS"], "tide_states": ["LOW_TIDE"], "weight": 10},
        "drops": [],
    },
}

app = FastAPI(title="The Tide v2 - Web Customizer API")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://127.0.0.1:5173"],
    allow_methods=["*"],
    allow_headers=["*"],
)


def _build_from_form(content_type: str, form_data: dict) -> dict:
    merged = {**FORM_DEFAULTS[content_type], **form_data}
    if "id" not in merged or not merged["id"]:
        raise HTTPException(400, "form_data.id 가 필요합니다.")
    return merged


def _generate_with_ai(content_type: str, request_text: str, model: str | None) -> dict:
    try:
        from generate_content import DEFAULT_MODEL  # noqa: F401  (import-time validation only)
        import anthropic
    except ImportError as exc:
        raise HTTPException(500, f"AI 생성기 의존성이 설치되지 않았습니다: {exc}") from exc

    system_prompt = (AI_GENERATOR_DIR / "prompts" / f"system_{content_type}.txt").read_text(encoding="utf-8")
    client = anthropic.Anthropic()
    message = client.messages.create(
        model=model or os.environ.get("ANTHROPIC_MODEL", "claude-sonnet-4-6"),
        max_tokens=2000,
        system=system_prompt,
        messages=[{"role": "user", "content": request_text}],
    )
    yaml_text = "".join(block.text for block in message.content if hasattr(block, "text"))
    yaml_text = yaml_text.strip()
    if yaml_text.startswith("```"):
        lines = yaml_text.splitlines()[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        yaml_text = "\n".join(lines)

    data = yaml.safe_load(yaml_text)
    if not isinstance(data, dict) or "id" not in data:
        raise HTTPException(502, "AI 응답이 유효한 YAML 스키마가 아닙니다.")
    return data


@app.post("/api/generate/{content_type}", response_model=GenerateResponse)
def generate(content_type: str, body: GenerateRequest):
    if content_type not in PLUGIN_SUBDIR:
        raise HTTPException(404, f"알 수 없는 컨텐츠 타입: {content_type}")

    if body.form_data:
        data = _build_from_form(content_type, body.form_data)
    elif body.request_text:
        data = _generate_with_ai(content_type, body.request_text, body.model)
    else:
        raise HTTPException(400, "request_text 또는 form_data 중 하나가 필요합니다.")

    yaml_text = yaml.safe_dump(data, allow_unicode=True, sort_keys=False)
    return GenerateResponse(yaml=yaml_text, id=data["id"], file_type=content_type)


@app.post("/api/deploy", response_model=DeployResponse)
def deploy(body: DeployRequest):
    if body.file_type not in PLUGIN_SUBDIR:
        raise HTTPException(404, f"알 수 없는 파일 타입: {body.file_type}")

    try:
        parsed = yaml.safe_load(body.file_content)
        if not isinstance(parsed, dict):
            raise ValueError("YAML 최상위 구조가 매핑이 아닙니다.")
    except Exception as exc:
        raise HTTPException(400, f"유효하지 않은 YAML: {exc}") from exc

    plugin_name, subdir = PLUGIN_SUBDIR[body.file_type]
    target_dir = DEPLOY_ROOT / plugin_name / subdir
    target_dir.mkdir(parents=True, exist_ok=True)
    target_path = target_dir / f"{body.file_id}.yml"
    target_path.write_text(body.file_content, encoding="utf-8")

    rcon_sent = _try_rcon_reload(RELOAD_TARGET[body.file_type])
    return DeployResponse(path=str(target_path), reload_target=RELOAD_TARGET[body.file_type], rcon_sent=rcon_sent)


@app.get("/api/registry/{content_type}")
def registry(content_type: str):
    if content_type not in PLUGIN_SUBDIR:
        raise HTTPException(404, f"알 수 없는 컨텐츠 타입: {content_type}")
    plugin_name, subdir = PLUGIN_SUBDIR[content_type]
    target_dir = DEPLOY_ROOT / plugin_name / subdir
    if not target_dir.exists():
        return {"ids": []}
    return {"ids": sorted(p.stem for p in target_dir.glob("*.yml"))}


def _try_rcon_reload(target: str) -> bool:
    host = os.environ.get("RCON_HOST")
    password = os.environ.get("RCON_PASSWORD")
    if not host or not password:
        return False
    try:
        from mcrcon import MCRcon
    except ImportError:
        return False
    port = int(os.environ.get("RCON_PORT", "25575"))
    try:
        with MCRcon(host, password, port=port) as rcon:
            rcon.command(f"/tide reload {target}")
        return True
    except Exception:
        return False
