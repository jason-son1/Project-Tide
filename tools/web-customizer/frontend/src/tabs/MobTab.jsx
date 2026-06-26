import React, { useState, useEffect } from "react";
import { generateContent, deployContent } from "../api.js";
import YamlPreviewPanel from "../components/YamlPreviewPanel.jsx";

const BASE_MOBS = ["ZOMBIE", "SKELETON", "SPIDER", "CREEPER", "WITHER_SKELETON", "HUSK", "DROWNED"];
const AFFIXES = ["염화의", "신속의", "폭심의", "분열의", "강철의", "재생의", "가시의", "보호막의"];
const TIDE_STATES = ["HIGH_TIDE", "LOW_TIDE", "SPRING_TIDE", "BLOOD_MOON", "BLOOD_TIDE"];

export default function MobTab({ loadedConfig, onClearLoaded }) {
  const [id, setId] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [baseMob, setBaseMob] = useState(BASE_MOBS[0]);
  const [affixes, setAffixes] = useState([]);
  const [hpMultiplier, setHpMultiplier] = useState(1.5);
  const [damageMultiplier, setDamageMultiplier] = useState(1.5);
  const [movementSpeed, setMovementSpeed] = useState(0.23);
  const [tideStates, setTideStates] = useState(["LOW_TIDE"]);
  const [requestText, setRequestText] = useState("");
  const [yamlText, setYamlText] = useState("");
  const [deployStatus, setDeployStatus] = useState("");

  useEffect(() => {
    if (loadedConfig) {
      setId(loadedConfig.id || "");
      setDisplayName(loadedConfig.display_name || "");
      setBaseMob(loadedConfig.base_mob || BASE_MOBS[0]);
      setAffixes(loadedConfig.affixes || []);
      
      const stats = loadedConfig.stats || {};
      setHpMultiplier(stats.hp_multiplier || 1.5);
      setDamageMultiplier(stats.damage_multiplier || 1.5);
      setMovementSpeed(stats.movement_speed || 0.23);
      
      const spawn = loadedConfig.spawn || {};
      setTideStates(spawn.tide_states || ["LOW_TIDE"]);
      
      setYamlText("");
      setRequestText("");
      setDeployStatus("서버에서 설정을 불러왔습니다. 수정 후 미리보기를 생성하세요.");
      
      onClearLoaded();
    }
  }, [loadedConfig]);


  function toggle(list, setList, value) {
    setList(list.includes(value) ? list.filter((v) => v !== value) : [...list, value]);
  }

  async function handleGenerate() {
    setDeployStatus("");
    const payload = requestText.trim()
      ? { request_text: requestText.trim() }
      : {
          form_data: {
            id,
            display_name: displayName || `§f${id}`,
            base_mob: baseMob,
            stats: { hp_multiplier: Number(hpMultiplier), damage_multiplier: Number(damageMultiplier), movement_speed: Number(movementSpeed) },
            affixes,
            spawn: { worlds: ["world"], biomes: ["PLAINS"], tide_states: tideStates, weight: 10 },
            drops: [],
          },
        };
    try {
      const result = await generateContent("mob", payload);
      setYamlText(result.yaml);
    } catch (error) {
      setDeployStatus(`오류: ${error.message}`);
    }
  }

  async function handleDeploy() {
    try {
      const fileId = (yamlText.match(/^id:\s*(.+)$/m) || [, id])[1].trim();
      const result = await deployContent({ file_content: yamlText, file_type: "mob", file_id: fileId });
      setDeployStatus(`배포 완료: ${result.path} (reload: ${result.reload_target}, rcon: ${result.rcon_sent})`);
    } catch (error) {
      setDeployStatus(`배포 오류: ${error.message}`);
    }
  }

  return (
    <div className="tab-grid">
      <div className="form-panel">
        <h2>몹 생성기</h2>
        <label>몹 ID<input value={id} onChange={(e) => setId(e.target.value)} placeholder="zombie_염화의_t2" /></label>
        <label>표시 이름<input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="§c[염화의] §f좀비" /></label>
        <label>기반 몹
          <select value={baseMob} onChange={(e) => setBaseMob(e.target.value)}>
            {BASE_MOBS.map((m) => <option key={m} value={m}>{m}</option>)}
          </select>
        </label>

        <fieldset>
          <legend>접두사</legend>
          {AFFIXES.map((a) => (
            <label key={a} className="checkbox">
              <input type="checkbox" checked={affixes.includes(a)} onChange={() => toggle(affixes, setAffixes, a)} />{a}
            </label>
          ))}
        </fieldset>

        <label>HP 배율: {hpMultiplier}
          <input type="range" min="1.0" max="5.0" step="0.1" value={hpMultiplier} onChange={(e) => setHpMultiplier(e.target.value)} />
        </label>
        <label>데미지 배율: {damageMultiplier}
          <input type="range" min="1.0" max="4.0" step="0.1" value={damageMultiplier} onChange={(e) => setDamageMultiplier(e.target.value)} />
        </label>
        <label>이동속도: {movementSpeed}
          <input type="range" min="0.20" max="0.40" step="0.01" value={movementSpeed} onChange={(e) => setMovementSpeed(e.target.value)} />
        </label>

        <fieldset>
          <legend>스폰 조건 (조수 상태)</legend>
          {TIDE_STATES.map((s) => (
            <label key={s} className="checkbox">
              <input type="checkbox" checked={tideStates.includes(s)} onChange={() => toggle(tideStates, setTideStates, s)} />{s}
            </label>
          ))}
        </fieldset>

        <label>또는 자연어 요청 (입력 시 폼 대신 AI가 생성)
          <textarea value={requestText} onChange={(e) => setRequestText(e.target.value)}
            placeholder="공격력이 높고 피격 시 화염 장판을 까는 레벨 30 네더라이트 좀비" />
        </label>

        <button className="primary" onClick={handleGenerate}>YAML 미리보기 생성</button>
      </div>

      <YamlPreviewPanel yamlText={yamlText} fileId={id} fileType="mob" onDeploy={handleDeploy} deployStatus={deployStatus} />
    </div>
  );
}
