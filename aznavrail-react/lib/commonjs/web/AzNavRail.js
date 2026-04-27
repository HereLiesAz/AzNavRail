"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _react = _interopRequireWildcard(require("react"));
require("./AzNavRail.css");
var _MenuItem = _interopRequireDefault(require("./MenuItem"));
var _AzNavRailButton = _interopRequireDefault(require("./AzNavRailButton"));
var _HelpOverlay = _interopRequireDefault(require("./HelpOverlay"));
var _AzNestedRailPopup = _interopRequireDefault(require("./AzNestedRailPopup"));
var _RelocItemHandler = require("../util/RelocItemHandler");
var _AzDivider = _interopRequireDefault(require("./AzDivider"));
var _AzTextBox = _interopRequireDefault(require("./AzTextBox"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
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
    activeClassifiers = new Set(),
    // Set of strings
    activeColor,
    translucentBackground,
    packRailButtons = false,
    headerIconShape = 'CIRCLE'
  } = settings;

  // If noMenu is true, we force expanded to false, unless infoScreen overrides (which it doesn't really)
  // Logic: effectiveNoMenu prevents expansion.
  const effectiveNoMenu = noMenu;
  const [isExpanded, setIsExpanded] = (0, _react.useState)(initiallyExpanded && !effectiveNoMenu);

  // Update expansion state if noMenu changes
  (0, _react.useEffect)(() => {
    if (effectiveNoMenu && isExpanded) {
      setIsExpanded(false);
    }
  }, [effectiveNoMenu, isExpanded]);
  const [showFooterPopup, setShowFooterPopup] = (0, _react.useState)(false);
  const onToggle = () => {
    if (infoScreen) return;
    if (effectiveNoMenu) {
      setShowFooterPopup(!showFooterPopup);
    } else {
      setIsExpanded(!isExpanded);
    }
  };
  const [cyclerStates, setCyclerStates] = (0, _react.useState)({});
  const [hostStates, setHostStates] = (0, _react.useState)({});
  const [draggedItemId, setDraggedItemId] = (0, _react.useState)(null);
  const [dragOffset, setDragOffset] = (0, _react.useState)(0);
  const [localNavItems, setLocalNavItems] = (0, _react.useState)([]);
  const [hiddenMenuOpenId, setHiddenMenuOpenId] = (0, _react.useState)(null);
  const [nestedRailVisibleId, setNestedRailVisibleId] = (0, _react.useState)(null);
  (0, _react.useEffect)(() => {
    const openItem = localNavItems.find(item => item.forceHiddenMenuOpen);
    if (openItem) {
      setHiddenMenuOpenId(openItem.id);
    }
  }, [localNavItems]);
  (0, _react.useEffect)(() => {
    const handleClickOutside = e => {
      if (hiddenMenuOpenId && !e.target.closest('.hidden-menu-popup')) {
        const item = localNavItems.find(i => i.id === hiddenMenuOpenId);
        if (item && item.onHiddenMenuDismiss) {
          item.onHiddenMenuDismiss();
        }
        setHiddenMenuOpenId(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [hiddenMenuOpenId, localNavItems]);
  const [anchorPosition, setAnchorPosition] = (0, _react.useState)(null);
  const [itemBounds, setItemBounds] = (0, _react.useState)({});
  const subItemsMap = (0, _react.useMemo)(() => {
    const map = {};
    const processItem = item => {
      if (item.hostId) {
        if (!map[item.hostId]) map[item.hostId] = [];
        // Avoid duplicates if item is already present
        if (!map[item.hostId].some(i => i.id === item.id)) {
          map[item.hostId].push(item);
        }
      }
      if (item.items) {
        // Also index nested items as sub-items of their parent
        if (!map[item.id]) map[item.id] = [];
        item.items.forEach(child => {
          if (!map[item.id].some(i => i.id === child.id)) {
            map[item.id].push(child);
          }
          processItem(child);
        });
      }
    };
    localNavItems.forEach(processItem);
    return map;
  }, [localNavItems]);
  const getEffectiveSubItems = (0, _react.useCallback)(item => {
    const subItemsFromMap = subItemsMap[item.id] || [];
    const subItemsFromProp = item.items || [];
    return subItemsFromMap.length > 0 ? subItemsFromMap : subItemsFromProp;
  }, [subItemsMap]);
  const cyclerTimers = (0, _react.useRef)({});
  const dragStartY = (0, _react.useRef)(0);
  const longPressTimer = (0, _react.useRef)(null);
  const itemsRef = (0, _react.useRef)([]);
  (0, _react.useEffect)(() => {
    setLocalNavItems(content || []);
  }, [content]);
  (0, _react.useEffect)(() => {
    itemsRef.current = localNavItems;
  }, [localNavItems]);
  const updateItemBound = (0, _react.useCallback)((id, element) => {
    if (!element) return;
    const rect = element.getBoundingClientRect();
    const bounds = {
      top: rect.top,
      left: rect.left,
      width: rect.width,
      height: rect.height
    };
    setItemBounds(prev => {
      if (prev[id] && prev[id].top === bounds.top && prev[id].left === bounds.left) return prev;
      return {
        ...prev,
        [id]: bounds
      };
    });
  }, []);
  const navItems = localNavItems;

  // Flatten items for cycler initialization
  (0, _react.useEffect)(() => {
    const initialCyclerStates = {};
    const processItem = item => {
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
  const handleCyclerClick = item => {
    var _cyclerStates$item$id;
    if (cyclerTimers.current[item.id]) {
      clearTimeout(cyclerTimers.current[item.id]);
    }
    const {
      options
    } = item;
    const currentOption = ((_cyclerStates$item$id = cyclerStates[item.id]) === null || _cyclerStates$item$id === void 0 ? void 0 : _cyclerStates$item$id.displayedOption) || item.selectedOption;
    const currentIndex = options.indexOf(currentOption);
    const nextIndex = (currentIndex + 1) % options.length;
    const nextOption = options[nextIndex];
    setCyclerStates(prev => ({
      ...prev,
      [item.id]: {
        ...prev[item.id],
        displayedOption: nextOption
      }
    }));
    cyclerTimers.current[item.id] = setTimeout(() => {
      if (item.onClick) item.onClick(nextOption);
      delete cyclerTimers.current[item.id];
    }, 1000);
  };
  const toggleHost = item => {
    setHostStates(prev => ({
      ...prev,
      [item.id]: !prev[item.id]
    }));
  };
  const checkIsActive = item => {
    if (item.route && item.route === currentDestination) return true;
    if (item.classifiers && activeClassifiers.size > 0) {
      // Check intersection
      const classifiersArray = Array.isArray(item.classifiers) ? item.classifiers : Array.from(item.classifiers);
      return classifiersArray.some(c => activeClassifiers.has(c));
    }
    if (item.id === currentDestination) return true;
    return false;
  };
  const handleMouseDown = (e, item) => {
    if (!item.isRelocItem || infoScreen) return;
    dragStartY.current = e.clientY;
    const initialIndex = itemsRef.current.findIndex(it => it.id === item.id);
    let currentIndex = initialIndex;
    longPressTimer.current = setTimeout(() => {
      setHiddenMenuOpenId(null); // Close menu if dragging starts
      longPressTimer.current = null;
      // Start drag indication if needed
    }, 500);
    const onMouseMove = moveEvent => {
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
        if (slotsMoved !== 0 && currentIndex !== -1) {
          const targetIndex = currentIndex + slotsMoved;
          if (targetIndex >= 0 && targetIndex < currentItems.length) {
            const cluster = _RelocItemHandler.RelocItemHandler.findCluster(currentItems, item.id, currentIndex);
            if (cluster && targetIndex >= cluster.start && targetIndex <= cluster.end) {
              const newItems = _RelocItemHandler.RelocItemHandler.updateOrder(currentItems, item.id, targetIndex, currentIndex);
              if (newItems !== currentItems) {
                itemsRef.current = newItems;
                setLocalNavItems(newItems);
                currentIndex = targetIndex;
                dragStartY.current = moveEvent.clientY;
                setDragOffset(0);
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
      const finalIndex = currentIndex;
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
  const handleDoubleTap = item => {
    if (item.hiddenMenu) {
      setHiddenMenuOpenId(prev => prev === item.id ? null : item.id);
    }
  };
  const renderMenuItem = (item, depth = 0) => {
    var _cyclerStates$item$id2;
    if (item.isDivider) {
      return /*#__PURE__*/_react.default.createElement(_AzDivider.default, {
        key: item.id
      });
    }
    const finalItem = item.isCycler ? {
      ...item,
      selectedOption: (_cyclerStates$item$id2 = cyclerStates[item.id]) === null || _cyclerStates$item$id2 === void 0 ? void 0 : _cyclerStates$item$id2.displayedOption
    } : item;
    const effectiveSubItems = getEffectiveSubItems(item);
    const isHost = item.isHost || effectiveSubItems.length > 0;
    const isHostExpanded = hostStates[item.id];
    const isActive = checkIsActive(item);
    return /*#__PURE__*/_react.default.createElement(_react.default.Fragment, {
      key: item.id
    }, /*#__PURE__*/_react.default.createElement(_MenuItem.default, {
      item: finalItem,
      depth: depth,
      onToggle: onToggle,
      onCyclerClick: () => handleCyclerClick(item),
      isHost: isHost,
      isExpanded: isHostExpanded,
      onHostClick: () => toggleHost(item),
      infoScreen: infoScreen
    }), isHost && isHostExpanded && /*#__PURE__*/_react.default.createElement("div", {
      className: "az-nav-rail-subitems"
    }, effectiveSubItems.map(subItem => renderMenuItem(subItem, depth + 1))));
  };
  const visibleItems = (0, _react.useMemo)(() => {
    const visible = [];
    navItems.forEach(item => {
      if (item.isRailItem || item.items) {
        visible.push(item);
        if (hostStates[item.id]) {
          const mapSubs = getEffectiveSubItems(item);
          mapSubs.forEach(sub => {
            if (sub.isRailItem) visible.push(sub);
          });
        }
      }
    });
    return visible;
  }, [navItems, hostStates, subItemsMap, getEffectiveSubItems]);
  const effectiveRailItems = (0, _react.useMemo)(() => {
    return navItems.filter(item => item.isRailItem || item.items || subItemsMap[item.id]);
  }, [navItems, subItemsMap]);
  const menuItems = (0, _react.useMemo)(() => {
    return navItems.filter(item => !item.isSubItem);
  }, [navItems]);
  const getHeaderIconClass = () => {
    switch (headerIconShape) {
      case 'ROUNDED':
        return 'app-icon rounded';
      case 'NONE':
        return 'app-icon none';
      case 'CIRCLE':
      default:
        return 'app-icon circle';
    }
  };
  return /*#__PURE__*/_react.default.createElement(_react.default.Fragment, null, /*#__PURE__*/_react.default.createElement("div", {
    className: `az-nav-rail ${isExpanded ? 'expanded' : 'collapsed'} ${dockingSide === 'RIGHT' ? 'right' : ''}`,
    style: {
      width: isExpanded ? expandedRailWidth : collapsedRailWidth,
      backgroundColor: translucentBackground || '#f0f0f0'
    }
  }, /*#__PURE__*/_react.default.createElement("div", {
    className: "header",
    onClick: onToggle
  }, displayAppNameInHeader ? /*#__PURE__*/_react.default.createElement("span", null, appName) : /*#__PURE__*/_react.default.createElement("img", {
    src: "/app-icon.png",
    alt: "App Icon",
    className: getHeaderIconClass()
  })), isLoading ? /*#__PURE__*/_react.default.createElement("div", {
    className: "loading-spinner"
  }, "Loading...") : /*#__PURE__*/_react.default.createElement("div", {
    className: "content"
  }, isExpanded ? /*#__PURE__*/_react.default.createElement("div", {
    className: "menu"
  }, menuItems.map(item => renderMenuItem(item))) : /*#__PURE__*/_react.default.createElement("div", {
    className: `rail ${packRailButtons ? 'packed' : ''}`,
    style: {
      overflowY: 'auto',
      maxHeight: '100%'
    }
  }, effectiveRailItems.map(item => {
    var _cyclerStates$item$id3;
    if (item.isDivider) return /*#__PURE__*/_react.default.createElement(_AzDivider.default, {
      key: item.id
    });
    const finalItem = item.isCycler ? {
      ...item,
      selectedOption: (_cyclerStates$item$id3 = cyclerStates[item.id]) === null || _cyclerStates$item$id3 === void 0 ? void 0 : _cyclerStates$item$id3.displayedOption
    } : item;
    const isActive = checkIsActive(item);

    // For reloc items
    if (item.isRelocItem) {
      const subItems = subItemsMap[item.id] || [];
      return /*#__PURE__*/_react.default.createElement("div", {
        key: item.id,
        ref: el => updateItemBound(item.id, el),
        onMouseDown: e => handleMouseDown(e, item),
        onDoubleClick: () => handleDoubleTap(item),
        style: {
          position: 'relative',
          width: '100%',
          display: 'flex',
          justifyContent: 'center'
        }
      }, /*#__PURE__*/_react.default.createElement(_AzNavRailButton.default, {
        item: {
          ...finalItem,
          isActive
        } // Pass isActive if Button supports it
        ,
        onCyclerClick: () => handleCyclerClick(item),
        onClickOverride: item.isNestedRail ? e => {
          const rect = e.currentTarget.getBoundingClientRect();
          setAnchorPosition({
            top: rect.top,
            left: rect.left,
            width: rect.width,
            height: rect.height
          });
          setNestedRailVisibleId(item.id);
        } : undefined,
        infoScreen: infoScreen,
        style: {
          transform: draggedItemId === item.id ? `translateY(${dragOffset}px)` : 'none',
          zIndex: draggedItemId === item.id ? 100 : 1,
          // Apply active color override if active
          borderColor: isActive && activeColor ? activeColor : item.color || 'blue'
        }
      }), hiddenMenuOpenId === item.id && item.hiddenMenu && /*#__PURE__*/_react.default.createElement("div", {
        className: "hidden-menu-popup",
        style: {
          position: 'absolute',
          left: dockingSide === 'RIGHT' ? 'auto' : '100%',
          right: dockingSide === 'RIGHT' ? '100%' : 'auto',
          top: 0,
          zIndex: 1000,
          background: translucentBackground || 'white',
          border: '1px solid black',
          padding: '8px',
          width: '150px'
        }
      }, (() => {
        const menuItems = [];
        if (typeof item.hiddenMenu === 'function') {
          const scope = {
            listItem: (text, action) => {
              if (typeof action === 'string') {
                menuItems.push({
                  text,
                  route: action
                });
              } else {
                menuItems.push({
                  text,
                  onClick: action
                });
              }
            },
            inputItem: (hint, arg2, arg3) => {
              let initialValue = '';
              let onValueChange;
              if (typeof arg2 === 'string') {
                initialValue = arg2;
                if (typeof arg3 !== 'function') {
                  console.warn("inputItem requires an onValueChange function callback.");
                  onValueChange = () => {};
                } else {
                  onValueChange = arg3;
                }
              } else if (typeof arg2 === 'function') {
                onValueChange = arg2;
              } else {
                console.warn("inputItem requires an onValueChange function callback.");
                onValueChange = () => {};
              }
              menuItems.push({
                text: '',
                isInput: true,
                hint,
                initialValue,
                onValueChange
              });
            }
          };
          item.hiddenMenu(scope);
        } else {
          menuItems.push(...item.hiddenMenu);
        }
        return menuItems.map((menuItem, idx) => {
          if (menuItem.isInput) {
            return /*#__PURE__*/_react.default.createElement("div", {
              key: idx,
              style: {
                padding: '4px'
              },
              onClick: e => e.stopPropagation()
            }, /*#__PURE__*/_react.default.createElement(_AzTextBox.default, {
              initialValue: menuItem.initialValue,
              hint: menuItem.hint,
              onValueChange: menuItem.onValueChange,
              onSubmit: val => {
                if (menuItem.onValueChange) menuItem.onValueChange(val);
                if (item.onHiddenMenuDismiss) item.onHiddenMenuDismiss();
                setHiddenMenuOpenId(null);
              },
              showSubmitButton: true
            }));
          }
          return /*#__PURE__*/_react.default.createElement("div", {
            key: idx,
            style: {
              padding: '8px',
              borderBottom: '1px solid #eee',
              cursor: 'pointer'
            },
            onClick: e => {
              e.stopPropagation();
              if (menuItem.onClick) menuItem.onClick();
              if (menuItem.route) window.location.href = menuItem.route;
              if (item.onHiddenMenuDismiss) item.onHiddenMenuDismiss();
              setHiddenMenuOpenId(null);
            }
          }, menuItem.text);
        });
      })()));
    }
    return /*#__PURE__*/_react.default.createElement("div", {
      key: item.id,
      ref: el => updateItemBound(item.id, el)
    }, /*#__PURE__*/_react.default.createElement(_AzNavRailButton.default, {
      item: {
        ...finalItem,
        isActive
      },
      onCyclerClick: () => handleCyclerClick(item),
      onClickOverride: e => {
        if (item.isNestedRail) {
          const rect = e.currentTarget.getBoundingClientRect();
          setAnchorPosition({
            top: rect.top,
            left: rect.left,
            width: rect.width,
            height: rect.height
          });
          setNestedRailVisibleId(item.id);
          return;
        }
        if (item.items || subItemsMap[item.id]) {
          toggleHost(item);
        }
      },
      infoScreen: infoScreen,
      style: {
        borderColor: isActive && activeColor ? activeColor : item.color || 'blue'
      }
    }), hostStates[item.id] && (subItemsMap[item.id] || []).filter(sub => sub.isRailItem).map(sub => {
      const subActive = checkIsActive(sub);
      return /*#__PURE__*/_react.default.createElement("div", {
        key: sub.id,
        ref: el => updateItemBound(sub.id, el),
        onMouseDown: e => handleMouseDown(e, sub),
        style: {
          position: 'relative'
        }
      }, /*#__PURE__*/_react.default.createElement(_AzNavRailButton.default, {
        item: {
          ...sub,
          isActive: subActive
        },
        onCyclerClick: () => handleCyclerClick(sub),
        infoScreen: infoScreen,
        style: {
          transform: draggedItemId === sub.id ? `translateY(${dragOffset}px)` : 'none',
          zIndex: draggedItemId === sub.id ? 100 : 1,
          borderColor: subActive && activeColor ? activeColor : sub.color || 'blue'
        }
      }));
    }));
  }))), showFooter && isExpanded && /*#__PURE__*/_react.default.createElement("div", {
    className: "footer",
    style: {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      padding: '16px',
      color: activeColor || 'currentColor'
    }
  }, /*#__PURE__*/_react.default.createElement("div", {
    className: "az-menu-item-text",
    style: {
      padding: '8px 0',
      fontWeight: 'bold'
    }
  }, appName), /*#__PURE__*/_react.default.createElement("div", {
    className: "az-menu-item-text",
    style: {
      padding: '4px 0'
    }
  }, "About"), /*#__PURE__*/_react.default.createElement("div", {
    className: "az-menu-item-text",
    style: {
      padding: '4px 0'
    }
  }, "Feedback"), /*#__PURE__*/_react.default.createElement("div", {
    className: "az-menu-item-text",
    style: {
      padding: '4px 0',
      opacity: 0.5
    }
  }, "@HereLiesAz")), showFooterPopup && effectiveNoMenu && /*#__PURE__*/_react.default.createElement("div", {
    style: {
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
    }
  }, /*#__PURE__*/_react.default.createElement("div", null, appName), /*#__PURE__*/_react.default.createElement("button", {
    onClick: () => setShowFooterPopup(false),
    style: {
      marginTop: '8px'
    }
  }, "Close"))), infoScreen && /*#__PURE__*/_react.default.createElement(_HelpOverlay.default, {
    items: visibleItems,
    itemBounds: itemBounds,
    railWidth: collapsedRailWidth,
    onDismiss: onDismissInfoScreen,
    nestedRailVisibleId: nestedRailVisibleId,
    helpList: (settings === null || settings === void 0 ? void 0 : settings.helpList) || {}
  }), effectiveRailItems.filter(i => i.isNestedRail).map(item => /*#__PURE__*/_react.default.createElement(_AzNestedRailPopup.default, {
    key: `nested-${item.id}`,
    visible: nestedRailVisibleId === item.id,
    onDismiss: () => setNestedRailVisibleId(null),
    items: subItemsMap[item.id] || [],
    alignment: item.nestedRailAlignment || 'VERTICAL',
    renderItem: (sub, idx) => /*#__PURE__*/_react.default.createElement(_AzNavRailButton.default, {
      key: sub.id,
      item: sub,
      onCyclerClick: () => handleCyclerClick(sub),
      infoScreen: infoScreen,
      onItemGloballyPositioned: (id, rect) => setItemBounds(prev => ({
        ...prev,
        [id]: rect
      }))
    }),
    anchorPosition: anchorPosition,
    dockingSide: dockingSide
  })));
};
var _default = exports.default = AzNavRail;
//# sourceMappingURL=AzNavRail.js.map