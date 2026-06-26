import React, { useState, useEffect } from "react";
import { generateContent, deployContent } from "../api.js";
import YamlPreviewPanel from "../components/YamlPreviewPanel.jsx";

const RUNE_TYPES = ["lifesteal", "lightning", "slow", "shield", "berserk"];
const GRADES = [1, 2, 3, 4, 5];

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

export default function RuneTab({ loadedConfig, onClearLoaded }) {
  const [id, setId] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [type, setType] = useState(RUNE_TYPES[0]);
  const [grade, setGrade] = useState(1);
  const [material, setMaterial] = useState("AMETHYST_SHARD");
  const [customModelData, setCustomModelData] = useState(3000);
  const [effectValue, setEffectValue] = useState(0.08);
  const [fusionInputId, setFusionInputId] = useState("");
  const [fusionCostClam, setFusionCostClam] = useState(200);
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
      setType(loadedConfig.type || RUNE_TYPES[0]);
      setGrade(loadedConfig.grade || 1);
      setMaterial(loadedConfig.material || "AMETHYST_SHARD");
      setCustomModelData(loadedConfig.custom_model_data || 3000);
      
      const effect = loadedConfig.effect || {};
      setEffectValue(effect.value || 0.08);
      
      const fusion = loadedConfig.fusion || {};
      setFusionInputId(fusion.input_id || "");
      setFusionCostClam(fusion.cost_clam || 200);
      
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
            display_name: displayName || `§c${type} ${grade}`,
            type,
            grade: Number(grade),
            material,
            custom_model_data: Number(customModelData),
            effect: { type, value: Number(effectValue) },
            ...(fusionInputId
              ? { fusion: { input_id: fusionInputId, input_count: 3, cost_clam: Number(fusionCostClam), output_id: id } }
              : {}),
          },
        };
    try {
      const result = await generateContent("rune", payload);
      setYamlText(result.yaml);
    } catch (error) {
      setDeployStatus(`오류: ${error.message}`);
    }
  }

  async function handleDeploy() {
    try {
      const fileId = (yamlText.match(/^id:\s*(.+)$/m) || [, id])[1].trim();
      const result = await deployContent({ file_content: yamlText, file_type: "rune", file_id: fileId });
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
        <h2>룬 생성기</h2>
        <label>
          룬 ID
          <input value={id} onChange={(e) => setId(e.target.value)} placeholder="rune_lifesteal_3" />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>룬 고유 식별자 명칭입니다. (예: rune_lifesteal_3)</span>
        </label>
        
        <label>
          표시 이름
          <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="§c흡혈의 룬 III" />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>게임 인게임 내 룬 아이템의 이름입니다. 색상 코드 지정 가능. (예: §c흡혈의 룬 III)</span>
        </label>
        
        <label>
          효과 타입
          <select value={type} onChange={(e) => setType(e.target.value)}>
            {RUNE_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>룬이 활성화할 전투 시너지 효과의 유형입니다.</span>
        </label>
        
        <label>
          룬 등급 (1~5)
          <select value={grade} onChange={(e) => setGrade(e.target.value)}>
            {GRADES.map((g) => <option key={g} value={g}>{g}</option>)}
          </select>
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>룬의 품질 등급 단계입니다. 세트 시너지 및 융합 기능에 관여합니다.</span>
        </label>
        
        <label>
          재질 (Material)
          <input value={material} onChange={(e) => setMaterial(e.target.value.toUpperCase())} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>아이템의 기본 바닐라 재질 유형입니다. (예: AMETHYST_SHARD)</span>
        </label>
        
        <label>
          CustomModelData 번호
          <input type="number" value={customModelData} onChange={(e) => setCustomModelData(e.target.value)} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>리소스팩 룬 텍스처를 맵핑할 고유 CMD 번호입니다.</span>
        </label>
        
        <label>
          효과치 (Effect Value): {effectValue}
          <input type="range" min="0" max="1" step="0.01" value={effectValue} onChange={(e) => setEffectValue(e.target.value)} />
          <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>룬 효과의 성능 배율/확률 수치입니다. (예: 0.08 = 흡혈율 8%, 번개 발동률 8%)</span>
        </label>

        <fieldset>
          <legend>룬 융합 레시피 (선택)</legend>
          <label>
            입력 재료 룬 ID
            <input value={fusionInputId} onChange={(e) => setFusionInputId(e.target.value)} placeholder="rune_lifesteal_2" />
            <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>대장간 융합 시 재료로 바칠 한 단계 낮은 하위 룬의 고유 ID입니다.</span>
          </label>
          <label>
            융합 소모 비용 (조개)
            <input type="number" value={fusionCostClam} onChange={(e) => setFusionCostClam(e.target.value)} />
            <span style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>재료 룬 3개와 함께 상위 룬 융합을 위해 소모할 조개(Clam) 비용입니다.</span>
          </label>
        </fieldset>

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
            placeholder="피해의 24%를 흡혈하는 3등급 룬" />
        </label>

        <button className="primary" onClick={handleGenerate}>YAML 미리보기 생성</button>
      </div>

      <YamlPreviewPanel yamlText={yamlText} fileId={id} fileType="rune" onDeploy={handleDeploy} deployStatus={deployStatus} />
    </div>
  );
}
