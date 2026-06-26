import React from "react";

export default function YamlPreviewPanel({ yamlText, fileId, fileType, onDeploy, deployStatus }) {
  function download() {
    const blob = new Blob([yamlText], { type: "text/yaml" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${fileId || "content"}.yml`;
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="preview-panel">
      <h3>YAML 미리보기</h3>
      <pre>{yamlText || "// 아직 생성된 내용이 없습니다."}</pre>
      {yamlText && (
        <div className="preview-actions">
          <button onClick={download}>⬇ 다운로드</button>
          <button onClick={onDeploy}>🚀 서버에 직접 주입</button>
        </div>
      )}
      {deployStatus && <p className="status">{deployStatus}</p>}
    </div>
  );
}
