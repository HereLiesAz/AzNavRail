with open('./aznavrail-react/src/web/AzNavRail.jsx', 'r') as f:
    content = f.read()

replacement_footer = """      {showFooter && isExpanded && (
        <div className="footer" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '16px', color: activeColor || 'currentColor' }}>
             <div style={{ padding: '8px 0', fontWeight: 'bold' }}>{appName}</div>
             <div style={{ padding: '4px 0', fontSize: '10px' }}>About</div>
             <div style={{ padding: '4px 0', fontSize: '10px' }}>Feedback</div>
             <div style={{ padding: '4px 0', fontSize: '10px' }}>@HereLiesAz</div>
        </div>
      )}"""

content = content.replace("""      {showFooter && isExpanded && (
        <div className="footer">
             <div style={{padding: '16px', fontSize: '12px', opacity: 0.5}}>
                 {appName}
             </div>
        </div>
      )}""", replacement_footer)

with open('./aznavrail-react/src/web/AzNavRail.jsx', 'w') as f:
    f.write(content)
