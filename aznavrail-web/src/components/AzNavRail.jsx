import React, { useState, useEffect, useRef, useMemo } from 'react';
import './AzNavRail.css';
import MenuItem from './MenuItem';
import AzNavRailButton from './AzNavRailButton';
import HelpOverlay from './HelpOverlay';
import { RelocItemHandler } from '../utils/RelocItemHandler';
import AzTextBox from './AzTextBox';

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
  const [draggedItemId, setDraggedItemId] = useState(null);
  const [dragOffset, setDragOffset] = useState(0);
  const [localNavItems, setLocalNavItems] = useState([]);
  const [hiddenMenuOpenId, setHiddenMenuOpenId] = useState(null);
  const cyclerTimers = useRef({});
  const dragStartY = useRef(0);
  const longPressTimer = useRef(null);
  const itemsRef = useRef([]);

  useEffect(() => {
      setLocalNavItems(content || []);
  }, [content]);

  useEffect(() => {
      itemsRef.current = localNavItems;
  }, [localNavItems]);

  const navItems = localNavItems;

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

  const handleMouseDown = (e, item) => {
      if (!item.isRelocItem || infoScreen) return;

      dragStartY.current = e.clientY;
      const initialIndex = itemsRef.current.findIndex(it => it.id === item.id);

      longPressTimer.current = setTimeout(() => {
          setHiddenMenuOpenId(item.id);
          longPressTimer.current = null;
      }, 500);

      const onMouseMove = (moveEvent) => {
          const diff = moveEvent.clientY - dragStartY.current;
          if (Math.abs(diff) > 5) {
              if (longPressTimer.current) {
                  clearTimeout(longPressTimer.current);
                  longPressTimer.current = null;
              }
              setDraggedItemId(item.id);
              setDragOffset(diff);

              const itemHeight = 56; // Approximation
              const slotsMoved = Math.round(diff / itemHeight);
              const currentItems = itemsRef.current;

              if (slotsMoved !== 0) {
                  const currentIndex = currentItems.findIndex(it => it.id === item.id);
                  if (currentIndex !== -1) {
                      const targetIndex = currentIndex + slotsMoved;
                      if (targetIndex >= 0 && targetIndex < currentItems.length) {
                          const cluster = RelocItemHandler.findCluster(currentItems, item.id);
                          if (cluster && targetIndex >= cluster.start && targetIndex <= cluster.end) {
                              const newItems = RelocItemHandler.updateOrder(currentItems, item.id, targetIndex);
                              // Comparison needs deep check or just ID check if reference changed
                              // updateOrder returns new array
                              if (newItems !== currentItems) {
                                  setLocalNavItems(newItems);
                                  dragStartY.current = moveEvent.clientY; // Reset reference
                                  setDragOffset(0);
                              }
                          }
                      }
                  }
              }
          }
      };

      const onMouseUp = () => {
          if (longPressTimer.current) {
              clearTimeout(longPressTimer.current);
          }
          // Access ref for final state
          const finalItems = itemsRef.current;
          // We can't check draggedItemId state reliably inside closure if it's stale,
          // but we set it. However, simpler to just check if we were dragging.
          // Or rely on closure variable 'item'.
          const finalIndex = finalItems.findIndex(it => it.id === item.id);
          if (item.onRelocate && initialIndex !== -1 && finalIndex !== -1) {
                item.onRelocate(initialIndex, finalIndex, finalItems.map(it => it.id));
          }
          setDraggedItemId(null);
          setDragOffset(0);
          window.removeEventListener('mousemove', onMouseMove);
          window.removeEventListener('mouseup', onMouseUp);
      };

      window.addEventListener('mousemove', onMouseMove);
      window.addEventListener('mouseup', onMouseUp);
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
              {/* DEBUG */}
              {/* <div style={{fontSize: '8px'}}>{JSON.stringify(navItems.map(i => i.id))}</div> */}

              {navItems
                .filter(item => item.isRailItem || item.items)
                .map(item => {
                  const finalItem = item.isCycler
                    ? { ...item, selectedOption: cyclerStates[item.id]?.displayedOption }
                    : item;

                  // Render logic...
                  // For reloc items, we must wrap them to handle mouse events
                  if (item.isRelocItem) {
                       return (
                          <div key={item.id} onMouseDown={(e) => handleMouseDown(e, item)} style={{position: 'relative', width: '100%', display: 'flex', justifyContent: 'center'}}>
                              <AzNavRailButton
                                  item={finalItem}
                                  onCyclerClick={() => handleCyclerClick(item)}
                                  infoScreen={infoScreen}
                                  style={{ transform: draggedItemId === item.id ? `translateY(${dragOffset}px)` : 'none', zIndex: draggedItemId === item.id ? 100 : 1 }}
                              />
                              {hiddenMenuOpenId === item.id && item.hiddenMenu && (
                                  <div className="hidden-menu-popup" style={{position: 'absolute', left: '100%', top: 0, zIndex: 1000, background: 'white', border: '1px solid black', padding: '8px', width: '150px'}}>
                                      {item.hiddenMenu.map((menuItem, idx) => (
                                          <div key={idx} style={{padding: '8px', borderBottom: '1px solid #eee', cursor: 'pointer'}} onClick={() => {
                                              if(menuItem.onClick) menuItem.onClick();
                                              setHiddenMenuOpenId(null);
                                          }}>
                                              {menuItem.text}
                                          </div>
                                      ))}
                                  </div>
                              )}
                          </div>
                       );
                  }

                  return (
                    <React.Fragment key={item.id}>
                        <AzNavRailButton
                            item={finalItem}
                            onCyclerClick={() => handleCyclerClick(item)}
                            onClickOverride={item.items ? () => {
                                if (infoScreen) {
                                    toggleHost(item);
                                } else {
                                    setIsExpanded(true);
                                    setHostStates(prev => ({ ...prev, [item.id]: true }));
                                }
                            } : undefined}
                            infoScreen={infoScreen}
                        />
                        {/* Sub items rendering */}
                        {item.items && hostStates[item.id] && (
                            item.items.filter(sub => sub.isRailItem).map(sub => (
                                <div key={sub.id} onMouseDown={(e) => handleMouseDown(e, sub)} style={{position: 'relative'}}>
                                    <AzNavRailButton
                                        item={sub}
                                        onCyclerClick={() => handleCyclerClick(sub)}
                                        infoScreen={infoScreen}
                                        style={{ transform: draggedItemId === sub.id ? `translateY(${dragOffset}px)` : 'none', zIndex: draggedItemId === sub.id ? 100 : 1 }}
                                    />
                                    {/* ... hidden menu for sub items ... */}
                                </div>
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
