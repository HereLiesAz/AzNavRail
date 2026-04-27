"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _react = _interopRequireDefault(require("react"));
var _useFitText = _interopRequireDefault(require("../hooks/useFitText"));
require("./AzNavRailButton.css");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * A circular button for the collapsed navigation rail.
 */
const AzNavRailButton = ({
  item,
  onCyclerClick,
  onClickOverride,
  infoScreen,
  style,
  onItemGloballyPositioned
}) => {
  const {
    text,
    isToggle,
    isChecked,
    toggleOnText,
    toggleOffText,
    isCycler,
    selectedOption,
    onClick,
    onFocus,
    color,
    id,
    disabled,
    content
  } = item;
  const fitTextRef = (0, _useFitText.default)();
  const textRef = fitTextRef; // Always fit text in rail button

  const textToShow = (() => {
    if (isToggle) return isChecked ? toggleOnText : toggleOffText;
    if (isCycler) return selectedOption || '';
    return text;
  })();
  const isInteractive = infoScreen ? !!onClickOverride || item.isHelpItem : !disabled;
  const handleClick = e => {
    if (!isInteractive) return;
    if (infoScreen) {
      if (item.isHelpItem && onClick) {
        onClick(e);
      } else if (onClickOverride) {
        onClickOverride(e); // Allow host expansion
      }
      return;
    }

    // Always trigger onFocus if present (parity with Android)
    if (onFocus) {
      onFocus();
    }
    if (onClickOverride) {
      onClickOverride(e);
    } else if (isCycler) {
      onCyclerClick();
    } else {
      onClick && onClick();
    }
  };
  const ariaProps = {};
  if (isToggle) {
    ariaProps['aria-checked'] = isChecked;
    ariaProps.role = 'switch';
  } else if (item.isHost) {
    ariaProps['aria-expanded'] = item.isExpanded;
  } else if (isCycler) {
    ariaProps['aria-label'] = `${text} ${selectedOption}`;
  } else if (item.isNestedRail) {
    ariaProps['aria-expanded'] = item.isExpanded;
    ariaProps['aria-haspopup'] = 'true';
  }
  const shapeClass = item.shape ? item.shape.toLowerCase() : 'circle';
  const isReactNode = content && /*#__PURE__*/_react.default.isValidElement(content);
  const effectiveColor = color || 'blue';
  const lowerColor = effectiveColor.toLowerCase();
  const computedFillColor = lowerColor === 'black' || lowerColor === '#000000' || lowerColor === '#000' ? 'rgba(255, 255, 255, 0.25)' : 'rgba(0, 0, 0, 0.25)';
  const finalFillColor = item.fillColor || computedFillColor;
  const wrapperRef = _react.default.useRef(null);
  _react.default.useEffect(() => {
    if (onItemGloballyPositioned && wrapperRef.current) {
      onItemGloballyPositioned(id, wrapperRef.current.getBoundingClientRect());
    }
  }, [id, onItemGloballyPositioned]);
  return /*#__PURE__*/_react.default.createElement("div", {
    style: {
      position: 'relative'
    },
    "data-az-nav-id": id,
    ref: wrapperRef
  }, /*#__PURE__*/_react.default.createElement("button", _extends({
    className: `az-nav-rail-button ${shapeClass} ${!isInteractive ? 'disabled' : ''}`,
    onClick: handleClick,
    style: {
      borderColor: effectiveColor,
      backgroundColor: finalFillColor,
      opacity: isInteractive ? 1 : 0.5,
      cursor: isInteractive ? 'pointer' : 'default',
      width: '64px',
      minWidth: '64px',
      maxWidth: '64px',
      height: '64px',
      minHeight: '64px',
      maxHeight: '64px',
      overflow: 'hidden',
      ...style
    },
    disabled: !isInteractive
  }, ariaProps), content ? isReactNode ? content : /*#__PURE__*/_react.default.createElement("div", {
    className: "button-content-wrapper",
    style: {
      width: '100%',
      height: '100%',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, typeof content === 'string' && (content.startsWith('http') || content.startsWith('/') || content.startsWith('data:')) ? /*#__PURE__*/_react.default.createElement("img", {
    src: content,
    alt: "",
    style: {
      width: '100%',
      height: '100%',
      objectFit: 'cover',
      borderRadius: 'inherit'
    }
  }) : /*#__PURE__*/_react.default.createElement("div", {
    style: {
      backgroundColor: content,
      width: '100%',
      height: '100%',
      borderRadius: 'inherit'
    }
  })) : /*#__PURE__*/_react.default.createElement("span", {
    className: "button-text",
    ref: textRef
  }, textToShow)));
};
var _default = exports.default = AzNavRailButton;
//# sourceMappingURL=AzNavRailButton.js.map