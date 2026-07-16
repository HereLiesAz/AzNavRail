import { defineConfig } from "vite";
import { viteSingleFile } from "vite-plugin-singlefile";

export default defineConfig({
  plugins: [viteSingleFile()],
  build: {
    target: "esnext",
    assetsInlineLimit: 100000000,
    chunkSizeWarningLimit: 100000000,
    cssCodeSplit: false,
    brotliSize: false,
    emptyOutDir: false,
    rollupOptions: {
      input: {
        ui: "src/ui.html",
        code: "src/code.ts",
      },
      output: {
        entryFileNames: "[name].js",
      },
    },
  },
});
