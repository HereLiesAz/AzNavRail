/**
 * Placeholder screen. The legacy playground content lives entirely on the rail itself —
 * navigate via the rail items to see toggles, cyclers, host items, reloc items, and nested rails
 * in action. This screen exists so the showcase home has a target route.
 */
export default function LegacyPlayground() {
  return (
    <div style={{ padding: 24, maxWidth: 720 }}>
      <h2 style={{ margin: 0 }}>Rail Playground</h2>
      <p style={{ opacity: 0.8 }}>
        The rail items on the left drive every legacy demo. Try the following:
      </p>
      <ul style={{ lineHeight: 1.7 }}>
        <li>Toggle <strong>Pack Rail</strong>, <strong>Online</strong>, <strong>Dark Mode</strong> — top of the rail.</li>
        <li>Cycle the <strong>Rail Cycler</strong> (option C is disabled) and the menu cycler.</li>
        <li>Open <strong>Menu Host</strong> or <strong>Rail Host</strong> to reveal sub-items, sub-toggle and sub-cycler.</li>
        <li>Long-press <strong>Reloc Item 1</strong> or <strong>Reloc + Nested</strong> to access the hidden menu.</li>
        <li>Tap <strong>Vertical Nested</strong> / <strong>Horizontal Nested</strong> to pop a nested rail.</li>
        <li>Hit <strong>Help</strong> for the connecting-line overlay.</li>
      </ul>
    </div>
  )
}
