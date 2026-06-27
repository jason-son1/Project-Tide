import React, { useState } from "react";

export default function GuideTab() {
  const [activeSection, setActiveSection] = useState("architecture");

  return (
    <div className="tab-grid" style={{ gridTemplateColumns: "240px 1fr", gap: "20px" }}>
      {/* Left Sidebar: Guide Navigation */}
      <div className="form-panel" style={{ maxHeight: "calc(100vh - 160px)", overflowY: "auto" }}>
        <h3>📖 가이드 메뉴</h3>
        <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
          <button
            onClick={() => setActiveSection("architecture")}
            className={`sidebar-item-btn ${activeSection === "architecture" ? "selected" : ""}`}
            style={{
              textAlign: "left",
              background: activeSection === "architecture" ? "#0ea5e9" : "transparent",
              color: activeSection === "architecture" ? "white" : "#cbd5e1",
              border: "none",
              padding: "8px 12px",
              borderRadius: "4px",
              cursor: "pointer",
              fontSize: "13px"
            }}
          >
            🧩 3대 시스템 아키텍처
          </button>
          <button
            onClick={() => setActiveSection("gearscore")}
            className={`sidebar-item-btn ${activeSection === "gearscore" ? "selected" : ""}`}
            style={{
              textAlign: "left",
              background: activeSection === "gearscore" ? "#0ea5e9" : "transparent",
              color: activeSection === "gearscore" ? "white" : "#cbd5e1",
              border: "none",
              padding: "8px 12px",
              borderRadius: "4px",
              cursor: "pointer",
              fontSize: "13px"
            }}
          >
            ⚔️ 기어스코어 및 대장간
          </button>
          <button
            onClick={() => setActiveSection("runes")}
            className={`sidebar-item-btn ${activeSection === "runes" ? "selected" : ""}`}
            style={{
              textAlign: "left",
              background: activeSection === "runes" ? "#0ea5e9" : "transparent",
              color: activeSection === "runes" ? "white" : "#cbd5e1",
              border: "none",
              padding: "8px 12px",
              borderRadius: "4px",
              cursor: "pointer",
              fontSize: "13px"
            }}
          >
            💎 룬 장착 및 세트 효과
          </button>
          <button
            onClick={() => setActiveSection("mobs")}
            className={`sidebar-item-btn ${activeSection === "mobs" ? "selected" : ""}`}
            style={{
              textAlign: "left",
              background: activeSection === "mobs" ? "#0ea5e9" : "transparent",
              color: activeSection === "mobs" ? "white" : "#cbd5e1",
              border: "none",
              padding: "8px 12px",
              borderRadius: "4px",
              cursor: "pointer",
              fontSize: "13px"
            }}
          >
            👹 정예 몹 및 스폰 규칙
          </button>
          <button
            onClick={() => setActiveSection("nemesis")}
            className={`sidebar-item-btn ${activeSection === "nemesis" ? "selected" : ""}`}
            style={{
              textAlign: "left",
              background: activeSection === "nemesis" ? "#0ea5e9" : "transparent",
              color: activeSection === "nemesis" ? "white" : "#cbd5e1",
              border: "none",
              padding: "8px 12px",
              borderRadius: "4px",
              cursor: "pointer",
              fontSize: "13px"
            }}
          >
            💀 네메시스(복수자) 시스템
          </button>
          <button
            onClick={() => setActiveSection("tidecore")}
            className={`sidebar-item-btn ${activeSection === "tidecore" ? "selected" : ""}`}
            style={{
              textAlign: "left",
              background: activeSection === "tidecore" ? "#0ea5e9" : "transparent",
              color: activeSection === "tidecore" ? "white" : "#cbd5e1",
              border: "none",
              padding: "8px 12px",
              borderRadius: "4px",
              cursor: "pointer",
              fontSize: "13px"
            }}
          >
            🌊 조수 시스템 & 하드코어 데스
          </button>
        </div>
      </div>

      {/* Right Content Pane */}
      <div className="preview-panel" style={{ minHeight: "500px", padding: "24px" }}>
        {activeSection === "architecture" && (
          <div>
            <h2 style={{ color: "#38bdf8", marginTop: 0 }}>🧩 3대 시스템 아키텍처 개요</h2>
            <p>The Tide v2 서버는 기능별로 독립된 3개의 Spigot 플러그인과 1개의 파일 자동화 생성기로 긴밀히 연결되어 작동합니다.</p>
            
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px", marginTop: "16px" }}>
              <div style={{ background: "#1e293b", padding: "16px", borderRadius: "8px", borderLeft: "4px solid #38bdf8" }}>
                <h4 style={{ margin: "0 0 8px", color: "#f1f5f9" }}>🌊 TideCore</h4>
                <p style={{ fontSize: "13px", color: "#cbd5e1", margin: 0 }}>
                  서버의 핵심 기반을 제공합니다. 주기적인 조수(Tide) 상태 주기 제어, 클램/진주/평판에 기반한 기본 경제 시스템, 사망 시 유실물 비석을 세우고 디버프를 부여하는 소프트-하드코어 사망 시스템, 관리자 RCON 웹서버 등이 포함되어 있습니다.
                </p>
              </div>

              <div style={{ background: "#1e293b", padding: "16px", borderRadius: "8px", borderLeft: "4px solid #fbbf24" }}>
                <h4 style={{ margin: "0 0 8px", color: "#f1f5f9" }}>⚔️ TideRPG</h4>
                <p style={{ fontSize: "13px", color: "#cbd5e1", margin: 0 }}>
                  플레이어 성장과 파밍 콘텐츠를 담당합니다. 아이템 기어스코어(GS) 계산, 강화·룬 장착·룬 리롤·룬 융합을 아우르는 통합 대장간 GUI, 높은 조수 상태에만 열 수 있는 조수 금고(Tide Vault) 및 블러드 문 소환템(Tide Bell) 등이 포함됩니다.
                </p>
              </div>

              <div style={{ background: "#1e293b", padding: "16px", borderRadius: "8px", borderLeft: "4px solid #f87171" }}>
                <h4 style={{ margin: "0 0 8px", color: "#f1f5f9" }}>👹 TideMobs</h4>
                <p style={{ fontSize: "13px", color: "#cbd5e1", margin: 0 }}>
                  몬스터 위협 및 사냥 처치 보상을 담당합니다. 자연 소폰 시 접두사(어픽스)를 씌우거나 커스텀 스탯을 반영하는 정예화 스폰 시스템, 플레이어를 죽인 몹이 강화되어 복수자로 변하는 네메시스 시스템, 일일/주간 현상금 퀘스트 및 보스 소환 제단이 포함됩니다.
                </p>
              </div>

              <div style={{ background: "#1e293b", padding: "16px", borderRadius: "8px", borderLeft: "4px solid #10b981" }}>
                <h4 style={{ margin: "0 0 8px", color: "#f1f5f9" }}>🤖 Web Customizer (현재 웹 앱)</h4>
                <p style={{ fontSize: "13px", color: "#cbd5e1", margin: 0 }}>
                  서버 재시작 없이 실시간으로 아이템, 룬, 몹, 보스 속성을 변경·핫 리로드할 수 있도록 돕는 React/FastAPI 도구입니다. 자연어로 요청하면 규칙에 맞게 파일을 자동으로 생성해 주는 AI 생성을 완벽 지원합니다.
                </p>
              </div>
            </div>
          </div>
        )}

        {activeSection === "gearscore" && (
          <div>
            <h2 style={{ color: "#38bdf8", marginTop: 0 }}>⚔️ 기어스코어 및 통합 대장간 시스템</h2>
            <p>장비의 핵심 지표인 기어스코어(GS)를 바탕으로, 플레이어는 통합 대장간 명령어를 통해 장비를 강화하고 룬을 박아 성장해 나갑니다.</p>

            <h3 style={{ color: "#cbd5e1" }}>1. 기어스코어 (Gear Score)</h3>
            <ul style={{ fontSize: "13px", lineHeight: "1.6", color: "#cbd5e1" }}>
              <li><strong>역할</strong>: 장비의 전반적인 전투력을 대변하는 스탯입니다.</li>
              <li><strong>활용</strong>: 기어스코어가 낮으면 특정 고난이도 사냥 구역 진입 시 경고 및 디버프를 받게 됩니다.</li>
            </ul>

            <h3 style={{ color: "#cbd5e1" }}>2. 통합 대장간 GUI (`/forge`)</h3>
            <ul style={{ fontSize: "13px", lineHeight: "1.6", color: "#cbd5e1" }}>
              <li><strong>1강화 탭</strong>: 조개(Clam)를 지불하고 장비 등급별 확률에 맞추어 공격력/방어력을 1성 단위로 올립니다. 강화당 증가하는 보너스 수치는 아이템 YAML 설정에서 직접 기입합니다.</li>
              <li><strong>2룬 장착 탭</strong>: 장비가 가진 소켓 수(최대 3개) 내에서 획득한 특수 효과 룬을 아이템에 장착합니다.</li>
              <li><strong>3룬 리롤 탭</strong>: 룬 효과의 무작위 변동폭 수치(예: 흡혈율 5%~12%)를 재조정하기 위해 주사위를 굴립니다.</li>
              <li><strong>4룬 융합 탭</strong>: 동일한 종류와 등급의 룬 3개와 조개를 지불하여 상위 등급의 룬 1개로 업그레이드합니다.</li>
            </ul>
          </div>
        )}

        {activeSection === "runes" && (
          <div>
            <h2 style={{ color: "#38bdf8", marginTop: 0 }}>💎 룬 효과 장착 및 세트 효과</h2>
            <p>아이템 소켓에 결합되는 룬 조각들은 장착 시 특별한 전투 보너스를 유발하고, 조건 충족 시 세트 효과를 발생시킵니다.</p>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px", marginTop: "16px" }}>
              <div style={{ background: "#0f172a", padding: "12px", borderRadius: "6px" }}>
                <h4 style={{ margin: "0 0 6px", color: "#e2e8f0" }}>🧪 룬 특수 효과 종류</h4>
                <ul style={{ fontSize: "12px", paddingLeft: "20px", color: "#94a3b8", lineHeight: "1.6" }}>
                  <li><strong style={{ color: "#f87171" }}>Lifesteal (흡혈)</strong>: 적에게 가한 물리 피해의 일정 % 만큼 자신의 HP로 즉시 흡수 회복합니다.</li>
                  <li><strong style={{ color: "#60a5fa" }}>Lightning (번개)</strong>: 공격 성공 시 확률적으로 타겟 주변 3블록 이내 몬스터들에게 연쇄 번개 피해를 가합니다.</li>
                  <li><strong style={{ color: "#fbbf24" }}>Slow (감속)</strong>: 적 공격 시 감속 디버프를 유발해 적의 도주 및 접근을 차단합니다.</li>
                  <li><strong style={{ color: "#34d399" }}>Shield (보호막)</strong>: 피격 시 임시 데미지 흡수 쉴드를 획득하여 치명적인 피해로부터 보호합니다.</li>
                  <li><strong style={{ color: "#c084fc" }}>Berserk (광전사)</strong>: 플레이어의 현재 체력이 적어질수록 공격 데미지량이 대폭 증가합니다.</li>
                </ul>
              </div>

              <div style={{ background: "#0f172a", padding: "12px", borderRadius: "6px" }}>
                <h4 style={{ margin: "0 0 6px", color: "#e2e8f0" }}>✨ 세트 보너스 (Set Bonus)</h4>
                <p style={{ fontSize: "12px", color: "#94a3b8", lineHeight: "1.5", margin: 0 }}>
                  한 장비에 <strong>동일한 등급 및 동일한 종류의 룬 3개</strong>를 장착할 경우, 숨겨진 룬의 시너지 능력이 개방되어 특수 능력 효과 수치가 1.5배로 대폭 증폭됩니다.<br />
                  예를 들어 흡혈 룬 5등급 3개를 하나의 대검 소켓에 전부 장착하면 단일 장착 시보다 높은 흡혈 배율과 추가 연출이 제공됩니다.
                </p>
              </div>
            </div>
          </div>
        )}

        {activeSection === "mobs" && (
          <div>
            <h2 style={{ color: "#38bdf8", marginTop: 0 }}>👹 정예 몹 속성 및 자연 스폰 법칙</h2>
            <p>자연 스폰되는 기본 마인크래프트 몬스터들은 조수 상태와 난이도 조건에 맞춰 정예화 접두사(Affixes)를 달고 태어납니다.</p>

            <h3 style={{ color: "#cbd5e1" }}>1. 접두사(어픽스) 속성 목록</h3>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "8px", fontSize: "12px" }}>
              <div style={{ background: "#1e293b", padding: "8px", borderRadius: "4px" }}><strong>🔥 염화의</strong>: 피격 시 주변 바닥에 화염 구역을 형성합니다.</div>
              <div style={{ background: "#1e293b", padding: "8px", borderRadius: "4px" }}><strong>⚡ 신속의</strong>: 영구 신속 버프와 빠른 이동속도를 갖습니다.</div>
              <div style={{ background: "#1e293b", padding: "8px", borderRadius: "4px" }}><strong>💥 폭심의</strong>: 사망하는 순간 넓은 범위의 폭발 대미지를 입힙니다.</div>
              <div style={{ background: "#1e293b", padding: "8px", borderRadius: "4px" }}><strong>👥 분열의</strong>: 쓰러지면 2마리의 소형 분신 몹으로 쪼개져 재생성됩니다.</div>
              <div style={{ background: "#1e293b", padding: "8px", borderRadius: "4px" }}><strong>🛡️ 강철의</strong>: 넉백 면역과 물리 데미지 감쇄 패시브를 얻습니다.</div>
              <div style={{ background: "#1e293b", padding: "8px", borderRadius: "4px" }}><strong>💚 재생의</strong>: 체력이 손실되면 매 초마다 최대 HP 비례 자가 회복을 합니다.</div>
              <div style={{ background: "#1e293b", padding: "8px", borderRadius: "4px" }}><strong>🌵 가시의</strong>: 플레이어가 공격할 때마다 대미지의 15%를 반사시킵니다.</div>
              <div style={{ background: "#1e293b", padding: "8px", borderRadius: "4px" }}><strong>🧿 보호막의</strong>: 피격 시 수시로 추가 보호막 임시 하트를 형성합니다.</div>
            </div>

            <h3 style={{ color: "#cbd5e1", marginTop: "16px" }}>2. 커스텀 몹 스폰 매칭</h3>
            <p style={{ fontSize: "13px", color: "#cbd5e1" }}>
              AI 생성기로 만든 몹은 특수한 바이옴(Biomes), 지정된 조수 상태(Tide States), 스폰 가중치(Weight) 값을 가집니다. 플레이어가 스폰 구역 근처를 지나갈 때 엔진이 가중치 주사위를 굴려 당첨된 경우 일반 몹 대신 이 정예 커스텀 몹을 생성시킵니다.
            </p>
          </div>
        )}

        {activeSection === "nemesis" && (
          <div>
            <h2 style={{ color: "#38bdf8", marginTop: 0 }}>💀 네메시스(Nemesis) 복수자 추격 시스템</h2>
            <p>마인크래프트 야생에서 플레이어의 방심을 유도하는 최악의 위협이자, 기어스코어를 돌파하기 위한 최종 보상 파밍 수단입니다.</p>

            <div style={{ background: "#111827", border: "1px solid #7f1d1d", padding: "16px", borderRadius: "8px", margin: "16px 0" }}>
              <h4 style={{ margin: "0 0 8px", color: "#f87171" }}>🔄 복수의 순환 프로세스</h4>
              <ol style={{ fontSize: "13px", color: "#cbd5e1", paddingLeft: "20px", lineHeight: "1.6", margin: 0 }}>
                <li>플레이어가 정예 몹이나 보스에게 **사망**하면, 플레이어를 죽인 몬스터가 특별한 PDC 태그를 받고 **[네메시스 복수자]**로 승격됩니다.</li>
                <li>승격된 몹은 죽은 플레이어의 이름을 새기고, **신속/힘 버프**를 장착하여 체력 회복과 함께 서버 데이터베이스(SQLite 또는 Local YAML)에 영구 저장됩니다.</li>
                <li>서버가 재시작되어도 네메시스는 유지되며, 해당 플레이어가 근처에 접속하면 **복수자 추격 AI**가 활성화되어 스폰 위치로부터 최단 거리로 플레이어를 사냥하러 쫓아옵니다.</li>
                <li>플레이어가 본인의 네메시스를 역으로 처치하는 데 성공하면, 전용 화폐인 **네메시스 토큰(Nemesis Token)**과 고성능 복수 장비를 획득할 수 있습니다.</li>
              </ol>
            </div>
          </div>
        )}

        {activeSection === "tidecore" && (
          <div>
            <h2 style={{ color: "#38bdf8", marginTop: 0 }}>🌊 조수(Tide) 상태 제어 및 사망 시스템</h2>
            <p>밀물과 썰물처럼 서버 환경 전체의 날씨, 스폰 난이도, 경제 가치가 실시간 조수 상태에 따라 극적으로 바뀝니다.</p>

            <h3 style={{ color: "#cbd5e1" }}>1. 조수 상태 변화</h3>
            <ul style={{ fontSize: "13px", lineHeight: "1.6", color: "#cbd5e1" }}>
              <li><strong>LOW_TIDE (썰물)</strong>: 평화로운 탐험 단계. 몹 스폰 속도가 낮고 낚시 성공률이 낮습니다.</li>
              <li><strong>HIGH_TIDE / SPRING_TIDE (밀물 / 사리)</strong>: 몹 스탯이 상승하며 사냥 및 상점 판매 가격이 보너스를 받습니다.</li>
              <li><strong>BLOOD_MOON / BLOOD_TIDE (붉은 달)</strong>: 최악의 재앙 상태. 야생에 거대한 엘리트 정예군단이 대량 출몰하며, 숨겨진 <strong>조수 금고(Tide Vault)</strong>가 활성화되어 룬 보물상자를 털 기회가 생깁니다.</li>
            </ul>

            <h3 style={{ color: "#cbd5e1", marginTop: "16px" }}>2. "조류에 휩쓸림" 소프트 하드코어 사망 시스템</h3>
            <ul style={{ fontSize: "13px", lineHeight: "1.6", color: "#cbd5e1" }}>
              <li><strong>하드 모드 상태</strong>: 개별 플레이어는 `/tide hardcore` 토글을 통해 하드코어 모드를 켤 수 있습니다.</li>
              <li><strong>사망 시 유실물 비석</strong>: 하드코어 플레이어가 사망하면 그 자리에 아머스탠드로 비석이 세워지고 모든 아이템이 비석 내부 보관함에 격리됩니다.</li>
              <li><strong>회수 및 부활 디버프</strong>: 부활 시 플레이어는 심각한 부상 디버프(피로, 감속 등)를 입으며, 사망한 비석 자리에 가서 우클릭으로 아이템을 정상 회수할 때까지 디버프가 지속됩니다. 회수 전에 재차 사망하면 비석 내부 템 중 일부가 영구 소실될 위험이 존재합니다.</li>
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
