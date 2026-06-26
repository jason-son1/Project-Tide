const API_BASE = import.meta.env.VITE_API_BASE || 
  (window.location.port === "5173" ? "http://localhost:8080" : window.location.origin);

async function postJson(path, body) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.detail || data.error || "요청이 실패했습니다.");
  }
  return data;
}

export function generateContent(contentType, payload) {
  return postJson(`/api/generate/${contentType}`, payload);
}

export function deployContent(payload) {
  return postJson("/api/deploy", payload);
}

// Config Synchronization endpoints
export async function fetchConfigs() {
  const response = await fetch(`${API_BASE}/api/configs`);
  if (!response.ok) throw new Error("설정 목록을 가져오지 못했습니다.");
  return response.json();
}

export async function fetchConfig(category, id) {
  const response = await fetch(`${API_BASE}/api/configs/${category}/${encodeURIComponent(id)}`);
  if (!response.ok) throw new Error(`설정을 가져오지 못했습니다: ${category}/${id}`);
  return response.json();
}

export async function saveConfig(category, id, yamlContent) {
  const response = await fetch(`${API_BASE}/api/configs/${category}/${encodeURIComponent(id)}`, {
    method: "POST",
    headers: { "Content-Type": "text/plain; charset=utf-8" },
    body: yamlContent
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "설정 저장에 실패했습니다.");
  }
  return data;
}

export async function fetchLogs() {
  const response = await fetch(`${API_BASE}/api/logs`);
  if (!response.ok) throw new Error("로그를 가져오지 못했습니다.");
  return response.json();
}
