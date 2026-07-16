# AzNavRail Figma UI Kit Exporter

This tool spins up a local React app that renders all the primary `AzNavRail` UI components side-by-side. You can then use Figma plugins to instantly snapshot the page and convert it into native Figma layers, giving you a perfect 1:1 UI Kit.

## How to use:

1. Open your terminal and navigate to the `figma-export` directory:
   ```bash
   cd figma-export
   ```

2. Install the dependencies:
   ```bash
   npm install
   ```

3. Start the local server:
   ```bash
   npm run dev
   ```

4. Open the provided localhost URL (usually `http://localhost:5173`) in your browser. You will see a gallery of all the components.

5. Open **Figma**.

6. Install and run the **"html.to.design"** or **"Figma to HTML, CSS, React & more!" (by Builder.io)** plugin.

7. Paste your localhost URL (e.g. `http://localhost:5173`) into the plugin and click Import.

8. The plugin will automatically parse the DOM and CSS from the React page, generating native Figma Auto Layout frames, text layers, and vectors for every AzNavRail component!