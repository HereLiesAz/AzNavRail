"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.HelpOverlay = void 0;
var _react = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
var _AzTutorialController = require("../tutorial/AzTutorialController");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
const HelpOverlay = ({
  items,
  onDismiss,
  helpList,
  itemBounds,
  nestedRailVisibleId = null,
  tutorials = {}
}) => {
  const [expandedItemId, setExpandedItemId] = (0, _react.useState)(null);
  const [cardBounds, setCardBounds] = (0, _react.useState)({});
  const tutorialController = (0, _AzTutorialController.useAzTutorialController)();
  const isNestedRailOpen = nestedRailVisibleId !== null;
  const paddingLeft = isNestedRailOpen ? 240 : 120;
  const allItems = _react.default.useMemo(() => {
    const list = [...items];
    if (nestedRailVisibleId) {
      const nestedHost = items.find(i => i.id === nestedRailVisibleId);
      if (nestedHost !== null && nestedHost !== void 0 && nestedHost.nestedRailItems) {
        list.push(...nestedHost.nestedRailItems);
      }
    }
    return list;
  }, [items, nestedRailVisibleId]);
  const [scrollY, setScrollY] = (0, _react.useState)(0);
  const itemsWithInfo = _react.default.useMemo(() => {
    return allItems.filter(i => {
      var _i$info, _helpList$i$id;
      const infoText = (_i$info = i.info) === null || _i$info === void 0 ? void 0 : _i$info.trim();
      const listText = helpList === null || helpList === void 0 || (_helpList$i$id = helpList[i.id]) === null || _helpList$i$id === void 0 ? void 0 : _helpList$i$id.trim();
      return infoText || listText;
    });
  }, [allItems, helpList]);
  return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.overlay
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: _reactNative.StyleSheet.absoluteFill,
    pointerEvents: "none"
  }, itemsWithInfo.map(item => {
    const navBounds = itemBounds[item.id];
    const descBounds = cardBounds[item.id];
    if (navBounds && descBounds) {
      const startX = navBounds.x + navBounds.width;
      const startY = navBounds.y + navBounds.height / 2;
      const endX = descBounds.x;
      // Apply scroll offset to description card Y coordinate for lines
      const endY = descBounds.y + descBounds.height / 2 - scrollY;
      const elbowX = (startX + endX) / 2;
      return /*#__PURE__*/_react.default.createElement(_react.default.Fragment, {
        key: `line-${item.id}`
      }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
        style: {
          position: 'absolute',
          left: Math.min(startX, elbowX),
          top: startY,
          width: Math.abs(elbowX - startX),
          height: 2,
          backgroundColor: 'yellow'
        }
      }), /*#__PURE__*/_react.default.createElement(_reactNative.View, {
        style: {
          position: 'absolute',
          left: elbowX,
          top: Math.min(startY, endY),
          width: 2,
          height: Math.abs(endY - startY),
          backgroundColor: 'yellow'
        }
      }), /*#__PURE__*/_react.default.createElement(_reactNative.View, {
        style: {
          position: 'absolute',
          left: Math.min(elbowX, endX),
          top: endY,
          width: Math.abs(endX - elbowX),
          height: 2,
          backgroundColor: 'yellow'
        }
      }));
    }
    return null;
  })), /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    style: _reactNative.StyleSheet.absoluteFill,
    onPress: onDismiss,
    activeOpacity: 1
  }), /*#__PURE__*/_react.default.createElement(_reactNative.ScrollView, {
    style: styles.scrollView,
    contentContainerStyle: [styles.scrollContent, {
      paddingLeft
    }],
    scrollEventThrottle: 16,
    onScroll: e => setScrollY(e.nativeEvent.contentOffset.y)
  }, itemsWithInfo.map(i => {
    var _i$info2, _helpList$i$id2, _i$text;
    const infoText = (_i$info2 = i.info) === null || _i$info2 === void 0 ? void 0 : _i$info2.trim();
    const listText = helpList === null || helpList === void 0 || (_helpList$i$id2 = helpList[i.id]) === null || _helpList$i$id2 === void 0 ? void 0 : _helpList$i$id2.trim();
    const titleText = ((_i$text = i.text) === null || _i$text === void 0 ? void 0 : _i$text.trim()) || `Item ${i.id}`;
    const isExpanded = expandedItemId === i.id;
    const hasTutorial = !!tutorials[i.id];
    return /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
      key: i.id,
      style: styles.card,
      activeOpacity: 0.8,
      onPress: () => {
        if (hasTutorial) {
          tutorialController.startTutorial(i.id);
          onDismiss();
        } else {
          setExpandedItemId(isExpanded ? null : i.id);
        }
      },
      onLayout: e => {
        const layout = e.nativeEvent.layout;
        setCardBounds(prev => ({
          ...prev,
          [i.id]: layout
        }));
      }
    }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
      style: styles.cardTitle
    }, titleText), infoText && /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
      style: styles.cardText,
      numberOfLines: isExpanded ? undefined : 1
    }, infoText), listText && /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
      style: [styles.cardText, infoText ? {
        marginTop: 8
      } : {}],
      numberOfLines: isExpanded ? undefined : 1
    }, listText), isExpanded && /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
      style: styles.tapToCollapse
    }, "Tap to collapse"));
  })));
};
exports.HelpOverlay = HelpOverlay;
const styles = _reactNative.StyleSheet.create({
  overlay: {
    ..._reactNative.StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.7)',
    zIndex: 9999
  },
  scrollView: {
    flex: 1
  },
  scrollContent: {
    paddingVertical: 32,
    paddingRight: 16
  },
  card: {
    backgroundColor: '#333',
    padding: 16,
    marginBottom: 16,
    borderRadius: 8
  },
  cardTitle: {
    color: 'yellow',
    fontWeight: 'bold',
    fontSize: 16,
    marginBottom: 4
  },
  cardText: {
    color: 'white',
    fontSize: 14
  },
  tapToCollapse: {
    color: 'gray',
    fontSize: 12,
    marginTop: 8
  }
});
//# sourceMappingURL=HelpOverlay.js.map