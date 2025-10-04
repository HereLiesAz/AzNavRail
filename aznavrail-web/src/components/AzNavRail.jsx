import React, { useState, useEffect, useRef } from 'react';
import './AzNavRail.css';
import MenuItem from './MenuItem';
import AzNavRailButton from './AzNavRailButton';

/**
 * An M3-style navigation rail that expands into a menu drawer for web applications.
 *
 * This component provides a vertical navigation rail that can be expanded to a full menu drawer.
 * It is designed to be highly configurable and supports standard, toggle, and cycler navigation items.
 *
 * @param {object} props - The component props.
 * @param {boolean} [props.initiallyExpanded=false] - Whether the navigation rail is expanded by default.
 * @param {boolean} [props.disableSwipeToOpen=false] - Whether to disable the swipe-to-open gesture (currently not implemented).
 * @param {Array<object>} props.content - An array of navigation item objects that define the content of the rail and menu.
 * @param {object} [props.settings={}] - An object containing settings to customize the appearance and behavior of the rail.
 * @param {boolean} [props.settings.displayAppNameInHeader=false] - If true, displays the app name in the header instead of the app icon.
 * @param {boolean} [props.settings.packRailButtons=false] - Whether to pack the rail buttons together at the top of the rail.
 * @param {string} [props.settings.expandedRailWidth='260px'] - The width of the rail when it is expanded.
 * @param {string} [props.settings.collapsedRailWidth='80px'] - The width of the rail when it is collapsed.
 * @param {boolean} [props.settings.showFooter=true] - Whether to show the footer.
 * @param {boolean} [props.settings.isLoading=false] - Whether to show the loading animation.
 * @param {string} [props.settings.appName='App'] - The name of the app to be displayed in the header.
 */
const AzNavRail = ({
  initiallyExpanded = false,
  disableSwipeToOpen = false, // Note: Swipe gesture is not implemented yet.
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

  /**
   * Toggles the expanded/collapsed state of the navigation rail.
   */
  const onToggle = () => setIsExpanded(!isExpanded);

  const [cyclerStates, setCyclerStates] = useState({});
  const cyclerTimers = useRef({});

  const navItems = content || [];

  // Initialize cycler states when the component mounts or navItems change.
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

    // Cleanup timers on component unmount to prevent memory leaks.
    return () => {
      Object.values(cyclerTimers.current).forEach(clearTimeout);
    };
  }, [navItems]);

  /**
   * Handles the click event for cycler items.
   * On click, it cancels any pending action, advances to the next option,
   * and sets a 1-second timer to trigger the action for the new option.
   * @param {object} item - The cycler navigation item.
   */
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
      onToggle(); // Collapse the menu after the action
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