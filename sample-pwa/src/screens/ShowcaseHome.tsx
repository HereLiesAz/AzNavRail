import { useNavigate } from 'react-router-dom'
import { useState } from 'react'

const entries: { route: string; title: string; blurb: string }[] = [
  { route: '/legacy', title: 'Rail Playground', blurb: 'Toggles, cyclers, nested rails, reloc items, host items.' },
  { route: '/bottom-sheet', title: 'Bottom Sheets', blurb: 'AzBottomSheet + AzSheetController with all four detents.' },
  { route: '/standalone', title: 'Standalone Widgets', blurb: 'AzButton/Toggle/Cycler at every shape, AzLoad, AzDivider, AzRoller.' },
  { route: '/customization', title: 'Theming Customization', blurb: 'Live header shape, default shape, rail widths, footer, classifiers.' },
  { route: '/forms', title: 'Forms & Text Inputs', blurb: 'AzForm + AzTextBox showcase.' },
  { route: '/tutorial', title: 'Status-Driven Guidance', blurb: 'AzStatus / AzEdge / AzGoal — activate goals and watch callouts route live.' },
  { route: '/help-system', title: 'Help System', blurb: 'screenTitle, info, classifiers, helpList; rail-scoped help cards.' },
]

export default function ShowcaseHome({ railExpanded = false, hostExpanded = {} }: { railExpanded?: boolean; hostExpanded?: Record<string, boolean> }) {
  const navigate = useNavigate()
  return (
    <div style={page}>
      <header style={heroSection}>
        <h1 style={heroTitle}>AzNavRail</h1>
        <p style={heroSubtitle}>
          Material Expressive components meeting Skeuomorphic depth.
        </p>
      </header>
      
      <div style={statusContainer}>
        <div style={statusChip}>
          <span>Rail state</span>
          <strong>{railExpanded ? 'Expanded' : 'Collapsed'}</strong>
        </div>
        {(['menu-host', 'rail-host'] as const).map((id) => (
          <div key={id} style={hostChip}>
            <span>{id}</span>
            <strong>{hostExpanded[id] ? 'Expanded' : 'Collapsed'}</strong>
          </div>
        ))}
      </div>

      <div style={grid}>
        {entries.map((e) => (
          <InteractiveCard key={e.route} entry={e} onClick={() => navigate(e.route)} />
        ))}
      </div>
    </div>
  )
}

function InteractiveCard({ entry, onClick }: { entry: any, onClick: () => void }) {
  const [isHovered, setIsHovered] = useState(false);
  const [isPressed, setIsPressed] = useState(false);

  return (
    <button
      onClick={onClick}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => { setIsHovered(false); setIsPressed(false); }}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      style={{
        ...cardBase,
        ...(isHovered ? cardHovered : {}),
        ...(isPressed ? cardPressed : {}),
      }}
    >
      <div style={cardContent}>
        <div style={cardTitle}>{entry.title}</div>
        <div style={cardBlurb}>{entry.blurb}</div>
      </div>
    </button>
  )
}

const page: React.CSSProperties = {
  padding: '40px 24px',
  maxWidth: 1000,
  margin: '0 auto',
  color: 'inherit',
  display: 'flex',
  flexDirection: 'column',
  gap: 32,
}

const heroSection: React.CSSProperties = {
  textAlign: 'center',
  padding: '60px 20px',
  background: 'var(--glass-bg)',
  borderRadius: 24,
  boxShadow: 'var(--shadow-glass)',
  border: 'var(--glass-border)',
  backdropFilter: 'blur(12px)',
  WebkitBackdropFilter: 'blur(12px)',
  marginBottom: 20,
}

const heroTitle: React.CSSProperties = {
  margin: 0,
  fontSize: '4rem',
  fontWeight: 800,
  background: 'linear-gradient(135deg, var(--accent) 0%, #ff6b6b 100%)',
  WebkitBackgroundClip: 'text',
  WebkitTextFillColor: 'transparent',
  letterSpacing: '-2px',
}

const heroSubtitle: React.CSSProperties = {
  marginTop: 16,
  fontSize: '1.25rem',
  opacity: 0.85,
  fontWeight: 500,
}

const statusContainer: React.CSSProperties = {
  display: 'flex',
  gap: 16,
  flexWrap: 'wrap',
  justifyContent: 'center',
}

const chipBase: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  padding: '12px 20px',
  borderRadius: 16,
  fontSize: 14,
  fontWeight: 600,
  gap: 12,
  boxShadow: 'var(--shadow-raised)',
  background: 'var(--bg)',
  transition: 'transform var(--dur-default-spatial) var(--ease-spring-default-spatial)',
}

const statusChip: React.CSSProperties = {
  ...chipBase,
  border: '1px solid rgba(98,0,238,0.2)',
  color: 'var(--accent)',
}

const hostChip: React.CSSProperties = {
  ...chipBase,
  border: '1px solid rgba(0,150,136,0.2)',
  color: '#009688',
}

const grid: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
  gap: 24,
}

const cardBase: React.CSSProperties = {
  textAlign: 'left',
  background: 'var(--bg)',
  borderRadius: 24,
  padding: 24,
  cursor: 'pointer',
  color: 'inherit',
  font: 'inherit',
  border: 'none',
  outline: 'none',
  boxShadow: 'var(--shadow-raised)',
  transition: 'all var(--dur-fast-spatial) var(--ease-spring-fast-spatial)',
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'space-between',
  minHeight: 160,
  position: 'relative',
  overflow: 'hidden',
}

const cardHovered: React.CSSProperties = {
  transform: 'translateY(-4px) scale(1.02)',
  boxShadow: '12px 12px 24px rgba(163, 177, 198, 0.5), -12px -12px 24px rgba(255, 255, 255, 0.9)',
  // Note: For dark mode, shadow tokens will override appropriately if setup strictly. 
  // Given we use CSS vars in shadow-raised, let's just stick to scaling and token overrides if needed,
  // but a slight manual override here gives that extra 'pop'.
}

const cardPressed: React.CSSProperties = {
  transform: 'translateY(2px) scale(0.98)',
  boxShadow: 'var(--shadow-pressed)',
  background: 'var(--code-bg)',
}

const cardContent: React.CSSProperties = {
  position: 'relative',
  zIndex: 1,
}

const cardTitle: React.CSSProperties = {
  fontWeight: 700,
  fontSize: '1.2rem',
  marginBottom: 8,
  color: 'var(--text-h)',
}

const cardBlurb: React.CSSProperties = {
  opacity: 0.8,
  fontSize: '0.95rem',
  lineHeight: 1.5,
}
