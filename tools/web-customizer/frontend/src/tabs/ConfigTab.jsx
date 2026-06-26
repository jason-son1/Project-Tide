import React, { useState, useEffect } from "react";
import { fetchConfigs, fetchConfig, saveConfig, fetchLogs } from "../api.js";

const CATEGORY_NAMES = {
  global: "전역 설정",
  item: "아이템 목록",
  rune: "룬 목록",
  mob: "정예 몹 목록",
  affix: "접두사 목록",
  altar: "보스 제단 목록",
};

export default function ConfigTab({ onEditVisually }) {
  const [configs, setConfigs] = useState([]);
  const [selectedConfig, setSelectedConfig] = useState(null);
  const [yamlText, setYamlText] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [statusMsg, setStatusMsg] = useState("");
  const [logs, setLogs] = useState([]);
  const [showLogs, setShowLogs] = useState(false);
  const [isPollingLogs, setIsPollingLogs] = useState(false);

  useEffect(() => {
    loadConfigs();
  }, []);

  useEffect(() => {
    let interval;
    if (showLogs) {
      loadLogs();
      interval = setInterval(loadLogs, 3000);
      setIsPollingLogs(true);
    } else {
      setIsPollingLogs(false);
    }
    return () => clearInterval(interval);
  }, [showLogs]);

  async function loadConfigs() {
    try {
      const data = await fetchConfigs();
      setConfigs(data);
    } catch (err) {
      setStatusMsg(`목록 로드 실패: ${err.message}`);
    }
  }

  async function loadLogs() {
    try {
      const data = await fetchLogs();
      setLogs(data.logs || []);
    } catch (err) {
      console.error("로그 로드 실패:", err);
    }
  }

  async function handleSelect(cfg) {
    setIsLoading(true);
    setStatusMsg("");
    try {
      const data = await fetchConfig(cfg.category, cfg.id);
      setSelectedConfig(cfg);
      setYamlText(data.yaml);
    } catch (err) {
      setStatusMsg(`오류: ${err.message}`);
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSave() {
    if (!selectedConfig) return;
    setStatusMsg("저장 중...");
    try {
      const result = await saveConfig(selectedConfig.category, selectedConfig.id, yamlText);
      setStatusMsg(`성공: ${result.message} (reload target: ${result.reload_target})`);
      loadConfigs(); // refresh list in case it's a new file
    } catch (err) {
      setStatusMsg(`저장 실패: ${err.message}`);
    }
  }

  function handleLoadToGenerator() {
    if (!selectedConfig || !onEditVisually) return;
    try {
      // simple yaml parser or let the visual tabs deal with parsing
      onEditVisually(selectedConfig.category, yamlText, selectedConfig.id);
    } catch (err) {
      setStatusMsg(`파싱 실패: ${err.message}`);
    }
  }

  // Group configurations by category
  const grouped = configs.reduce((acc, curr) => {
    if (!acc[curr.category]) acc[curr.category] = [];
    acc[curr.category].push(curr);
    return acc;
  }, {});

  return (
    <div className="tab-grid" style={{ gridTemplateColumns: "280px 1fr" }}>
      {/* Left Column: Sidebar Tree */}
      <div className="form-panel" style={{ maxHeight: "calc(100vh - 160px)", overflowY: "auto" }}>
        <h3>서버 동기화 리스트</h3>
        <button className="secondary-btn" onClick={loadConfigs} style={{ marginBottom: "12px", padding: "6px" }}>
          🔄 새로고침
        </button>

        {Object.entries(CATEGORY_NAMES).map(([catKey, catLabel]) => {
          const items = grouped[catKey] || [];
          if (items.length === 0 && catKey !== "global") return null;
          return (
            <div key={catKey} style={{ marginBottom: "16px" }}>
              <h4 style={{ margin: "4px 0", color: "#38bdf8", borderBottom: "1px solid #334155", paddingBottom: "2px" }}>
                {catLabel}
              </h4>
              <div style={{ display: "flex", flexDirection: "column", gap: "2px", paddingLeft: "4px" }}>
                {items.length === 0 ? (
                  <span style={{ fontSize: "11px", color: "#64748b" }}>설정이 없습니다.</span>
                ) : (
                  items.map((cfg) => {
                    const isSelected = selectedConfig && selectedConfig.id === cfg.id && selectedConfig.category === cfg.category;
                    return (
                      <button
                        key={`${cfg.category}-${cfg.id}`}
                        onClick={() => handleSelect(cfg)}
                        className={`sidebar-item-btn ${isSelected ? "selected" : ""}`}
                        style={{
                          textAlign: "left",
                          background: isSelected ? "#0ea5e9" : "transparent",
                          color: isSelected ? "white" : "#cbd5e1",
                          border: "none",
                          padding: "6px 8px",
                          borderRadius: "4px",
                          cursor: "pointer",
                          fontSize: "12px",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                        }}
                      >
                        📄 {cfg.id}
                      </button>
                    );
                  })
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Right Column: Code Editor & Logs */}
      <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
        <div className="preview-panel" style={{ flex: 1, position: "static" }}>
          {selectedConfig ? (
            <>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                <div>
                  <h3 style={{ margin: 0 }}>{selectedConfig.id} ({CATEGORY_NAMES[selectedConfig.category]})</h3>
                  <span style={{ fontSize: "11px", color: "#94a3b8" }}>경로: {selectedConfig.path}</span>
                </div>
                <div style={{ display: "flex", gap: "8px" }}>
                  {["item", "mob", "rune"].includes(selectedConfig.category) && (
                    <button onClick={handleLoadToGenerator} className="secondary-btn" style={{ padding: "6px 12px" }}>
                      🛠️ 비주얼 생성기로 가져오기
                    </button>
                  )}
                  <button onClick={handleSave} className="primary" style={{ padding: "6px 16px" }}>
                    💾 저장 및 핫 리로드
                  </button>
                </div>
              </div>

              {isLoading ? (
                <div style={{ padding: "40px", textAlign: "center", color: "#94a3b8" }}>로딩 중...</div>
              ) : (
                <textarea
                  value={yamlText}
                  onChange={(e) => setYamlText(e.target.value)}
                  style={{
                    width: "100%",
                    height: "450px",
                    fontFamily: "Courier New, monospace",
                    fontSize: "13px",
                    lineHeight: "1.5",
                    background: "#0f172a",
                    color: "#38bdf8",
                    border: "1px solid #334155",
                    borderRadius: "6px",
                    padding: "12px",
                    resize: "vertical",
                  }}
                />
              )}

              {statusMsg && (
                <div
                  className="status"
                  style={{
                    marginTop: "8px",
                    padding: "8px",
                    borderRadius: "4px",
                    background: statusMsg.startsWith("오류") || statusMsg.startsWith("저장 실패") ? "#991b1b" : "#065f46",
                    color: "white",
                  }}
                >
                  {statusMsg}
                </div>
              )}
            </>
          ) : (
            <div style={{ padding: "80px 20px", textAlign: "center", color: "#64748b" }}>
              <h2 style={{ color: "#94a3b8" }}>🌊 Tide Config Sync Dashboard</h2>
              <p>왼쪽 목록에서 설정 파일을 선택하면 내용을 불러오고, 즉석에서 수정 및 서버 반영이 가능합니다.</p>
            </div>
          )}
        </div>

        {/* Live Server Logs Console */}
        <div className="preview-panel" style={{ position: "static" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
            <h4 style={{ margin: 0, display: "flex", alignItems: "center", gap: "8px" }}>
              🖥️ 실시간 서버 로그 모니터링
              {isPollingLogs && <span className="pulse-dot"></span>}
            </h4>
            <button onClick={() => setShowLogs(!showLogs)} className="secondary-btn" style={{ padding: "4px 10px", fontSize: "12px" }}>
              {showLogs ? "접기" : "로그 창 열기 (3초 자동 동기화)"}
            </button>
          </div>

          {showLogs && (
            <div
              style={{
                background: "#020617",
                color: "#10b981",
                fontFamily: "monospace",
                padding: "10px",
                borderRadius: "6px",
                height: "220px",
                overflowY: "auto",
                fontSize: "11px",
                whiteSpace: "pre-wrap",
                border: "1px solid #1e293b",
              }}
            >
              {logs.length === 0
                ? "로그를 가져오는 중이거나 파일이 비어있습니다..."
                : logs.map((line, idx) => <div key={idx}>{line}</div>)}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
