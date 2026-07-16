import { defineConfig } from "vite";

export default defineConfig({
  build: {
    target: "esnext",
    emptyOutDir: false,
    rollupOptions: {
      input: {
        code: "src/code.ts",
      },
      output: {
        entryFileNames: "[name].js",
      },
    },
  },
});
