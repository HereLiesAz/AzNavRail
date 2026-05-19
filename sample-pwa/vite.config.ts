import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Base path for GitHub Pages: served from https://herelieaz.github.io/AzNavRail/.
// Override with VITE_BASE=/ for local serves that should sit at the root.
const base = process.env.VITE_BASE ?? '/AzNavRail/'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base,
  resolve: {
    alias: {
      // react-native-web shim so library `react-native` imports resolve in the browser.
      'react-native': 'react-native-web',
    },
  },
})
