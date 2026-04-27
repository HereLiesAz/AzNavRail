"use strict";

var _react = _interopRequireDefault(require("react"));
var _react2 = require("@testing-library/react");
var _vitest = require("vitest");
var _AzToggle = _interopRequireDefault(require("./AzToggle"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
(0, _vitest.describe)('AzToggle', () => {
  (0, _vitest.it)('renders correctly with default props', () => {
    (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzToggle.default, {
      value: false,
      onValueChange: () => {}
    }));
    const checkbox = _react2.screen.getByRole('checkbox');
    (0, _vitest.expect)(checkbox).toBeInTheDocument();
    (0, _vitest.expect)(checkbox).not.toBeChecked();
    (0, _vitest.expect)(checkbox).not.toBeDisabled();
  });
  (0, _vitest.it)('renders correctly when checked', () => {
    (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzToggle.default, {
      value: true,
      onValueChange: () => {}
    }));
    const checkbox = _react2.screen.getByRole('checkbox');
    (0, _vitest.expect)(checkbox).toBeChecked();
  });
  (0, _vitest.it)('renders label if provided', () => {
    (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzToggle.default, {
      value: false,
      onValueChange: () => {},
      label: "Test Label"
    }));
    (0, _vitest.expect)(_react2.screen.getByText('Test Label')).toBeInTheDocument();
  });
  (0, _vitest.it)('calls onValueChange when clicked', () => {
    const handleValueChange = _vitest.vi.fn();
    (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzToggle.default, {
      value: false,
      onValueChange: handleValueChange
    }));
    const checkbox = _react2.screen.getByRole('checkbox');
    _react2.fireEvent.click(checkbox);
    (0, _vitest.expect)(handleValueChange).toHaveBeenCalledTimes(1);
    (0, _vitest.expect)(handleValueChange).toHaveBeenCalledWith(true);
  });
  (0, _vitest.it)('does not call onValueChange when disabled', () => {
    const handleValueChange = _vitest.vi.fn();
    (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzToggle.default, {
      value: false,
      onValueChange: handleValueChange,
      enabled: false
    }));
    const checkbox = _react2.screen.getByRole('checkbox');
    _react2.fireEvent.click(checkbox);
    (0, _vitest.expect)(handleValueChange).not.toHaveBeenCalled();
    (0, _vitest.expect)(checkbox).toBeDisabled();
  });
  (0, _vitest.it)('applies custom className and style', () => {
    const {
      container
    } = (0, _react2.render)(/*#__PURE__*/_react.default.createElement(_AzToggle.default, {
      value: false,
      onValueChange: () => {},
      className: "custom-class",
      style: {
        marginTop: '10px'
      }
    }));
    const wrapper = container.firstChild;
    (0, _vitest.expect)(wrapper).toHaveClass('custom-class');
    (0, _vitest.expect)(wrapper).toHaveStyle('margin-top: 10px');
  });
});
//# sourceMappingURL=AzToggle.test.js.map