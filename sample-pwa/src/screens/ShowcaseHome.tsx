import { useNavigate } from 'react-router-dom'

const entries: { route: string; title: string; blurb: string }[] = [
  { route: '/legacy', title: 'Rail Playground', blurb: 'Toggles, cyclers, nested rails, reloc items, host items.' },
  { route: '/bottom-sheet', title: 'Bottom Sheets', blurb: 'AzBottomSheet + AzSheetController with all four detents.' },
  { route: '/standalone', title: 'Standalone Widgets', blurb: 'AzButton/Toggle/Cycler at every shape, AzLoad, AzDivider, AzRoller.' },
  { route: '/customization', title: 'Theming Customization', blurb: 'Live header shape, default shape, rail widths, footer, classifiers.' },
  { route: '/forms', title: 'Forms & Text Inputs', blurb: 'AzForm + AzTextBox showcase.' },
  { route: '/tutorial', title: 'Interactive Tutorials', blurb: 'Scripted scenes with every AzAdvanceCondition and AzHighlight.' },
  { route: '/help-system', title: 'Help System', blurb: 'screenTitle, info, classifiers, helpList; rail-scoped help cards.' },
]

export default function ShowcaseHome({ railExpanded = false, hostExpanded = {} }: { railExpanded?: boolean; hostExpanded?: Record<string, boolean> }) {
  const navigate = useNavigate()
  return (
    <div style={page}>
      <h1 style={{ margin: 0 }}>AzNavRail React Showcase</h1>
      <p style={{ marginTop: 8, opacity: 0.8 }}>
        Each card jumps to a demo that exercises a different slice of the React port. The rail items on the
        left of the screen drive global state (dock side, dark mode, packing, classifiers, etc.).
      </p>
      <div style={statusChip}>
        <span>Rail state (onExpandedChange)</span>
        <strong>{railExpanded ? 'Expanded' : 'Collapsed'}</strong>
      </div>
      {(['menu-host', 'rail-host'] as const).map((id) => (
        <div key={id} style={hostChip}>
          <span>{id} (onExpandedChange)</span>
          <strong>{hostExpanded[id] ? 'Expanded' : 'Collapsed'}</strong>
        </div>
      ))}
      <div style={{ display: 'grid', gap: 12, marginTop: 16 }}>
        {entries.map((e) => (
          <button key={e.route} onClick={() => navigate(e.route)} style={card}>
            <div style={{ fontWeight: 600 }}>{e.title}</div>
            <div style={{ opacity: 0.75, fontSize: 13 }}>{e.blurb}</div>
          </button>
        ))}
      </div>
    </div>
  )
}

const page: React.CSSProperties = {
  padding: 24,
  maxWidth: 720,
  margin: '0 auto',
  color: 'inherit',
}

const card: React.CSSProperties = {
  textAlign: 'left',
  background: 'rgba(255,255,255,0.06)',
  border: '1px solid rgba(255,255,255,0.12)',
  borderRadius: 12,
  padding: 16,
  cursor: 'pointer',
  color: 'inherit',
  font: 'inherit',
}

const hostChip: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  marginTop: 8,
  padding: '8px 12px',
  borderRadius: 8,
  background: 'rgba(0,150,136,0.12)',
  border: '1px solid rgba(0,150,136,0.3)',
  fontSize: 13,
}

const statusChip: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  marginTop: 12,
  padding: '8px 12px',
  borderRadius: 8,
  background: 'rgba(98,0,238,0.12)',
  border: '1px solid rgba(98,0,238,0.3)',
  fontSize: 13,
}
