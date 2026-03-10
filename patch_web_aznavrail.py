with open('./aznavrail-web/src/components/AzNavRail.jsx', 'r') as f:
    content = f.read()

# Add explicit surface color handling for expanded state
replacement_bg = """      className={`az-nav-rail ${dockingSide.toLowerCase()} ${isExpanded ? 'expanded' : ''}`}
      style={{
        width: isExpanded ? expandedRailWidth : collapsedRailWidth,
        position: 'fixed',
        top: 0,
        [dockingSide.toLowerCase()]: 0,
        backgroundColor: isExpanded ? 'var(--md-sys-color-surface, white)' : 'transparent',
        boxShadow: isExpanded ? '2px 0 5px rgba(0,0,0,0.1)' : 'none'
      }}"""

content = content.replace("""      className={`az-nav-rail ${dockingSide.toLowerCase()} ${isExpanded ? 'expanded' : ''}`}
      style={{
        width: isExpanded ? expandedRailWidth : collapsedRailWidth,
        position: 'fixed',
        top: 0,
        [dockingSide.toLowerCase()]: 0
      }}""", replacement_bg)

with open('./aznavrail-web/src/components/AzNavRail.jsx', 'w') as f:
    f.write(content)
