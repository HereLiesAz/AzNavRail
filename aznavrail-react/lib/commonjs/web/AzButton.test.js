"use strict";

var _react = _interopRequireDefault(require("react"));
var _react2 = require("@testing-library/react");
var _vitest = require("vitest");
var _AzButton = _interopRequireDefault(require("./AzButton"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
// Mock the useFitText hook
_vitest.vi.mock('../hooks/useFitText', () => ({
  default: () => ({
    current: null
  })
}));
(0, _vitest.describe)('AzButton', () => {
  (0, _vitest.it)('renders text correctly', () => {
    (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzButton.default, {
      text: "Click Me",
      onClick: () => {}
    }));
    (0, _vitest.expect)(_react2.screen.getByText('Click Me')).toBeInTheDocument();
  });
  (0, _vitest.it)('calls onClick when clicked and enabled', () => {
    const handleClick = _vitest.vi.fn();
    (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzButton.default, {
      text: "Click Me",
      onClick: handleClick
    }));
    const button = _react2.screen.getByRole('button');
    _react2.fireEvent.click(button);
    (0, _vitest.expect)(handleClick).toHaveBeenCalledTimes(1);
  });
  (0, _vitest.it)('does not call onClick when enabled is false', () => {
    const handleClick = _vitest.vi.fn();
    (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzButton.default, {
      text: "Click Me",
      onClick: handleClick,
      enabled: false
    }));
    const button = _react2.screen.getByRole('button');
    (0, _vitest.expect)(button).toBeDisabled();
    _react2.fireEvent.click(button);
    (0, _vitest.expect)(handleClick).not.toHaveBeenCalled();
  });
  (0, _vitest.it)('does not call onClick when isLoading is true', () => {
    const handleClick = _vitest.vi.fn();
    (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzButton.default, {
      text: "Click Me",
      onClick: handleClick,
      isLoading: true
    }));
    const button = _react2.screen.getByRole('button');
    _react2.fireEvent.click(button);
    (0, _vitest.expect)(handleClick).not.toHaveBeenCalled();
  });
  (0, _vitest.it)('applies the correct shape class', () => {
    const {
      rerender
    } = (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzButton.default, {
      text: "Shape",
      shape: "CIRCLE",
      onClick: () => {}
    }));
    let button = _react2.screen.getByRole('button');
    (0, _vitest.expect)(button).toHaveClass('az-button-shape-circle');
    rerender(/*#__PURE__*/_react.default.createElement(_AzButton.default, {
      text: "Shape",
      shape: "ROUNDED",
      onClick: () => {}
    }));
    button = _react2.screen.getByRole('button');
    (0, _vitest.expect)(button).toHaveClass('az-button-shape-rounded');
  });
  (0, _vitest.it)('applies custom padding, color and class', () => {
    (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzButton.default, {
      text: "Custom",
      onClick: () => {},
      contentPadding: "20px",
      color: "#ff0000",
      className: "my-custom-class"
    }));
    const button = _react2.screen.getByRole('button');
    (0, _vitest.expect)(button).toHaveClass('my-custom-class');
    // Using rgb for the color since it might be converted
    (0, _vitest.expect)(button).toHaveStyle({
      padding: '20px',
      color: 'rgb(255, 0, 0)'
    });
  });
  (0, _vitest.it)('renders loading state correctly', () => {
    const {
      container
    } = (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzButton.default, {
      text: "Loading",
      onClick: () => {},
      isLoading: true
    }));

    // The text content wrapper should have opacity 0 when loading
    const contentWrapper = container.querySelector('.az-button-content');
    (0, _vitest.expect)(contentWrapper).toHaveStyle({
      opacity: 0
    });

    // The AzLoad component should be rendered
    const loader = container.querySelector('.az-load-container');
    (0, _vitest.expect)(loader).toBeInTheDocument();
  });
});
//# sourceMappingURL=AzButton.test.js.map