import React, { useState, useEffect } from "react";
import { fetchEconomy, updateEconomy, fetchNemesis } from "../api.js";

export default function DatabaseTab() {
  const [subTab, setSubTab] = useState("economy"); // "economy" or "nemesis"
  const [players, setPlayers] = useState([]);
  const [nemesis, setNemesis] = useState([]);
  const [search, setSearch] = useState("");
  const [statusMsg, setStatusMsg] = useState("");
  const [isSaving, setIsSaving] = useState(false);

  // Edit State
  const [editingPlayer, setEditingPlayer] = useState(null);
  const [editClam, setEditClam] = useState(0);
  const [editPearl, setEditPearl] = useState(0);
  const [editRep, setEditRep] = useState(0);
  const [editHardMode, setEditHardMode] = useState(false);

  useEffect(() => {
    loadData();
  }, [subTab]);

  async function loadData() {
    setStatusMsg("");
    try {
      if (subTab === "economy") {
        const data = await fetchEconomy();
        setPlayers(data);
      } else {
        const data = await fetchNemesis();
        setNemesis(data);
      }
    } catch (err) {
      setStatusMsg(`오류: ${err.message}`);
    }
  }

  function handleEditClick(player) {
    setEditingPlayer(player);
    setEditClam(player.clam);
    setEditPearl(player.pearl);
    setEditRep(player.rep);
    setEditHardMode(player.hardMode);
  }

  async function handleSaveEdit(e) {
    e.preventDefault();
    if (!editingPlayer) return;
    setIsSaving(true);
    setStatusMsg("저장 중...");
    try {
      await updateEconomy({
        uuid: editingPlayer.uuid,
        clam: parseInt(editClam, 10),
        pearl: parseInt(editPearl, 10),
        rep: parseInt(editRep, 10),
        hardMode: editHardMode
      });
      setStatusMsg("성공적으로 저장되었습니다.");
      setEditingPlayer(null);
      loadData();
    } catch (err) {
      setStatusMsg(`저장 실패: ${err.message}`);
    } finally {
      setIsSaving(false);
    }
  }

  const filteredPlayers = players.filter(p =>
    p.name.toLowerCase().includes(search.toLowerCase()) ||
    p.uuid.toLowerCase().includes(search.toLowerCase())
  );

  const filteredNemesis = nemesis.filter(n =>
    n.originalName.toLowerCase().includes(search.toLowerCase()) ||
    n.playerUuid.toLowerCase().includes(search.toLowerCase()) ||
    n.mobUuid.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="database-tab">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
        <div style={{ display: "flex", gap: "8px" }}>
          <button
            className={`tab ${subTab === "economy" ? "active" : ""}`}
            onClick={() => { setSubTab("economy"); setEditingPlayer(null); }}
            style={{ padding: "8px 16px", borderRadius: "6px", cursor: "pointer" }}
          >
            🪙 플레이어 경제 & 명성
          </button>
          <button
            className={`tab ${subTab === "nemesis" ? "active" : ""}`}
            onClick={() => { setSubTab("nemesis"); setEditingPlayer(null); }}
            style={{ padding: "8px 16px", borderRadius: "6px", cursor: "pointer" }}
          >
            👁️ 네메시스 추적
          </button>
        </div>
        <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
          <input
            type="text"
            placeholder="검색어 입력..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              padding: "6px 12px",
              background: "#1e293b",
              color: "white",
              border: "1px solid #475569",
              borderRadius: "6px",
              width: "200px"
            }}
          />
          <button
            onClick={loadData}
            className="secondary-btn"
            style={{ padding: "6px 12px" }}
          >
            🔄 새로고침
          </button>
        </div>
      </div>

      {statusMsg && (
        <div
          style={{
            padding: "8px 12px",
            borderRadius: "6px",
            marginBottom: "12px",
            background: statusMsg.startsWith("오류") || statusMsg.startsWith("저장 실패") ? "#ef4444" : "#10b981",
            color: "white",
            fontSize: "13px"
          }}
        >
          {statusMsg}
        </div>
      )}

      <div className="tab-grid" style={{ gridTemplateColumns: editingPlayer ? "1fr 320px" : "1fr" }}>
        {/* Main Content Pane */}
        <div className="preview-panel" style={{ overflowX: "auto" }}>
          {subTab === "economy" ? (
            <table className="db-table" style={{ width: "100%", borderCollapse: "collapse", textAlign: "left" }}>
              <thead>
                <tr style={{ borderBottom: "2px solid #334155" }}>
                  <th style={{ padding: "8px" }}>플레이어명</th>
                  <th style={{ padding: "8px" }}>UUID</th>
                  <th style={{ padding: "8px", textAlign: "right" }}>조개 (Clam)</th>
                  <th style={{ padding: "8px", textAlign: "right" }}>진주 (Pearl)</th>
                  <th style={{ padding: "8px", textAlign: "center" }}>평판 점수 (Rep)</th>
                  <th style={{ padding: "8px", textAlign: "center" }}>하드모드 여부</th>
                  <th style={{ padding: "8px", textAlign: "center" }}>관리</th>
                </tr>
              </thead>
              <tbody>
                {filteredPlayers.length === 0 ? (
                  <tr>
                    <td colSpan="7" style={{ textAlign: "center", padding: "20px", color: "#64748b" }}>
                      데이터가 존재하지 않습니다.
                    </td>
                  </tr>
                ) : (
                  filteredPlayers.map((p) => (
                    <tr key={p.uuid} style={{ borderBottom: "1px solid #1e293b", hover: { background: "#1e293b" } }}>
                      <td style={{ padding: "10px", fontWeight: "bold", color: "#38bdf8" }}>{p.name}</td>
                      <td style={{ padding: "10px", fontSize: "11px", color: "#94a3b8" }}>{p.uuid}</td>
                      <td style={{ padding: "10px", textAlign: "right" }}>{p.clam.toLocaleString()}</td>
                      <td style={{ padding: "10px", textAlign: "right" }}>{p.pearl.toLocaleString()}</td>
                      <td style={{ padding: "10px", textAlign: "center" }}>
                        <span style={{
                          background: "#0f172a",
                          color: "#fbbf24",
                          padding: "2px 8px",
                          borderRadius: "12px",
                          fontSize: "12px",
                          fontWeight: "bold"
                        }}>
                          {p.rep}
                        </span>
                      </td>
                      <td style={{ padding: "10px", textAlign: "center" }}>
                        {p.hardMode ? (
                          <span style={{ color: "#ef4444", fontWeight: "bold" }}>❤️ HARDCORE</span>
                        ) : (
                          <span style={{ color: "#10b981" }}>NORMAL</span>
                        )}
                      </td>
                      <td style={{ padding: "10px", textAlign: "center" }}>
                        <button
                          onClick={() => handleEditClick(p)}
                          className="primary"
                          style={{ padding: "4px 8px", fontSize: "12px" }}
                        >
                          ✏️ 수정
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          ) : (
            <table className="db-table" style={{ width: "100%", borderCollapse: "collapse", textAlign: "left" }}>
              <thead>
                <tr style={{ borderBottom: "2px solid #334155" }}>
                  <th style={{ padding: "8px" }}>네메시스 몹명</th>
                  <th style={{ padding: "8px" }}>몹 UUID</th>
                  <th style={{ padding: "8px" }}>타겟 플레이어 UUID</th>
                  <th style={{ padding: "8px" }}>접두사 (Affixes)</th>
                  <th style={{ padding: "8px", textAlign: "center" }}>누적 처치 수</th>
                  <th style={{ padding: "8px", textAlign: "center" }}>활성화 여부</th>
                </tr>
              </thead>
              <tbody>
                {filteredNemesis.length === 0 ? (
                  <tr>
                    <td colSpan="6" style={{ textAlign: "center", padding: "20px", color: "#64748b" }}>
                      데이터가 존재하지 않습니다.
                    </td>
                  </tr>
                ) : (
                  filteredNemesis.map((n) => (
                    <tr key={n.mobUuid} style={{ borderBottom: "1px solid #1e293b" }}>
                      <td style={{ padding: "10px", fontWeight: "bold", color: "#f87171" }}>
                        💀 {n.originalName}
                      </td>
                      <td style={{ padding: "10px", fontSize: "11px", color: "#94a3b8" }}>{n.mobUuid}</td>
                      <td style={{ padding: "10px", fontSize: "11px", color: "#94a3b8" }}>{n.playerUuid}</td>
                      <td style={{ padding: "10px" }}>
                        {n.affixesCsv ? (
                          n.affixesCsv.split(",").map((affix, idx) => (
                            <span
                              key={idx}
                              style={{
                                background: "#7f1d1d",
                                color: "#fca5a5",
                                padding: "2px 6px",
                                borderRadius: "4px",
                                fontSize: "10px",
                                marginRight: "4px",
                                border: "1px solid #b91c1c"
                              }}
                            >
                              {affix.trim()}
                            </span>
                          ))
                        ) : (
                          <span style={{ color: "#64748b", fontSize: "11px" }}>없음</span>
                        )}
                      </td>
                      <td style={{ padding: "10px", textAlign: "center", fontWeight: "bold" }}>
                        {n.killCount}
                      </td>
                      <td style={{ padding: "10px", textAlign: "center" }}>
                        {n.active ? (
                          <span style={{
                            background: "#065f46",
                            color: "#34d399",
                            padding: "2px 8px",
                            borderRadius: "12px",
                            fontSize: "11px",
                            fontWeight: "bold"
                          }}>
                            추격 중
                          </span>
                        ) : (
                          <span style={{
                            background: "#334155",
                            color: "#94a3b8",
                            padding: "2px 8px",
                            borderRadius: "12px",
                            fontSize: "11px"
                          }}>
                            종료됨
                          </span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </div>

        {/* Sidebar Form Panel for Editing Player */}
        {editingPlayer && (
          <div className="form-panel" style={{ animation: "fadeIn 0.2s ease" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h3 style={{ margin: 0 }}>✏️ 플레이어 수정</h3>
              <button
                onClick={() => setEditingPlayer(null)}
                style={{
                  background: "transparent",
                  color: "#94a3b8",
                  border: "none",
                  fontSize: "20px",
                  cursor: "pointer"
                }}
              >
                &times;
              </button>
            </div>
            <div style={{ marginBottom: "12px" }}>
              <span style={{ fontSize: "11px", color: "#94a3b8" }}>플레이어:</span>
              <div style={{ fontSize: "15px", fontWeight: "bold", color: "#38bdf8" }}>{editingPlayer.name}</div>
              <span style={{ fontSize: "10px", color: "#64748b" }}>{editingPlayer.uuid}</span>
            </div>

            <form onSubmit={handleSaveEdit} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <div className="form-group">
                <label>조개 (Clams)</label>
                <input
                  type="number"
                  min="0"
                  value={editClam}
                  onChange={(e) => setEditClam(e.target.value)}
                  style={{
                    width: "100%",
                    background: "#0f172a",
                    border: "1px solid #334155",
                    color: "#38bdf8",
                    padding: "8px",
                    borderRadius: "6px"
                  }}
                />
              </div>

              <div className="form-group">
                <label>진주 (Pearls)</label>
                <input
                  type="number"
                  min="0"
                  value={editPearl}
                  onChange={(e) => setEditPearl(e.target.value)}
                  style={{
                    width: "100%",
                    background: "#0f172a",
                    border: "1px solid #334155",
                    color: "#38bdf8",
                    padding: "8px",
                    borderRadius: "6px"
                  }}
                />
              </div>

              <div className="form-group">
                <label>평판 점수 (Reputation)</label>
                <input
                  type="number"
                  min="0"
                  value={editRep}
                  onChange={(e) => setEditRep(e.target.value)}
                  style={{
                    width: "100%",
                    background: "#0f172a",
                    border: "1px solid #334155",
                    color: "#38bdf8",
                    padding: "8px",
                    borderRadius: "6px"
                  }}
                />
              </div>

              <div className="form-group" style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "6px" }}>
                <input
                  type="checkbox"
                  id="editHardMode"
                  checked={editHardMode}
                  onChange={(e) => setEditHardMode(e.target.checked)}
                  style={{ width: "18px", height: "18px", cursor: "pointer" }}
                />
                <label htmlFor="editHardMode" style={{ cursor: "pointer", color: "#f1f5f9" }}>
                  하드코어 상태 지정 (사망 시 디버프)
                </label>
              </div>

              <button
                type="submit"
                className="primary"
                disabled={isSaving}
                style={{ width: "100%", padding: "10px", marginTop: "8px" }}
              >
                {isSaving ? "저장 중..." : "💾 데이터베이스 반영"}
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}
