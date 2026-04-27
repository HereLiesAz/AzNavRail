"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzTutorialOverlay = void 0;
var _react = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
var _AzTutorialController = require("../tutorial/AzTutorialController");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
const AzTutorialOverlay = ({
  tutorialId,
  tutorial,
  onDismiss,
  itemBoundsCache
}) => {
  var _currentCard$highligh;
  const [currentSceneIndex, setCurrentSceneIndex] = (0, _react.useState)(0);
  const [currentCardIndex, setCurrentCardIndex] = (0, _react.useState)(0);
  const tutorialController = (0, _AzTutorialController.useAzTutorialController)();
  (0, _react.useEffect)(() => {
    if (currentSceneIndex >= tutorial.scenes.length) {
      tutorialController.markTutorialRead(tutorialId);
      onDismiss();
    }
  }, [currentSceneIndex, tutorial.scenes.length, tutorialId, onDismiss, tutorialController]);
  if (currentSceneIndex >= tutorial.scenes.length) {
    return null;
  }
  const currentScene = tutorial.scenes[currentSceneIndex];
  const currentCard = currentScene.cards[currentCardIndex];
  if (!currentCard) {
    return null;
  }
  const highlightBounds = (0, _react.useMemo)(() => {
    const highlight = currentCard.highlight;
    if (!highlight) return null;
    if (highlight.type === 'Area') {
      return highlight.bounds;
    } else if (highlight.type === 'Item') {
      return itemBoundsCache[highlight.id];
    }
    return null;
  }, [currentCard.highlight, itemBoundsCache]);
  const isFullScreenHighlight = ((_currentCard$highligh = currentCard.highlight) === null || _currentCard$highligh === void 0 ? void 0 : _currentCard$highligh.type) === 'FullScreen';
  const window = _reactNative.Dimensions.get('window');
  const handleAction = () => {
    if (currentCard.onAction) {
      currentCard.onAction();
    }
    if (currentCardIndex + 1 >= currentScene.cards.length) {
      if (currentScene.onComplete) {
        currentScene.onComplete();
      }
      setCurrentSceneIndex(prev => prev + 1);
      setCurrentCardIndex(0);
    } else {
      setCurrentCardIndex(prev => prev + 1);
    }
  };
  const handleSkip = () => {
    tutorialController.markTutorialRead(tutorialId);
    onDismiss();
  };
  return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: _reactNative.StyleSheet.absoluteFill,
    pointerEvents: "box-none"
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: _reactNative.StyleSheet.absoluteFill,
    pointerEvents: "box-none"
  }, currentScene.content()), !isFullScreenHighlight && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.overlayMask,
    pointerEvents: "auto"
  }, highlightBounds && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [styles.cutout, {
      left: highlightBounds.x,
      top: highlightBounds.y,
      width: highlightBounds.width,
      height: highlightBounds.height
    }],
    pointerEvents: "none"
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.cutoutInner
  }))), /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.cardContainer,
    pointerEvents: "box-none"
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.card
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: styles.cardTitle
  }, currentCard.title), /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: styles.cardText
  }, currentCard.text), /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.buttonRow
  }, /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: handleSkip,
    style: styles.skipButton
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: styles.skipButtonText
  }, "Skip Tutorial")), /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: handleAction,
    style: styles.actionButton
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: styles.actionButtonText
  }, currentCard.actionText || 'Next'))))));
};
exports.AzTutorialOverlay = AzTutorialOverlay;
const styles = _reactNative.StyleSheet.create({
  overlayMask: {
    ..._reactNative.StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.7)',
    zIndex: 9998,
    overflow: 'hidden'
  },
  cutout: {
    position: 'absolute',
    borderRadius: 16
    // The "box-shadow" approach to create a transparent hole.
    // React Native doesn't perfectly support massive box shadows like web,
    // so for a true cross-platform cutout, we often use bordered absolute views.
    // We will simulate it by rendering border on the cutout inner view.
  },
  cutoutInner: {
    ..._reactNative.StyleSheet.absoluteFillObject,
    borderRadius: 16,
    borderColor: 'rgba(0,0,0,0.7)',
    borderWidth: 9999,
    // Extremely large border
    margin: -9999 // Offset by border width
  },
  cardContainer: {
    ..._reactNative.StyleSheet.absoluteFillObject,
    justifyContent: 'flex-end',
    alignItems: 'center',
    padding: 32,
    zIndex: 9999
  },
  card: {
    width: '80%',
    maxWidth: 400,
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 24,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4
    },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8
  },
  cardTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 16,
    color: '#000'
  },
  cardText: {
    fontSize: 16,
    color: '#333',
    marginBottom: 24
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  skipButton: {
    padding: 8
  },
  skipButtonText: {
    color: '#6200EE',
    fontSize: 14,
    fontWeight: '600'
  },
  actionButton: {
    backgroundColor: '#6200EE',
    paddingHorizontal: 24,
    paddingVertical: 10,
    borderRadius: 20
  },
  actionButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600'
  }
});
//# sourceMappingURL=AzTutorialOverlay.js.map