"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.useAzTutorialController = exports.AzTutorialProvider = exports.AzTutorialContext = void 0;
var _react = _interopRequireWildcard(require("react"));
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
const AzTutorialContext = exports.AzTutorialContext = /*#__PURE__*/(0, _react.createContext)(null);
const AzTutorialProvider = ({
  children,
  initialActiveTutorialId = null,
  initialReadTutorials = []
}) => {
  const [activeTutorialId, setActiveTutorialId] = (0, _react.useState)(initialActiveTutorialId);
  const [readTutorials, setReadTutorials] = (0, _react.useState)(initialReadTutorials);
  const startTutorial = (0, _react.useCallback)(id => {
    setActiveTutorialId(id);
  }, []);
  const endTutorial = (0, _react.useCallback)(() => {
    setActiveTutorialId(null);
  }, []);
  const markTutorialRead = (0, _react.useCallback)(id => {
    setReadTutorials(prev => {
      if (!prev.includes(id)) {
        return [...prev, id];
      }
      return prev;
    });
  }, []);
  const isTutorialRead = (0, _react.useCallback)(id => {
    return readTutorials.includes(id);
  }, [readTutorials]);
  const contextValue = (0, _react.useMemo)(() => ({
    activeTutorialId,
    readTutorials,
    startTutorial,
    endTutorial,
    markTutorialRead,
    isTutorialRead
  }), [activeTutorialId, readTutorials, startTutorial, endTutorial, markTutorialRead, isTutorialRead]);
  return /*#__PURE__*/_react.default.createElement(AzTutorialContext.Provider, {
    value: contextValue
  }, children);
};
exports.AzTutorialProvider = AzTutorialProvider;
const useAzTutorialController = () => {
  const context = (0, _react.useContext)(AzTutorialContext);
  if (!context) {
    throw new Error('useAzTutorialController must be used within an AzTutorialProvider');
  }
  return context;
};
exports.useAzTutorialController = useAzTutorialController;
//# sourceMappingURL=AzTutorialController.js.map