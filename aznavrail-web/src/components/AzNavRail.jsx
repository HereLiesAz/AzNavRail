import React, { useState, useEffect, useRef } from 'react';
import './AzNavRail.css';
import MenuItem from './MenuItem';
import AzNavRailButton from './AzNavRailButton';
import HelpOverlay from './HelpOverlay';
import { RelocItemHandler } from '../utils/RelocItemHandler';
import AzDivider from './AzDivider';

/**
 * An M3-style navigation rail that expands into a menu drawer for web applications.
 *
 * @param {object} props - The component props.
 * @param {boolean} [props.initiallyExpanded=false] - Whether the navigation rail is expanded by default.
 * @param {Array<object>} props.content - An array of navigation item objects (tree structure).
 * @param {object} [props.settings={}] - An object containing settings.
 * @param {string} [props.currentDestination] - The current route/destination ID to determine active state.
 */
const AzNavRail = ({
  initiallyExpanded = false,
  content,
  settings = {},
  currentDestination
}) => {
  const {
    displayAppNameInHeader = false,
    expandedRailWidth = '260px',
    collapsedRailWidth = '80px',
    showFooter = true,
    isLoading = false,
    appName = 'App',
    infoScreen = false,
    onDismissInfoScreen,
    dockingSide = 'LEFT',
    noMenu = false,
    activeClassifiers = [], // Array of strings
    activeColor,
    packRailButtons = false,
    headerIconShape = 'CIRCLE'
  } = settings;

  // If noMenu is true, we force expanded to false, unless infoScreen overrides (which it doesn't really)
  // Logic: effectiveNoMenu prevents expansion.
  const effectiveNoMenu = noMenu;

  const [isExpanded, setIsExpanded] = useState(initiallyExpanded && !effectiveNoMenu);

  // Update expansion state if noMenu changes
  useEffect(() => {
      if (effectiveNoMenu && isExpanded) {
          setIsExpanded(false);
      }
  }, [effectiveNoMenu, isExpanded]);

  const [showFooterPopup, setShowFooterPopup] = useState(false);

  const onToggle = () => {
      if (infoScreen) return;
      if (effectiveNoMenu) {
          setShowFooterPopup(!showFooterPopup);
      } else {
          setIsExpanded(!isExpanded);
      }
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

  // Flatten items for cycler initialization
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

  const checkIsActive = (item) => {
      if (item.route && item.route === currentDestination) return true;
      if (item.classifiers && activeClassifiers.length > 0) {
          // Check intersection
          return item.classifiers.some(c => activeClassifiers.includes(c));
      }
      return false;
  };

  const handleMouseDown = (e, item) => {
      if (!item.isRelocItem || infoScreen) return;

      dragStartY.current = e.clientY;
      const initialIndex = itemsRef.current.findIndex(it => it.id === item.id);

      longPressTimer.current = setTimeout(() => {
          setHiddenMenuOpenId(null); // Close menu if dragging starts
          longPressTimer.current = null;
          // Start drag indication if needed
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
                              if (newItems !== currentItems) {
                                  setLocalNavItems(newItems);
                                  dragStartY.current = moveEvent.clientY;
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
          const finalItems = itemsRef.current;
          const initialIdx = itemsRef.current.findIndex(it => it.id === item.id); // Re-find in case index changed?
          // Actually itemsRef updates on state change, so finding by ID is safer.
          const finalIndex = finalItems.findIndex(it => it.id === item.id);

          if (item.onRelocate && initialIndex !== -1 && finalIndex !== -1 && initialIndex !== finalIndex) {
                // Trigger relocate only if moved
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

  const handleDoubleTap = (item) => {
      if (item.hiddenMenu) {
          setHiddenMenuOpenId(prev => prev === item.id ? null : item.id);
      }
  };

  const renderMenuItem = (item, depth = 0) => {
      if (item.isDivider) {
          return <AzDivider key={item.id} />;
      }

      const finalItem = item.isCycler
        ? { ...item, selectedOption: cyclerStates[item.id]?.displayedOption }
        : item;

      const isHost = item.items && item.items.length > 0;
      const isHostExpanded = hostStates[item.id];
      const isActive = checkIsActive(item);

      // Inject isActive into item for MenuItem (if it supported it) or pass as prop
      // MenuItem logic update would be needed to use isActive prop visually

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
                  // Pass active state if MenuItem supports it, or modify item color?
                  // Currently simple pass-through.
              />
              {isHost && isHostExpanded && (
                  <div className="az-nav-rail-subitems">
                      {item.items.map(subItem => renderMenuItem(subItem, depth + 1))}
                  </div>
              )}
          </React.Fragment>
      );
  };

  const getVisibleItems = () => {
    const visible = [];
    navItems.forEach(item => {
        if (item.isRailItem || item.items) {
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

  const getHeaderIconClass = () => {
      switch (headerIconShape) {
          case 'ROUNDED': return 'app-icon rounded';
          case 'NONE': return 'app-icon none';
          case 'CIRCLE':
          default: return 'app-icon circle';
      }
  };

  return (
    <>
    <div
      className={`az-nav-rail ${isExpanded ? 'expanded' : 'collapsed'} ${dockingSide === 'RIGHT' ? 'right' : ''}`}
      style={{ width: isExpanded ? expandedRailWidth : collapsedRailWidth }}
    >
      <div className="header" onClick={onToggle}>
        {displayAppNameInHeader ? (
          <span>{appName}</span>
        ) : (
          <img src="/app-icon.png" alt="App Icon" className={getHeaderIconClass()} />
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
            <div className={`rail ${packRailButtons ? 'packed' : ''}`} style={{overflowY: 'auto', maxHeight: '100%'}}>
              {navItems
                .filter(item => item.isRailItem || item.items)
                .map(item => {
                  if (item.isDivider) return <AzDivider key={item.id} />;

                  const finalItem = item.isCycler
                    ? { ...item, selectedOption: cyclerStates[item.id]?.displayedOption }
                    : item;

                  const isActive = checkIsActive(item);

                  // For reloc items
                  if (item.isRelocItem) {
                       return (
                          <div
                            key={item.id}
                            onMouseDown={(e) => handleMouseDown(e, item)}
                            onDoubleClick={() => handleDoubleTap(item)}
                            style={{position: 'relative', width: '100%', display: 'flex', justifyContent: 'center'}}
                          >
                              <AzNavRailButton
                                  item={{...finalItem, isActive}} // Pass isActive if Button supports it
                                  onCyclerClick={() => handleCyclerClick(item)}
                                  infoScreen={infoScreen}
                                  style={{
                                      transform: draggedItemId === item.id ? `translateY(${dragOffset}px)` : 'none',
                                      zIndex: draggedItemId === item.id ? 100 : 1,
                                      // Apply active color override if active
                                      borderColor: isActive && activeColor ? activeColor : (item.color || 'blue')
                                  }}
                              />
                              {hiddenMenuOpenId === item.id && item.hiddenMenu && (
                                  <div className="hidden-menu-popup" style={{
                                      position: 'absolute',
                                      left: dockingSide === 'RIGHT' ? 'auto' : '100%',
                                      right: dockingSide === 'RIGHT' ? '100%' : 'auto',
                                      top: 0,
                                      zIndex: 1000,
                                      background: 'white',
                                      border: '1px solid black',
                                      padding: '8px',
                                      width: '150px'
                                  }}>
                                      {item.hiddenMenu.map((menuItem, idx) => (
                                          <div key={idx} style={{padding: '8px', borderBottom: '1px solid #eee', cursor: 'pointer'}} onClick={(e) => {
                                              e.stopPropagation(); // Prevent parent clicks
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
                            item={{...finalItem, isActive}}
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
                            style={{
                                borderColor: isActive && activeColor ? activeColor : (item.color || 'blue')
                            }}
                        />
                        {/* Sub items rendering */}
                        {item.items && hostStates[item.id] && (
                            item.items.filter(sub => sub.isRailItem).map(sub => {
                                const subActive = checkIsActive(sub);
                                return (
                                <div key={sub.id} onMouseDown={(e) => handleMouseDown(e, sub)} style={{position: 'relative'}}>
                                    <AzNavRailButton
                                        item={{...sub, isActive: subActive}}
                                        onCyclerClick={() => handleCyclerClick(sub)}
                                        infoScreen={infoScreen}
                                        style={{
                                            transform: draggedItemId === sub.id ? `translateY(${dragOffset}px)` : 'none',
                                            zIndex: draggedItemId === sub.id ? 100 : 1,
                                            borderColor: subActive && activeColor ? activeColor : (sub.color || 'blue')
                                        }}
                                    />
                                </div>
                            )})
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
                 {appName}
             </div>
        </div>
      )}

      {/* No Menu Footer Popup */}
      {showFooterPopup && effectiveNoMenu && (
          <div style={{
              position: 'fixed',
              bottom: '20px',
              left: '50%',
              transform: 'translateX(-50%)',
              background: 'white',
              border: '1px solid #ccc',
              padding: '16px',
              borderRadius: '8px',
              zIndex: 2000,
              boxShadow: '0 4px 6px rgba(0,0,0,0.1)'
          }}>
              <div>{appName}</div>
              <button onClick={() => setShowFooterPopup(false)} style={{marginTop: '8px'}}>Close</button>
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
