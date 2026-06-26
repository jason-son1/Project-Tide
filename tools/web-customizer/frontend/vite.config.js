import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: "../../tide-core/src/main/resources/web",
    emptyOutDir: true
  },
  server: {
    port: 5173,
  },
});

