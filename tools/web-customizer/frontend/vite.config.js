import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "url";

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: fileURLToPath(new URL("../../../tide-core/src/main/resources/web", import.meta.url)),
    emptyOutDir: false
  },
  server: {
    port: 5173,
  },
});

