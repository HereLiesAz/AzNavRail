const fs = require('fs');

let file = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');

const autoHelpOld = `  const effectiveRailItems = useMemo(() => {
    const list = [];
    items.forEach(i => {
      if (!i.isSubItem && (config.noMenu || i.isRailItem)) {
          list.push(i);
          if (hostStates[i.id] && !i.isNestedRail) {
              const sub = subItemsMap[i.id] || [];
              sub.forEach(s => list.push(s));
          }
      }
    });
    return list;
  }, [items, config.noMenu, hostStates, subItemsMap]);`;

const autoHelpNew = `  const effectiveRailItems = useMemo(() => {
    const list: AzNavItem[] = [];
    items.forEach(i => {
      if (!i.isSubItem && (config.noMenu || i.isRailItem)) {
          list.push(i);
          if (hostStates[i.id] && !i.isNestedRail) {
              const sub = subItemsMap[i.id] || [];
              sub.forEach(s => list.push(s));
          }
      }
    });

    const hasExplicitHelpItem = items.some(i => i.isHelpItem);
    if (config.helpEnabled && !hasExplicitHelpItem) {
        list.push({
            id: 'auto-help',
            isRailItem: true,
            isHelpItem: true,
            text: 'Help',
            shape: AzButtonShape.CIRCLE,
            isToggle: false,
            isCycler: false,
            isDivider: false,
            collapseOnClick: true,
            disabled: false,
            isHost: false,
            isSubItem: false,
            isExpanded: false,
            toggleOnText: '',
            toggleOffText: ''
        });
    }

    return list;
  }, [items, config.noMenu, hostStates, subItemsMap, config.helpEnabled]);`;

file = file.replace(autoHelpOld, autoHelpNew);
fs.writeFileSync('aznavrail-react/src/AzNavRail.tsx', file);
