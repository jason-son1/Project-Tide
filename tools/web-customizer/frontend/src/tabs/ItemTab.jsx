import React, { useState, useEffect } from "react";
import { generateContent, deployContent } from "../api.js";
import YamlPreviewPanel from "../components/YamlPreviewPanel.jsx";

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

  return (
    <div className="tab-grid">
      <div className="form-panel">
        <h2>아이템 생성기</h2>
        <label>아이템 ID<input value={id} onChange={(e) => setId(e.target.value)} placeholder="flame_sword_t2" /></label>
        <label>표시 이름<input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="§6[불꽃의 대검]" /></label>
        <label>재질(Material)<input value={material} onChange={(e) => setMaterial(e.target.value.toUpperCase())} /></label>
        <label>CustomModelData<input type="number" value={customModelData} onChange={(e) => setCustomModelData(e.target.value)} /></label>

        <label>GS: {gearScore}
          <input type="range" min="100" max="1500" step="10" value={gearScore} onChange={(e) => setGearScore(e.target.value)} />
        </label>
        <label>티어(1~5)<input type="number" min="1" max="5" value={tier} onChange={(e) => setTier(e.target.value)} /></label>
        <label>데미지<input type="number" value={damage} onChange={(e) => setDamage(e.target.value)} /></label>
        <label>방어력<input type="number" value={defense} onChange={(e) => setDefense(e.target.value)} /></label>
        <label>강화당 데미지 보너스<input type="number" value={damagePerStar} onChange={(e) => setDamagePerStar(e.target.value)} /></label>
        <label>소켓 수(0~3)<input type="number" min="0" max="3" value={socketCount} onChange={(e) => setSocketCount(e.target.value)} /></label>
        <label>판매가(조개)<input type="number" value={sellPrice} onChange={(e) => setSellPrice(e.target.value)} /></label>

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
            placeholder="공격력이 높은 T2 네더라이트 대검, 소켓 2개" />
        </label>

        <button className="primary" onClick={handleGenerate}>YAML 미리보기 생성</button>
      </div>

      <YamlPreviewPanel yamlText={yamlText} fileId={id} fileType="item" onDeploy={handleDeploy} deployStatus={deployStatus} />
    </div>
  );
}
