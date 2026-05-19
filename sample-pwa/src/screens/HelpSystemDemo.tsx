import { DEMO_CLASSIFIERS } from './CustomizationDemo'

interface Props {
  showHelp: boolean
  onToggleHelp: () => void
  activeClassifiers: Set<string>
  onClassifiersChange: (next: Set<string>) => void
  dismissCount: number
}

/**
 * Mirrors the Android HelpSystemDemoScreen. The help overlay is internal state of AzNavRail —
 * there is no external API to open it, so the "open" affordance is the Help rail item itself.
 * This screen drives the auxiliary state (activeClassifiers, dismissCount log) and points the
 * user at the Help item on the rail.
 */
export default function HelpSystemDemo({
  showHelp, onToggleHelp, activeClassifiers, onClassifiersChange, dismissCount,
}: Props) {
  return (
    <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
      <h2 style={{ margin: 0 }}>Help System</h2>

      <div style={{
        background: 'rgba(255,255,255,0.06)',
        borderRadius: 8,
        padding: 12,
        border: '1px solid rgba(255,255,255,0.12)',
      }}>
        <strong>How to open the help overlay</strong>
        <p style={{ margin: '6px 0 0', fontSize: 13, opacity: 0.85 }}>
          Tap the <strong>Help</strong> rail item (icon-only when collapsed; labelled "Help" when expanded).
          Each rail item with a non-blank <code>info</code> renders a connecting-line card.
          The nested rails have their own scoped Help item — try those too.
        </p>
      </div>

      <p>onDismissInfoScreen fired: {dismissCount} times</p>

      <button onClick={onToggleHelp} style={btn}>
        {showHelp ? 'Hide help overlay' : 'Show help overlay'}
      </button>

      <section>
        <strong>activeClassifiers</strong>
        <p style={{ fontSize: 13, opacity: 0.75, marginTop: 4 }}>
          Items in App.tsx tag themselves with classifiers. Toggling a chip pushes the string into
          <code> activeClassifiers</code> and the rail highlights matching items programmatically.
        </p>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 8 }}>
          {DEMO_CLASSIFIERS.map((c) => {
            const active = activeClassifiers.has(c)
            return (
              <button
                key={c}
                onClick={() => {
                  const next = new Set(activeClassifiers)
                  if (active) next.delete(c)
                  else next.add(c)
                  onClassifiersChange(next)
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
          <button onClick={() => onClassifiersChange(new Set())} style={btn}>Clear all</button>
        </div>
      </section>
    </div>
  )
}

const btn: React.CSSProperties = {
  padding: '6px 12px',
  borderRadius: 6,
  border: '1px solid rgba(255,255,255,0.2)',
  background: 'rgba(255,255,255,0.08)',
  color: 'inherit',
  cursor: 'pointer',
  font: 'inherit',
  alignSelf: 'flex-start',
}

const chip: React.CSSProperties = {
  padding: '4px 10px',
  borderRadius: 999,
  border: '1px solid rgba(255,255,255,0.2)',
  color: 'inherit',
  cursor: 'pointer',
  font: 'inherit',
}
