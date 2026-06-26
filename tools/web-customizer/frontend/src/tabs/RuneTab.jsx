import React, { useState, useEffect } from "react";
import { generateContent, deployContent } from "../api.js";
import YamlPreviewPanel from "../components/YamlPreviewPanel.jsx";

const RUNE_TYPES = ["lifesteal", "lightning", "slow", "shield", "berserk"];
const GRADES = [1, 2, 3, 4, 5];

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
  const [aiModel, setAiModel] = useState(localStorage.getItem("ai_model") || "gemini-2.5-flash");
  const [aiApiKey, setAiApiKey] = useState(localStorage.getItem("ai_api_key") || "");

  function handleProviderChange(prov) {
    setAiProvider(prov);
    const defaultModel = prov === "gemini" ? "gemini-2.5-flash" : "claude-3-5-sonnet-20241022";
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

  return (
    <div className="tab-grid">
      <div className="form-panel">
        <h2>룬 생성기</h2>
        <label>룬 ID<input value={id} onChange={(e) => setId(e.target.value)} placeholder="rune_lifesteal_3" /></label>
        <label>표시 이름<input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="§c흡혈의 룬 III" /></label>
        <label>타입
          <select value={type} onChange={(e) => setType(e.target.value)}>
            {RUNE_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>
        <label>등급
          <select value={grade} onChange={(e) => setGrade(e.target.value)}>
            {GRADES.map((g) => <option key={g} value={g}>{g}</option>)}
          </select>
        </label>
        <label>재질<input value={material} onChange={(e) => setMaterial(e.target.value.toUpperCase())} /></label>
        <label>CustomModelData<input type="number" value={customModelData} onChange={(e) => setCustomModelData(e.target.value)} /></label>
        <label>효과값: {effectValue}
          <input type="range" min="0" max="1" step="0.01" value={effectValue} onChange={(e) => setEffectValue(e.target.value)} />
        </label>

        <fieldset>
          <legend>융합 레시피 (선택)</legend>
          <label>입력 룬 ID(한 단계 낮은 등급)<input value={fusionInputId} onChange={(e) => setFusionInputId(e.target.value)} placeholder="rune_lifesteal_2" /></label>
          <label>융합 비용(조개)<input type="number" value={fusionCostClam} onChange={(e) => setFusionCostClam(e.target.value)} /></label>
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
              <select value={aiModel} onChange={(e) => setAiModel(e.target.value)} style={{ padding: "4px 8px" }}>
                {aiProvider === "gemini" ? (
                  <>
                    <option value="gemini-2.5-flash">gemini-2.5-flash</option>
                    <option value="gemini-1.5-flash">gemini-1.5-flash</option>
                    <option value="gemini-1.5-pro">gemini-1.5-pro</option>
                  </>
                ) : (
                  <>
                    <option value="claude-3-5-sonnet-20241022">claude-3-5-sonnet</option>
                    <option value="claude-3-5-haiku-20241022">claude-3-5-haiku</option>
                  </>
                )}
              </select>
            </label>
          </div>
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
