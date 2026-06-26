"""Pydantic request/response models for the Web Customizer API."""

from typing import Optional

from pydantic import BaseModel


class GenerateRequest(BaseModel):
    request_text: Optional[str] = None
    form_data: Optional[dict] = None
    model: Optional[str] = None
    api_key: Optional[str] = None
    provider: Optional[str] = None


class GenerateResponse(BaseModel):
    yaml: str
    id: str
    file_type: str


class DeployRequest(BaseModel):
    file_content: str
    file_type: str  # "item" | "rune" | "mob" | "affix" | "altar"
    file_id: str


class DeployResponse(BaseModel):
    path: str
    reload_target: str
    rcon_sent: bool
