import {
  AzButtonShape,
  AzHeaderIconShape,
} from '@HereLiesAz/aznavrail-react'

export interface CustomizationState {
  headerIconShape: AzHeaderIconShape
  defaultShape: AzButtonShape
  expandedRailWidth: number
  collapsedRailWidth: number
  displayAppNameInHeader: boolean
  showFooter: boolean
  appRepositoryUrl: string
  vibrate: boolean
  activeClassifiers: Set<string>
}

const headerShapes = Object.values(AzHeaderIconShape) as AzHeaderIconShape[]
const buttonShapes = Object.values(AzButtonShape) as AzButtonShape[]
const repoChoices: { label: string; url: string }[] = [
  { label: 'AzNavRail', url: 'https://github.com/HereLiesAz/AzNavRail' },
  { label: 'Anthropic', url: 'https://github.com/anthropics' },
  { label: 'JetBrains Compose', url: 'https://github.com/JetBrains/compose-multiplatform' },
]

export const DEMO_CLASSIFIERS = ['focus', 'advanced', 'danger'] as const

export default function CustomizationDemo({
  state,
  onChange,
}: {
  state: CustomizationState
  onChange: (next: CustomizationState) => void
}) {
  return (
    <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
      <h2 style={{ margin: 0 }}>Theming Customization</h2>
      <p style={{ opacity: 0.8 }}>
        Every control below feeds back into the rail's settings. Watch the rail react as you change values.
      </p>

      <Block label={`headerIconShape: ${state.headerIconShape}`}>
        <select
          value={state.headerIconShape}
          onChange={(e) => onChange({ ...state, headerIconShape: e.target.value as AzHeaderIconShape })}
        >
          {headerShapes.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </Block>

      <Block label={`defaultShape: ${state.defaultShape}`}>
        <select
          value={state.defaultShape}
          onChange={(e) => onChange({ ...state, defaultShape: e.target.value as AzButtonShape })}
        >
          {buttonShapes.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </Block>

      <Block label={`expandedRailWidth: ${state.expandedRailWidth}px`}>
        <input
          type="range" min={160} max={360} value={state.expandedRailWidth}
          onChange={(e) => onChange({ ...state, expandedRailWidth: Number(e.target.value) })}
        />
      </Block>

      <Block label={`collapsedRailWidth: ${state.collapsedRailWidth}px`}>
        <input
          type="range" min={80} max={200} value={state.collapsedRailWidth}
          onChange={(e) => onChange({ ...state, collapsedRailWidth: Number(e.target.value) })}
        />
      </Block>

      <Toggle
        label="displayAppNameInHeader"
        value={state.displayAppNameInHeader}
        onChange={(v) => onChange({ ...state, displayAppNameInHeader: v })}
      />
      <Toggle
        label="showFooter"
        value={state.showFooter}
        onChange={(v) => onChange({ ...state, showFooter: v })}
      />
      <Toggle
        label="vibrate"
        value={state.vibrate}
        onChange={(v) => onChange({ ...state, vibrate: v })}
      />

      <Block label="appRepositoryUrl">
        <select
          value={state.appRepositoryUrl}
          onChange={(e) => onChange({ ...state, appRepositoryUrl: e.target.value })}
        >
          {repoChoices.map((r) => <option key={r.url} value={r.url}>{r.label}</option>)}
        </select>
      </Block>

      <Block label="activeClassifiers">
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {DEMO_CLASSIFIERS.map((c) => {
            const active = state.activeClassifiers.has(c)
            return (
              <button
                key={c}
                onClick={() => {
                  const next = new Set(state.activeClassifiers)
                  if (active) next.delete(c)
                  else next.add(c)
                  onChange({ ...state, activeClassifiers: next })
                }}
                style={{
                  ...chip,
                  background: active ? 'rgba(98,0,238,0.4)' : 'rgba(255,255,255,0.06)',
                }}
              >
                {c}
              </button>
            )
          })}
        </div>
      </Block>
    </div>
  )
}

function Block({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      <span style={{ fontWeight: 600 }}>{label}</span>
      {children}
    </label>
  )
}

function Toggle({
  label, value, onChange,
}: { label: string; value: boolean; onChange: (v: boolean) => void }) {
  return (
    <label style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
      <input type="checkbox" checked={value} onChange={(e) => onChange(e.target.checked)} />
      <span>{label}</span>
    </label>
  )
}

const chip: React.CSSProperties = {
  padding: '4px 10px',
  borderRadius: 999,
  border: '1px solid rgba(255,255,255,0.2)',
  color: 'inherit',
  cursor: 'pointer',
  font: 'inherit',
}
