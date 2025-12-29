import React, { useState, useEffect, useRef, useMemo } from 'react';
import './AzNavRail.css';
import MenuItem from './MenuItem';
import AzNavRailButton from './AzNavRailButton';
import HelpOverlay from './HelpOverlay';

/**
 * An M3-style navigation rail that expands into a menu drawer for web applications.
 *
 * @param {object} props - The component props.
 * @param {boolean} [props.initiallyExpanded=false] - Whether the navigation rail is expanded by default.
 * @param {Array<object>} props.content - An array of navigation item objects (tree structure).
 * @param {object} [props.settings={}] - An object containing settings.
 */
const AzNavRail = ({
  initiallyExpanded = false,
  // disableSwipeToOpen,
  content,
  settings = {}
}) => {
  const [isExpanded, setIsExpanded] = useState(initiallyExpanded);
  const {
    displayAppNameInHeader = false,
    expandedRailWidth = '260px',
    collapsedRailWidth = '80px',
    showFooter = true,
    isLoading = false,
    appName = 'App',
    infoScreen = false,
    onDismissInfoScreen
  } = settings;

  const onToggle = () => {
      if (!infoScreen) setIsExpanded(!isExpanded);
  };

  const [cyclerStates, setCyclerStates] = useState({});
  const [hostStates, setHostStates] = useState({});
  const cyclerTimers = useRef({});

  const navItems = useMemo(() => content || [], [content]);

  // Flatten items for cycler initialization (simple recursive helper could be used if needed deep)
  useEffect(() => {
    const initialCyclerStates = {};
    const processItem = (item) => {
        if (item.isCycler) {
            initialCyclerStates[item.id] = {
                displayedOption: item.selectedOption || ''
            };
        }
        if (item.items) {
            item.items.forEach(processItem);
        }
    };
    navItems.forEach(processItem);
    setCyclerStates(initialCyclerStates);

    return () => {
      Object.values(cyclerTimers.current).forEach(clearTimeout);
    };
  }, [navItems]);

  const handleCyclerClick = (item) => {
    if (cyclerTimers.current[item.id]) {
      clearTimeout(cyclerTimers.current[item.id]);
    }

    const { options } = item;
    const currentOption = cyclerStates[item.id]?.displayedOption || item.selectedOption;
    const currentIndex = options.indexOf(currentOption);
    const nextIndex = (currentIndex + 1) % options.length;
    const nextOption = options[nextIndex];

    setCyclerStates(prev => ({
      ...prev,
      [item.id]: { ...prev[item.id], displayedOption: nextOption }
    }));

    cyclerTimers.current[item.id] = setTimeout(() => {
      if (item.onClick) item.onClick(nextOption);
      delete cyclerTimers.current[item.id];
    }, 1000);
  };

  const toggleHost = (item) => {
      setHostStates(prev => ({ ...prev, [item.id]: !prev[item.id] }));
  };

  const renderMenuItem = (item, depth = 0) => {
      const finalItem = item.isCycler
        ? { ...item, selectedOption: cyclerStates[item.id]?.displayedOption }
        : item;

      const isHost = item.items && item.items.length > 0;
      const isHostExpanded = hostStates[item.id];

      return (
          <React.Fragment key={item.id}>
              <MenuItem
                  item={finalItem}
                  depth={depth}
                  onToggle={onToggle}
                  onCyclerClick={() => handleCyclerClick(item)}
                  isHost={isHost}
                  isExpanded={isHostExpanded}
                  onHostClick={() => toggleHost(item)}
                  infoScreen={infoScreen}
              />
              {isHost && isHostExpanded && (
                  <div className="az-nav-rail-subitems">
                      {item.items.map(subItem => renderMenuItem(subItem, depth + 1))}
                  </div>
              )}
          </React.Fragment>
      );
  };

  // Prepare flat list of currently visible rail items for HelpOverlay
  const getVisibleItems = () => {
    const visible = [];
    navItems.forEach(item => {
        if (item.isRailItem || item.items) { // Top level rail items or hosts
            visible.push(item);
            if (item.items && hostStates[item.id]) {
               // If host is expanded, sub items are visible?
               // Wait, sub items are only shown in Menu mode?
               // "Rail Sub Item" - does it appear in rail?
               // Android: `azRailSubItem` appears in both.
               // In this simplified web version, we might not render sub items in collapsed rail unless logic supports it.
               // Looking at render loop below:
               // .map(item => ... AzNavRailButton ...)
               // It maps top level items.
               // If item has `items` (isHost), we render a button for it.
               // If we click it, it expands rail.
               // So in collapsed mode, we don't see subitems.
               // But Help Mode description says: "And when sub items are exposed, they too should have their descriptions shown and be pointed at."
               // Sub items are exposed when Host is expanded.
               // If Host is expanded, does the Rail expand?
               // In Android: "And when sub items are exposed... Host items should still expand and collapse their sub items."
               // In Android `RailItems` composable, we iterate subItems if host is expanded.

               // So we need to support rendering sub-items in the Rail if host is expanded.
               item.items.forEach(sub => {
                   if (sub.isRailItem) visible.push(sub);
               });
            }
        }
    });
    return visible;
  };

  const visibleItems = getVisibleItems();

  return (
    <>
    <div
      className={`az-nav-rail ${isExpanded ? 'expanded' : 'collapsed'}`}
      style={{ width: isExpanded ? expandedRailWidth : collapsedRailWidth }}
    >
      <div className="header" onClick={onToggle}>
        {displayAppNameInHeader ? (
          <span>{appName}</span>
        ) : (
          <img src="/app-icon.png" alt="App Icon" className="app-icon" />
        )}
      </div>

      {isLoading ? (
        <div className="loading-spinner">Loading...</div>
      ) : (
        <div className="content">
          {isExpanded ? (
            <div className="menu">
              {navItems.map(item => renderMenuItem(item))}
            </div>
          ) : (
            <div className="rail" style={{overflowY: 'auto', maxHeight: '100%'}}>
              {navItems
                .filter(item => item.isRailItem || item.items)
                .map(item => {
                  const finalItem = item.isCycler
                    ? { ...item, selectedOption: cyclerStates[item.id]?.displayedOption }
                    : item;

                  // If host is expanded, we should render its sub-items too?
                  // Currently the loop just renders top-level.
                  // We need to inject sub-items if expanded.

                  return (
                    <React.Fragment key={item.id}>
                        <AzNavRailButton
                            item={finalItem}
                            onCyclerClick={() => handleCyclerClick(item)}
                            onClickOverride={item.items ? () => {
                                // If infoScreen, just toggle state locally without expanding rail
                                if (infoScreen) {
                                    toggleHost(item);
                                } else {
                                    setIsExpanded(true);
                                    setHostStates(prev => ({ ...prev, [item.id]: true }));
                                }
                            } : undefined}
                            infoScreen={infoScreen}
                        />
                        {item.items && hostStates[item.id] && (
                            item.items.filter(sub => sub.isRailItem).map(sub => (
                                <AzNavRailButton
                                    key={sub.id}
                                    item={sub}
                                    onCyclerClick={() => handleCyclerClick(sub)}
                                    infoScreen={infoScreen}
                                />
                            ))
                        )}
                    </React.Fragment>
                  );
                })}
            </div>
          )}
        </div>
      )}

      {showFooter && isExpanded && (
        <div className="footer">
             <div style={{padding: '16px', fontSize: '12px', opacity: 0.5}}>
                 Footer
             </div>
        </div>
      )}
    </div>

    {infoScreen && (
        <HelpOverlay
            items={visibleItems}
            railWidth={collapsedRailWidth}
            onDismiss={onDismissInfoScreen}
        />
    )}
    </>
  );
};

export default AzNavRail;
