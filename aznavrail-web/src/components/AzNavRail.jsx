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
      setIsExpanded(!isExpanded);
  };

  const [cyclerStates, setCyclerStates] = useState({});
  const [hostStates, setHostStates] = useState({});
  const cyclerTimers = useRef({});

  const navItems = useMemo(() => content || [], [content]);

  // Flatten items for cycler initialization (simple recursive helper could be used if needed deep)
  // For now, assuming only top-level or 1-level deep cyclers for simplicity or checking both.
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
      // onToggle(); // Standard cyclers might not collapse menu, but AzNavRail logic says they generally do?
      // Memory says: "The standalone AzCycler component has the same 1-second delayed action behavior..."
      // Memory also says: "Toggle items in the expanded menu should collapse the menu when tapped."
      // But Cyclers? Maybe not. Let's keep it open to see the change.
      // Actually, if it triggers navigation, it might close. I'll leave onToggle out for cycler for now.
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

  return (
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
            <div className="rail">
              {navItems
                .filter(item => item.isRailItem || item.items) // Show rail items or hosts
                .map(item => {
                  const finalItem = item.isCycler
                    ? { ...item, selectedOption: cyclerStates[item.id]?.displayedOption }
                    : item;

                  return (
                    <AzNavRailButton
                        key={item.id}
                        item={finalItem}
                        onCyclerClick={() => handleCyclerClick(item)}
                        onClickOverride={item.items ? () => {
                            setIsExpanded(true);
                            setHostStates(prev => ({ ...prev, [item.id]: true }));
                        } : undefined}
                        infoScreen={infoScreen}
                    />
                  );
                })}
            </div>
          )}
        </div>
      )}

      {showFooter && isExpanded && (
        <div className="footer">
             {/* Footer placeholder */}
             <div style={{padding: '16px', fontSize: '12px', opacity: 0.5}}>
                 Footer
             </div>
        </div>
      )}

      {infoScreen && (
        <>
          <HelpOverlay items={navItems} />
          <button
            className="az-fab-exit"
            onClick={onDismissInfoScreen}
            style={{
                position: 'absolute',
                bottom: '16px',
                right: '16px',
                width: '56px',
                height: '56px',
                borderRadius: '50%',
                backgroundColor: 'var(--md-sys-color-primary, #6200ee)',
                color: 'white',
                border: 'none',
                boxShadow: '0 4px 8px rgba(0,0,0,0.2)',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 10000,
                fontSize: '24px'
            }}
          >
              ✕
          </button>
        </>
      )}
    </div>
  );
};

export default AzNavRail;
