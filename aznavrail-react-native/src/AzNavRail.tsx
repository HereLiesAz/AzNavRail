import React, { useState, useCallback, useRef, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Animated,
  StyleSheet,
  PanResponder,
  Dimensions,
  ScrollView,
} from 'react-native';
import { AzNavRailContext } from './AzNavRailScope';
import { AzNavItem, AzNavRailSettings, AzButtonShape } from './types';
import { AzNavRailDefaults } from './AzNavRailDefaults';
import { AzButton } from './components/AzButton';
import { AzToggle } from './components/AzToggle';
import { AzCycler } from './components/AzCycler';
import { RailMenuItem } from './components/RailMenuItem';
import { AzLoad } from './components/AzLoad';
import { DraggableRailItemWrapper } from './components/DraggableRailItemWrapper';
import { RelocItemHandler } from './util/RelocItemHandler';

interface AzNavRailProps extends AzNavRailSettings {
  children: React.ReactNode;
  navController?: any;
  currentDestination?: string;
  isLandscape?: boolean;
  initiallyExpanded?: boolean;
  disableSwipeToOpen?: boolean;
  onExpandedChange?: (expanded: boolean) => void;
  onInteraction?: (action: string, details?: string) => void;
}

export const AzNavRail: React.FC<AzNavRailProps> = ({
  children,
  initiallyExpanded = false,
  displayAppNameInHeader = true,
  expandedRailWidth = AzNavRailDefaults.ExpandedRailWidth,
  collapsedRailWidth = AzNavRailDefaults.CollapsedRailWidth,
  showFooter = true,
  isLoading = false,
  defaultShape = AzButtonShape.CIRCLE,
  enableRailDragging = false,
  onExpandedChange,
  onInteraction,
}) => {
  const logInteraction = useCallback(
    (action: string, details?: string) => {
      if (onInteraction) {
        onInteraction(action, details);
        return;
      }

      // Fallback to console logging only in development when no custom logger is configured
      if (typeof __DEV__ !== 'undefined' && __DEV__) {
        const suffix = details ? ` ${details}` : '';
        // eslint-disable-next-line no-console
        console.log(`[AzNavRail] ${action}${suffix}`);
      }
    },
    [onInteraction]
  );
  const [items, setItems] = useState<AzNavItem[]>([]);
  const [isExpanded, setIsExpanded] = useState(initiallyExpanded);
  const [isFloating, setIsFloating] = useState(false);
  const [showFloatingButtons, setShowFloatingButtons] = useState(false);
  const [headerHeight, setHeaderHeight] = useState(0);
  const [hostStates, setHostStates] = useState<Record<string, boolean>>({});

  const railWidthAnim = useRef(new Animated.Value(initiallyExpanded ? expandedRailWidth : collapsedRailWidth)).current;
  const pan = useRef(new Animated.ValueXY()).current;
  const panValue = useRef({ x: 0, y: 0 });
  const dragStartPos = useRef({ x: 0, y: 0 });

  // Animation values for item displacement
  const itemOffsets = useRef<Record<string, Animated.Value>>({});

  // Cache screen height
  const screenHeightRef = useRef(Dimensions.get('window').height);

  // Refs for PanResponder stale closures
  const isFloatingRef = useRef(isFloating);
  const enableRailDraggingRef = useRef(enableRailDragging);
  const showFloatingButtonsRef = useRef(showFloatingButtons);
  const wasVisibleOnDragStartRef = useRef(false);
  const itemsRef = useRef(items);

  useEffect(() => { isFloatingRef.current = isFloating; }, [isFloating]);
  useEffect(() => { enableRailDraggingRef.current = enableRailDragging; }, [enableRailDragging]);
  useEffect(() => { showFloatingButtonsRef.current = showFloatingButtons; }, [showFloatingButtons]);
  useEffect(() => { itemsRef.current = items; }, [items]);

  useEffect(() => {
      const id = pan.addListener((value) => { panValue.current = value; });
      return () => pan.removeListener(id);
  }, [pan]);

  useEffect(() => {
    const subscription = Dimensions.addEventListener('change', ({ window }) => {
      screenHeightRef.current = window.height;
    });
    return () => {
      if (!subscription) return;
      // @ts-ignore
      if (typeof subscription === 'function') {
        // @ts-ignore
        subscription();
      } else if (typeof (subscription as any).remove === 'function') {
        (subscription as any).remove();
      }
    };
  }, []);

  // --- Item Management ---
  const register = useCallback((item: AzNavItem) => {
    setItems((prev) => {
      const index = prev.findIndex((i) => i.id === item.id);
      if (index >= 0) {
        const old = prev[index];
        const isSame =
          old.text === item.text &&
          old.disabled === item.disabled &&
          old.isChecked === item.isChecked &&
          old.selectedOption === item.selectedOption &&
          old.isExpanded === item.isExpanded &&
          JSON.stringify(old.options) === JSON.stringify(item.options) &&
          old.shape === item.shape &&
          old.color === item.color &&
          old.route === item.route &&
          old.isRelocItem === item.isRelocItem &&
          old.hiddenMenu === item.hiddenMenu; // Simplistic check

        if (isSame) return prev;
        const newItems = [...prev];
        newItems[index] = item;
        return newItems;
      }
      return [...prev, item];
    });
  }, []);

  const unregister = useCallback((id: string) => {
    setItems((prev) => prev.filter((i) => i.id !== id));
    if (itemOffsets.current[id]) delete itemOffsets.current[id];
  }, []);

  // --- Animation & Effects ---
  useEffect(() => {
    Animated.timing(railWidthAnim, {
      toValue: isExpanded ? expandedRailWidth : collapsedRailWidth,
      duration: 300,
      useNativeDriver: false,
    }).start();

    if (onExpandedChange) onExpandedChange(isExpanded);
  }, [isExpanded, expandedRailWidth, collapsedRailWidth]);

  // Initialize animation values for new items
  useEffect(() => {
      items.forEach(item => {
          if (!itemOffsets.current[item.id]) {
              itemOffsets.current[item.id] = new Animated.Value(0);
          }
      });
  }, [items]);

  // --- Reloc Item Logic ---
  const handleRelocDragStart = (draggedItemIndex: number) => {
      logInteraction('Reloc drag started', items[draggedItemIndex].text);
  };

  const handleRelocDragEnd = (draggedItemIndex: number) => {
      // Reset all offsets
      Object.values(itemOffsets.current).forEach(anim => anim.setValue(0));
      logInteraction('Reloc drag ended');
  };

  const handleRelocDragMove = (dy: number, draggedItemIndex: number) => {
      const draggedItem = items[draggedItemIndex];
      if (!draggedItem.isRelocItem || !draggedItem.hostId) return;

      const cluster = RelocItemHandler.getCluster(items, draggedItem.hostId);
      const itemHeight = 48 + 8; // Approx height + margin

      const draggedClusterIndex = cluster.findIndex(i => i.id === draggedItem.id);
      const targetClusterIndex = RelocItemHandler.calculateTargetIndex(dy, draggedClusterIndex, cluster.length, itemHeight);

      const targetItem = cluster[targetClusterIndex];

      // Animate offsets
      cluster.forEach((item, index) => {
          if (item.id === draggedItem.id) return;

          let offset = 0;
          // Logic similar to Android/Web implementation
          if (draggedClusterIndex < targetClusterIndex) {
              // Dragging down
              if (index > draggedClusterIndex && index <= targetClusterIndex) {
                  offset = -itemHeight; // Move up
              }
          } else if (draggedClusterIndex > targetClusterIndex) {
              // Dragging up
              if (index >= targetClusterIndex && index < draggedClusterIndex) {
                  offset = itemHeight; // Move down
              }
          }

          Animated.spring(itemOffsets.current[item.id], {
              toValue: offset,
              useNativeDriver: false
          }).start();
      });

      // If dropped here (simulated), update state?
      // In React Native PanResponder, we update state only on release to avoid jank,
      // or we can throttle updates.
      // However, we need to know the final order on release.
      // We can store the calculated target index in a ref.
      (panValue as any).targetIndex = targetClusterIndex;
  };

  const handleRelocDrop = (draggedItemIndex: number) => {
       const draggedItem = items[draggedItemIndex];
       const cluster = RelocItemHandler.getCluster(items, draggedItem.hostId!);
       const draggedClusterIndex = cluster.findIndex(i => i.id === draggedItem.id);
       const targetClusterIndex = (panValue as any).targetIndex ?? draggedClusterIndex; // Default to self if no move

       if (draggedClusterIndex !== targetClusterIndex) {
           const newItems = RelocItemHandler.reorderItems(items, draggedItem.id, draggedItem.hostId!, targetClusterIndex);
           setItems(newItems);

           // Fire callback
           const newCluster = RelocItemHandler.getCluster(newItems, draggedItem.hostId!);
           const newOrder = newCluster.map(i => i.id);
           if (draggedItem.onRelocate) {
               draggedItem.onRelocate(draggedClusterIndex, targetClusterIndex, newOrder);
           }
       }

       // Clean up
       delete (panValue as any).targetIndex;
       handleRelocDragEnd(draggedItemIndex);
  };


  // --- Helpers ---

  const adjustFloatingRailWithinBounds = (currentX: number, currentY: number) => {
      const screenHeight = screenHeightRef.current;
      const railItemsCount = itemsRef.current.filter(i => i.isRailItem && !i.isSubItem).length;
      // Estimated height: Header + (Count * (48 + 8)) + Padding
      // 48 = button size, 8 = margin
      const contentHeight = headerHeight + (railItemsCount * 56) + 16;

      const bottomY = currentY + contentHeight;
      const limitY = screenHeight * 0.9;

      if (bottomY > limitY) {
          const shift = bottomY - limitY;
          const newY = currentY - shift;

          Animated.timing(pan, {
              toValue: { x: currentX, y: newY },
              duration: 200,
              useNativeDriver: false
          }).start();

          logInteraction('Adjusted position', `Shifted up by ${shift}`);
      }
  };

  // --- Gestures ---
  const panResponder = useRef(
    PanResponder.create({
      onMoveShouldSetPanResponder: (_, gestureState) => {
        if (!enableRailDraggingRef.current && !isFloatingRef.current) return false;
        const { dx, dy } = gestureState;
        // "Vertical swipe immediately initiates FAB mode"
        if (!isFloatingRef.current && Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > 10 && enableRailDraggingRef.current) {
            return true;
        }
        return isFloatingRef.current;
      },
      onPanResponderGrant: () => {
        if (isFloatingRef.current) {
             pan.setOffset({
                 x: panValue.current.x,
                 y: panValue.current.y
             });
             pan.setValue({ x: 0, y: 0 });
             dragStartPos.current = { x: panValue.current.x, y: panValue.current.y };

             wasVisibleOnDragStartRef.current = showFloatingButtonsRef.current;
             setShowFloatingButtons(false);
             logInteraction('Drag started', 'FAB mode');
        } else {
             setIsFloating(true);
             setIsExpanded(false);
             logInteraction('Swipe detected', 'Entering FAB mode');

             // Reset to 0,0 for simplicity as we undock
             pan.setOffset({ x: 0, y: 0 });
             pan.setValue({ x: 0, y: 0 });
             dragStartPos.current = { x: 0, y: 0 };
        }
      },
      onPanResponderMove: (_, gestureState) => {
          if (!isFloatingRef.current) return;

          const { dx, dy } = gestureState;
          const screenHeight = screenHeightRef.current;
          const iconSize = AzNavRailDefaults.HeaderIconSize;

          // 10% margin bounds
          const minY = screenHeight * 0.1;
          const maxY = screenHeight * 0.9 - iconSize;

          const targetY = dragStartPos.current.y + dy;

          // Clamp
          let clampedY = Math.max(minY, Math.min(targetY, maxY));

          // Apply as new value (remove offset to get delta)
          pan.setValue({ x: dx, y: clampedY - dragStartPos.current.y });
      },
      onPanResponderRelease: () => {
        pan.flattenOffset();

        if (isFloatingRef.current) {
            logInteraction('Drag ended');

            // Snap back logic
            const currentX = panValue.current.x;
            const currentY = panValue.current.y;
            const distance = Math.sqrt(Math.pow(currentX, 2) + Math.pow(currentY, 2));

             if (distance < AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
                 setIsFloating(false);
                 pan.setValue({ x: 0, y: 0 });
                 logInteraction('Snap back', 'Docked');
             } else if (wasVisibleOnDragStartRef.current) {
                 // Unfold logic
                 setShowFloatingButtons(true);
                 adjustFloatingRailWithinBounds(currentX, currentY);
             }
        }
      },
    })
  ).current;

  const handleHeaderLongPress = () => {
      if (enableRailDragging) {
          if (isFloating) {
              setIsFloating(false);
              pan.setValue({ x: 0, y: 0 });
              logInteraction('Long press', 'Docked rail');
          } else {
              setIsFloating(true);
              setIsExpanded(false);
              logInteraction('Long press', 'Undocked rail');
          }
      }
  };

  const handleHeaderTap = () => {
      logInteraction('Header tapped');
      if (isFloating) {
          // Unfold/Fold logic
          const willShow = !showFloatingButtons;
          setShowFloatingButtons(willShow);

          if (willShow) {
               const currentX = panValue.current.x;
               const currentY = panValue.current.y;
               adjustFloatingRailWithinBounds(currentX, currentY);
          }

      } else {
          setIsExpanded(!isExpanded);
      }
  };

  // --- Render Helpers ---

  const renderRailItem = (item: AzNavItem, index: number) => {
      const isExpandedHost = hostStates[item.id] || false;
      const subItems = items.filter(i => i.hostId === item.id);

      const isRect = item.shape === AzButtonShape.RECTANGLE;

      const commonProps = {
          key: item.id,
          color: item.color,
          shape: item.shape || defaultShape,
          disabled: item.disabled,
          style: { marginBottom: isRect ? 2 : AzNavRailDefaults.RailContentVerticalArrangement }
      };

      if (item.isRelocItem) {
          return (
              <DraggableRailItemWrapper
                  key={item.id}
                  item={item}
                  index={items.indexOf(item)} // Global index
                  totalItems={items.length}
                  onDragStart={handleRelocDragStart}
                  onDragEnd={handleRelocDrop}
                  onDragMove={handleRelocDragMove}
                  offsetY={itemOffsets.current[item.id] || new Animated.Value(0)}
                  style={commonProps.style}
              />
          );
      }

      if (item.isHost) {
           return (
             <View key={item.id} style={{ alignItems: 'center', width: '100%' }}>
                 <AzButton
                     {...commonProps}
                     text={item.text}
                     onClick={() => {
                         setHostStates(prev => ({...prev, [item.id]: !prev[item.id]}));
                         logInteraction('Host toggled', item.text);
                     }}
                 />
                 {isExpandedHost && subItems.map((sub, i) => renderRailItem(sub, items.indexOf(sub)))}
             </View>
           );
      }

      if (item.isCycler) {
          return (
              <AzCycler
                  {...commonProps}
                  options={item.options || []}
                  selectedOption={item.selectedOption || ''}
                  onCycle={() => {
                      if (item.onClick) item.onClick();
                      logInteraction('Cycler cycled', item.text);
                  }}
              />
          );
      }
      if (item.isToggle) {
          return (
              <AzToggle
                  {...commonProps}
                  isChecked={item.isChecked || false}
                  toggleOnText={item.toggleOnText}
                  toggleOffText={item.toggleOffText}
                  onToggle={() => {
                      if (item.onClick) item.onClick();
                      logInteraction('Toggle toggled', item.text);
                  }}
              />
          );
      }

      return (
          <AzButton
              {...commonProps}
              text={item.text}
              onClick={() => {
                  logInteraction('Item clicked', item.text);
                  if (item.onClick) item.onClick();
                  if (item.collapseOnClick) setIsExpanded(false);
              }}
          />
      );
  };

  const renderMenuItem = (item: AzNavItem, depth = 0): React.ReactNode => {
      const isExpandedHost = hostStates[item.id] || false;
      const subItems = items.filter(i => i.hostId === item.id);

      return (
          <RailMenuItem
              key={item.id}
              item={item}
              depth={depth}
              isExpandedHost={isExpandedHost}
              onToggleHost={() => setHostStates(prev => ({ ...prev, [item.id]: !prev[item.id] }))}
              onItemClick={() => {
                  logInteraction('Menu item clicked', item.text);
                  if (item.onClick) item.onClick();
                  if (item.collapseOnClick) setIsExpanded(false);
              }}
              renderSubItems={() => (
                  <>
                  {subItems.map(subItem => renderMenuItem(subItem, depth + 1))}
                  </>
              )}
          />
      );
  };

  const railItems = items.filter(i => i.isRailItem && !i.isSubItem);
  const menuItems = items.filter(i => !i.isSubItem);

  return (
    <AzNavRailContext.Provider value={{ register, unregister }}>
        <View style={{ flexDirection: 'row', height: '100%' }}>
            <Animated.View
                style={[
                    styles.railContainer,
                {
                    width: railWidthAnim,
                    position: isFloating ? 'absolute' : 'relative',
                    transform: isFloating ? [{ translateX: pan.x }, { translateY: pan.y }] : [],
                    zIndex: isFloating ? 1000 : 1,
                    height: isFloating ? 'auto' : '100%',
                }
            ]}
            {...panResponder.panHandlers}
        >
            {/* Header */}
            <TouchableOpacity
                onPress={handleHeaderTap}
                onLongPress={handleHeaderLongPress}
                delayLongPress={500}
                style={styles.header}
                onLayout={(e) => setHeaderHeight(e.nativeEvent.layout.height)}
                accessibilityRole="button"
                accessibilityLabel={isFloating ? "Undocked Rail" : (isExpanded ? "Collapse Menu" : "Expand Menu")}
                accessibilityHint="Double tap to toggle, long press to drag"
            >
                 <View style={{ width: AzNavRailDefaults.HeaderIconSize, height: AzNavRailDefaults.HeaderIconSize, backgroundColor: 'gray', borderRadius: 24, alignItems: 'center', justifyContent: 'center' }}>
                     <Text style={{ color: 'white' }}>Icon</Text>
                 </View>
                 {(!isFloating && isExpanded && displayAppNameInHeader) && (
                     <Text style={styles.appName} numberOfLines={1}>App Name</Text>
                 )}
            </TouchableOpacity>

            {/* Content */}
            {isExpanded ? (
                <ScrollView style={styles.menuContent}>
                    {menuItems.map(item => {
                         if (item.isDivider) {
                             return <View key={item.id} style={styles.divider} />;
                         }
                         return renderMenuItem(item);
                    })}
                    {showFooter && (
                        <View style={styles.footer}>
                            <Text>Footer</Text>
                        </View>
                    )}
                </ScrollView>
            ) : (
                <ScrollView contentContainerStyle={styles.railContent}>
                     {(isFloating && !showFloatingButtons) ? null : railItems.map((item, index) => renderRailItem(item, index))}
                </ScrollView>
            )}

            </Animated.View>
            {children}
            {isLoading && (
                 <View style={styles.loaderOverlay}>
                     <AzLoad />
                 </View>
            )}
        </View>
    </AzNavRailContext.Provider>
  );
};

const styles = StyleSheet.create({
  railContainer: {
    backgroundColor: '#f0f0f0',
    overflow: 'hidden',
    borderRightWidth: 1,
    borderRightColor: '#ddd',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: AzNavRailDefaults.HeaderPadding,
  },
  appName: {
    marginLeft: 16,
    fontWeight: 'bold',
    fontSize: 18,
  },
  railContent: {
    paddingHorizontal: AzNavRailDefaults.RailContentHorizontalPadding,
    paddingVertical: 8,
    alignItems: 'center',
  },
  menuContent: {
    flex: 1,
  },
  menuItem: {
      padding: 16,
  },
  menuItemText: {
      fontSize: 16,
  },
  divider: {
      height: 1,
      backgroundColor: '#ccc',
      marginVertical: 8,
  },
  footer: {
      padding: 16,
      borderTopWidth: 1,
      borderTopColor: '#ccc',
  },
  loaderOverlay: {
      ...StyleSheet.absoluteFillObject,
      backgroundColor: 'rgba(255,255,255,0.5)',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 10000,
      elevation: 10,
  }
});
