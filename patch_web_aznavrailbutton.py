with open('./aznavrail-web/src/components/AzNavRailButton.jsx', 'r') as f:
    content = f.read()

replacement_aria = """  const ariaProps = {};
  if (isToggle) {
    ariaProps['aria-checked'] = isChecked;
    ariaProps.role = 'switch';
  } else if (item.isHost) {
    ariaProps['aria-expanded'] = item.isExpanded;
  } else if (isCycler) {
    ariaProps['aria-label'] = `${text} ${selectedOption}`;
  } else if (item.isNestedRail) {
    ariaProps['aria-expanded'] = item.isExpanded;
    ariaProps['aria-haspopup'] = 'true';
  }

  const shapeClass = item.shape ? item.shape.toLowerCase() : 'circle';

  return (
    <div style={{ position: 'relative' }} data-az-nav-id={id}>
        <button
            className={`az-nav-rail-button ${shapeClass} ${!isInteractive ? 'disabled' : ''}`}
            onClick={handleClick}
            style={{
                borderColor: color || 'blue',
                opacity: isInteractive ? 1 : 0.5,
                cursor: isInteractive ? 'pointer' : 'default',
                ...style
            }}
            disabled={!isInteractive}
            {...ariaProps}
        >"""

content = content.replace("""  return (
    <div style={{ position: 'relative' }} data-az-nav-id={id}>
        <button
            className={`az-nav-rail-button ${!isInteractive ? 'disabled' : ''}`}
            onClick={handleClick}
            style={{
                borderColor: color || 'blue',
                opacity: isInteractive ? 1 : 0.5,
                cursor: isInteractive ? 'pointer' : 'default',
                ...style
            }}
            disabled={!isInteractive}
        >""", replacement_aria)

with open('./aznavrail-web/src/components/AzNavRailButton.jsx', 'w') as f:
    f.write(content)
