import React, { useState } from "react";
import YAML from "yaml";
import ConfigTab from "./tabs/ConfigTab.jsx";
import MobTab from "./tabs/MobTab.jsx";
import ItemTab from "./tabs/ItemTab.jsx";
import RuneTab from "./tabs/RuneTab.jsx";
import DatabaseTab from "./tabs/DatabaseTab.jsx";

const TABS = [
  { id: "config", label: "⚙️ 설정 관리자", Component: ConfigTab },
  { id: "database", label: "🗄️ 데이터베이스", Component: DatabaseTab },
  { id: "mob", label: "👹 몹 생성기", Component: MobTab },
  { id: "item", label: "⚔️ 아이템 생성기", Component: ItemTab },
  { id: "rune", label: "💎 룬 생성기", Component: RuneTab },
];

export default function App() {
  const [activeTab, setActiveTab] = useState("config");
  
  // States for prefilled configurations loaded from the server
  const [loadedMob, setLoadedMob] = useState(null);
  const [loadedItem, setLoadedItem] = useState(null);
  const [loadedRune, setLoadedRune] = useState(null);

  const handleEditVisually = (category, yamlText, id) => {
    try {
      const parsed = YAML.parse(yamlText);
      parsed.id = parsed.id || id;
      
      if (category === "mob") {
        setLoadedMob(parsed);
        setActiveTab("mob");
      } else if (category === "item") {
        setLoadedItem(parsed);
        setActiveTab("item");
      } else if (category === "rune") {
        setLoadedRune(parsed);
        setActiveTab("rune");
      }
    } catch (err) {
      alert(`YAML 파싱 중 오류가 발생했습니다: ${err.message}`);
    }
  };

  return (
    <div className="app">
      <header>
        <h1>🌊 The Tide v2 — Web Customizer</h1>
        <nav>
          {TABS.map((tab) => (
            <button
              key={tab.id}
              className={tab.id === activeTab ? "tab active" : "tab"}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </header>
      <main>
        {activeTab === "config" && <ConfigTab onEditVisually={handleEditVisually} />}
        {activeTab === "database" && <DatabaseTab />}
        {activeTab === "mob" && <MobTab loadedConfig={loadedMob} onClearLoaded={() => setLoadedMob(null)} />}
        {activeTab === "item" && <ItemTab loadedConfig={loadedItem} onClearLoaded={() => setLoadedItem(null)} />}
        {activeTab === "rune" && <RuneTab loadedConfig={loadedRune} onClearLoaded={() => setLoadedRune(null)} />}
      </main>
    </div>
  );
}
