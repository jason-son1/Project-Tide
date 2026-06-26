import React, { useState, useEffect } from "react";
import { generateContent, deployContent } from "../api.js";
import YamlPreviewPanel from "../components/YamlPreviewPanel.jsx";

const GEMINI_MODELS = [
  "gemini-3.5-flash",
  "gemini-3.5-pro",
  "gemini-2.5-flash",
  "gemini-2.5-pro",
  "gemini-2.0-flash",
  "gemini-2.0-pro",
  "gemini-1.5-flash",
  "gemini-1.5-pro"
];
const CLAUDE_MODELS = [
  "claude-4-6-sonnet",
  "claude-4-5-sonnet",
  "claude-4-0-haiku",
  "claude-3-5-sonnet-20241022",
  "claude-3-5-haiku-20241022"
];

export default function ItemTab({ loadedConfig, onClearLoaded }) {
  const [id, setId] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [material, setMaterial] = useState("IRON_SWORD");
  const [customModelData, setCustomModelData] = useState(1000);
  const [gearScore, setGearScore] = useState(150);
  const [tier, setTier] = useState(1);
  const [damage, setDamage] = useState(10);
  const [defense, setDefense] = useState(0);
  const [damagePerStar, setDamagePerStar] = useState(3);
  const [socketCount, setSocketCount] = useState(1);
  const [sellPrice, setSellPrice] = useState(20);
  const [requestText, setRequestText] = useState("");
  const [yamlText, setYamlText] = useState("");
  const [deployStatus, setDeployStatus] = useState("");

  const [aiProvider, setAiProvider] = useState(localStorage.getItem("ai_provider") || "gemini");
  const [aiModel, setAiModel] = useState(localStorage.getItem("ai_model") || "gemini-3.5-flash");
  const [aiApiKey, setAiApiKey] = useState(localStorage.getItem("ai_api_key") || "");

  function handleProviderChange(prov) {
    setAiProvider(prov);
    const defaultModel = prov === "gemini" ? "gemini-3.5-flash" : "claude-4-6-sonnet";
    setAiModel(defaultModel);
    localStorage.setItem("ai_provider", prov);
    localStorage.setItem("ai_model", defaultModel);
  }

  function handleApiKeyChange(key) {
    setAiApiKey(key);
    localStorage.setItem("ai_api_key", key);
  }

  useEffect(() => {
    if (loadedConfig) {
      setId(loadedConfig.id || "");
      setDisplayName(loadedConfig.display_name || "");
      setMaterial(loadedConfig.material || "IRON_SWORD");
      setCustomModelData(loadedConfig.custom_model_data || 1000);
      setGearScore(loadedConfig.gear_score || 150);
      setTier(loadedConfig.tier || 1);
      
      const stats = loadedConfig.base_stats || {};
      setDamage(stats.damage || 0);
      setDefense(stats.defense || 0);
      
      const reinforce = loadedConfig.reinforce_bonus || {};
      setDamagePerStar(reinforce.damage_per_star || 0);
      
      setSocketCount(loadedConfig.socket_count || 0);
      setSellPrice(loadedConfig.sell_price || 0);
      
      setYamlText("");
      setRequestText("");
      setDeployStatus("서버에서 설정을 불러왔습니다. 수정 후 미리보기를 생성하세요.");
      
      onClearLoaded();
    }
  }, [loadedConfig]);


  async function handleGenerate() {
    setDeployStatus("");
    const payload = requestText.trim()
      ? {
          request_text: requestText.trim(),
          provider: aiProvider,
          model: aiModel,
          api_key: aiApiKey || undefined,
        }
      : {
          form_data: {
            id,
            display_name: displayName || `§6[${id}]`,
            material,
            custom_model_data: Number(customModelData),
            gear_score: Number(gearScore),
            tier: Number(tier),
            base_stats: { damage: Number(damage), defense: Number(defense) },
            reinforce_bonus: { damage_per_star: Number(damagePerStar), defense_per_star: 0 },
            socket_count: Number(socketCount),
            socket_max: 3,
            sell_price: Number(sellPrice),
          },
        };
    try {
      const result = await generateContent("item", payload);
      setYamlText(result.yaml);
    } catch (error) {
      setDeployStatus(`오류: ${error.message}`);
    }
  }

  async function handleDeploy() {
    try {
      const fileId = (yamlText.match(/^id:\s*(.+)$/m) || [, id])[1].trim();
      const result = await deployContent({ file_content: yamlText, file_type: "item", file_id: fileId });
      setDeployStatus(`배포 완료: ${result.path} (reload: ${result.reload_target}, rcon: ${result.rcon_sent})`);
    } catch (error) {
      setDeployStatus(`배포 오류: ${error.message}`);
    }
  }

  const isCustomModel = aiProvider === "gemini"
    ? !GEMINI_MODELS.includes(aiModel)
    : !CLAUDE_MODELS.includes(aiModel);

  return (
    <div className="tab-grid">
      <div className="form-panel">
        <h2>아이템 생성기</h2>
        <label>
          아이템 ID
          <input value={id} onChange={(e) => setId(e.target.value)} placeholder="flame_sword_t2" />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>게임 내 고유 ID로, 영문 소문자와 언더바(_)만 지원합니다. (예: flame_sword_t2)</span>
        </label>
        
        <label>
          표시 이름
          <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="§6[불꽃의 대검]" />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>게임 내 장비 이름입니다. 색상 코드(§) 사용이 가능합니다. (예: §6[불꽃의 대검])</span>
        </label>
        
        <label>
          재질 (Material)
          <input value={material} onChange={(e) => setMaterial(e.target.value.toUpperCase())} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>바닐라 마인크래프트 아이템 재질 명칭입니다. (예: NETHERITE_SWORD, BOW)</span>
        </label>
        
        <label>
          CustomModelData 번호
          <input type="number" value={customModelData} onChange={(e) => setCustomModelData(e.target.value)} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>리소스팩 연동 시 사용할 모델 고유 아이디입니다. (예: 1000)</span>
        </label>
        
        <label>
          전투력 (Gear Score): {gearScore}
          <input type="range" min="100" max="1500" step="10" value={gearScore} onChange={(e) => setGearScore(e.target.value)} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>아이템의 강함을 나타내는 척도입니다. (100 ~ 1500)</span>
        </label>
        
        <label>
          장비 티어 (1~5)
          <input type="number" min="1" max="5" value={tier} onChange={(e) => setTier(e.target.value)} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>장비의 등급(Tier)을 지정합니다. (일반적으로 1~5티어)</span>
        </label>
        
        <label>
          기본 공격력 (Damage)
          <input type="number" value={damage} onChange={(e) => setDamage(e.target.value)} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>해당 무기 착용 시 증가하는 기본 데미지입니다.</span>
        </label>
        
        <label>
          기본 방어력 (Defense)
          <input type="number" value={defense} onChange={(e) => setDefense(e.target.value)} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>해당 방어구 착용 시 증가하는 기본 방어력입니다.</span>
        </label>
        
        <label>
          강화 당 데미지 상승치 (Damage Per Star)
          <input type="number" value={damagePerStar} onChange={(e) => setDamagePerStar(e.target.value)} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>대장간에서 강화에 성공해 별이 올라갈 때마다 추가되는 물리 공격력입니다.</span>
        </label>
        
        <label>
          룬 소켓 개수 (0~3)
          <input type="number" min="0" max="3" value={socketCount} onChange={(e) => setSocketCount(e.target.value)} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>룬을 장착할 수 있는 빈 구멍의 수입니다. (최대 3개)</span>
        </label>
        
        <label>
          판매 가격 (조개)
          <input type="number" value={sellPrice} onChange={(e) => setSellPrice(e.target.value)} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>`/sellall` 자동 환전 상점 등에 판매 시 지급될 기본 조개(Clam) 가격입니다.</span>
        </label>

        <fieldset style={{ marginBottom: "12px", border: "1px dashed #475569" }}>
          <legend style={{ color: "#38bdf8", fontSize: "12px" }}>🤖 AI 생성 설정</legend>
          <div style={{ display: "flex", gap: "8px", marginBottom: "8px" }}>
            <label style={{ flex: 1, margin: 0 }}>제공자
              <select value={aiProvider} onChange={(e) => handleProviderChange(e.target.value)} style={{ padding: "4px 8px" }}>
                <option value="gemini">Google Gemini</option>
                <option value="claude">Anthropic Claude</option>
              </select>
            </label>
            <label style={{ flex: 1, margin: 0 }}>모델
              <select 
                value={
                  aiProvider === "gemini"
                    ? (GEMINI_MODELS.includes(aiModel) ? aiModel : "custom")
                    : (CLAUDE_MODELS.includes(aiModel) ? aiModel : "custom")
                } 
                onChange={(e) => {
                  const val = e.target.value;
                  if (val === "custom") {
                    setAiModel("");
                  } else {
                    setAiModel(val);
                    localStorage.setItem("ai_model", val);
                  }
                }} 
                style={{ padding: "4px 8px" }}
              >
                {aiProvider === "gemini" ? (
                  <>
                    {GEMINI_MODELS.map(m => <option key={m} value={m}>{m}</option>)}
                    <option value="custom">직접 입력 (Custom)...</option>
                  </>
                ) : (
                  <>
                    {CLAUDE_MODELS.map(m => <option key={m} value={m}>{m}</option>)}
                    <option value="custom">직접 입력 (Custom)...</option>
                  </>
                )}
              </select>
            </label>
          </div>
          {isCustomModel && (
            <label style={{ margin: "0 0 8px 0" }}>모델명 직접 입력
              <input
                type="text"
                value={aiModel}
                onChange={(e) => {
                  setAiModel(e.target.value);
                  localStorage.setItem("ai_model", e.target.value);
                }}
                placeholder={aiProvider === "gemini" ? "예: gemini-3.5-pro" : "예: claude-4-6-sonnet"}
                style={{ padding: "6px 8px", fontSize: "12px" }}
              />
            </label>
          )}
          <label style={{ margin: 0 }}>API Key (비워두면 서버 키 사용)
            <input
              type="password"
              value={aiApiKey}
              onChange={(e) => handleApiKeyChange(e.target.value)}
              placeholder="API Key 입력..."
              style={{ padding: "6px 8px", fontSize: "12px" }}
            />
          </label>
        </fieldset>

        <label>또는 자연어 요청 (입력 시 폼 대신 AI가 생성)
          <textarea value={requestText} onChange={(e) => setRequestText(e.target.value)}
            placeholder="공격력이 높은 T2 네더라이트 대검, 소켓 2개" />
        </label>

        <button className="primary" onClick={handleGenerate}>YAML 미리보기 생성</button>
      </div>

      <YamlPreviewPanel yamlText={yamlText} fileId={id} fileType="item" onDeploy={handleDeploy} deployStatus={deployStatus} />
    </div>
  );
}
