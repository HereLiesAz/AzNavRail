import React, { useState, useEffect, useRef } from 'react';
import './AzNavRail.css';
import MenuItem from './MenuItem';
import AzNavRailButton from './AzNavRailButton';

const AzNavRail = ({
  initiallyExpanded = false,
  disableSwipeToOpen = false,
  content,
  settings = {}
}) => {
  const [isExpanded, setIsExpanded] = useState(initiallyExpanded);
  const {
    displayAppNameInHeader = false,
    packRailButtons = false,
    expandedRailWidth = '260px',
    collapsedRailWidth = '80px',
    showFooter = true,
    isLoading = false,
    appName = 'App'
  } = settings;

  const onToggle = () => setIsExpanded(!isExpanded);

  const [cyclerStates, setCyclerStates] = useState({});
  const cyclerTimers = useRef({});

  const navItems = content || [];

  useEffect(() => {
    const initialCyclerStates = {};
    navItems.forEach(item => {
      if (item.isCycler) {
        initialCyclerStates[item.id] = {
          displayedOption: item.selectedOption || ''
        };
      }
    });
    setCyclerStates(initialCyclerStates);

    return () => {
      // Clear all timers on unmount
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
      item.onClick(nextOption); // Pass the selected option to the handler
      onToggle();
      delete cyclerTimers.current[item.id];
    }, 1000);
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
          <img src="/app-icon.png" alt="App Icon" /> // Placeholder for app icon
        )}
      </div>

      {isLoading ? (
        <div className="loading-spinner">Loading...</div>
      ) : (
        <div className="content">
          {isExpanded ? (
            <div className="menu">
              {navItems.map(item => {
                const finalItem = item.isCycler
                  ? { ...item, selectedOption: cyclerStates[item.id]?.displayedOption }
                  : item;

                return (
                  <MenuItem
                    key={item.id}
                    item={finalItem}
                    onToggle={onToggle}
                    onCyclerClick={() => handleCyclerClick(item)}
                  />
                );
              })}
            </div>
          ) : (
            <div className="rail">
              {navItems
                .filter(item => item.isRailItem)
                .map(item => {
                  const finalItem = item.isCycler
                    ? { ...item, selectedOption: cyclerStates[item.id]?.displayedOption }
                    : item;
                  return <AzNavRailButton key={item.id} item={finalItem} onCyclerClick={() => handleCyclerClick(item)} />;
                })}
            </div>
          )}
        </div>
      )}

      {showFooter && isExpanded && (
        <div className="footer">
          {/* Footer content will be added here */}
        </div>
      )}
    </div>
  );
};

export default AzNavRail;