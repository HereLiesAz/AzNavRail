# sample-pwa

Vite + React + TypeScript showcase for the `@HereLiesAz/aznavrail-react` library. Deployed to
GitHub Pages at **https://HereLiesAz.github.io/AzNavRail/** by
`.github/workflows/jekyll-gh-pages.yml` (legacy filename retained for git history — it now builds
this Vite PWA, not Jekyll).

## Demos

- **Showcase Home** — index of every screen.
- **Bottom Sheets** — `AzBottomSheet` + `AzSheetController` with all four detents
  (HIDDEN / PEEK / HALF / FULL), live `AzSheetConfig` toggles, `onSwipeLeft`/`onSwipeRight`,
  and the `AzBottomSheetInsetAware` variant.
- **Standalone Widgets** — `AzButton`, `AzToggle`, `AzCycler` at every `AzButtonShape`,
  plus `AzRoller`, `AzDivider`, `AzLoad`.
- **Customization** — live header icon shape, default shape, rail widths, footer, repo URL,
  haptics, and `activeClassifiers` chips.
- **Forms** — `AzForm` + `AzTextBox` showcase.
- **Rail Playground** — toggles, cyclers, host items, reloc items with hidden menus, and both
  vertical/horizontal nested rails (each with its own Help item that scopes the help overlay).

## Local development

```bash
cd sample-pwa
npm install --legacy-peer-deps
npm run dev
```

The `file:../aznavrail-react` dependency means changes inside `aznavrail-react/src/` are picked up
on save. Run `npm install` again if you change `aznavrail-react/package.json`.

To preview the production bundle locally (at the root, not `/AzNavRail/`):

```bash
VITE_BASE=/ npm run build && npm run preview
```

CI sets `VITE_BASE=/AzNavRail/` automatically so the GitHub Pages deployment resolves assets under
the repo subpath.

## Deployment

Pushes to `main`/`master` that touch `sample-pwa/`, `aznavrail-react/`, or the workflow file
trigger a Pages deployment. The workflow:

1. Installs `aznavrail-react/` deps and runs its build (best-effort — the source is consumed
   directly via the `file:` dependency, so build failure does not block the PWA build).
2. Installs `sample-pwa/` deps.
3. Runs `npm run build` with `VITE_BASE=/AzNavRail/`.
4. Uploads `sample-pwa/dist` as the Pages artifact and deploys it.
