with open('./aznavrail-web/src/components/MenuItem.jsx', 'r') as f:
    content = f.read()

replacement_aria = """  const ariaProps = {};
  if (isToggle) {
    ariaProps['aria-checked'] = isChecked;
    ariaProps.role = 'switch';
  } else if (isHost) {
    ariaProps['aria-expanded'] = isExpanded;
  } else if (isCycler) {
    ariaProps['aria-label'] = `${text} ${selectedOption}`;
  } else if (item.isNestedRail) {
    ariaProps['aria-expanded'] = isExpanded;
    ariaProps['aria-haspopup'] = 'true';
  }

  return (
    <div
        className="az-menu-item"
        style={{ color: color, paddingLeft: `${paddingLeft}px`, position: 'relative' }}
        onClick={handleClick}
        data-az-nav-id={item.id}
        {...ariaProps}
    >"""

content = content.replace("""  return (
    <div
        className="az-menu-item"
        style={{ color: color, paddingLeft: `${paddingLeft}px`, position: 'relative' }}
        onClick={handleClick}
        data-az-nav-id={item.id}
    >""", replacement_aria)

with open('./aznavrail-web/src/components/MenuItem.jsx', 'w') as f:
    f.write(content)
