import { fileURLToPath } from 'node:url'
import { createRequire } from 'node:module'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Base path for GitHub Pages: served from https://HereLiesAz.github.io/AzNavRail/.
// Override with VITE_BASE=/ for local serves that should sit at the root.
const base = process.env.VITE_BASE ?? '/AzNavRail/'

// Resolve `react-native-web` to an absolute path so the Rolldown/Vite alias plugin doesn't
// rewrite `react-native` → `react-native-web` as a bare specifier (which the build resolver
// then can't find when consuming the `aznavrail-react` lib output from `lib/module/...`).
const require = createRequire(import.meta.url)
const reactNativeWebEntry = require.resolve('react-native-web', {
  paths: [fileURLToPath(new URL('.', import.meta.url))],
})

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base,
  resolve: {
    alias: {
      // Absolute-path alias so library `react-native` imports resolve in the browser bundle.
      'react-native': reactNativeWebEntry,
    },
    dedupe: ['react', 'react-dom', 'react-native-web'],
  },
})
