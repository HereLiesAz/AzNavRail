import React from 'react';
import renderer, { act } from 'react-test-renderer';
import { TouchableOpacity, TextInput } from 'react-native';
import { AzTextBox } from '../src/components/AzTextBox';
import { historyManager } from '../src/util/HistoryManager';

// Mock historyManager
jest.mock('../src/util/HistoryManager', () => {
  return {
    historyManager: {
      addEntry: jest.fn(),
      getSuggestions: jest.fn(() => []),
      setLimit: jest.fn(),
    },
  };
});

describe('AzTextBox', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should call historyManager.addEntry on submit when secret is false', () => {
    let component: renderer.ReactTestRenderer;
    act(() => {
      component = renderer.create(
        <AzTextBox hint="Enter text" historyContext="test-context-1" />
      );
    });
    const root = component!.root;

    const input = root.findByType(TextInput);
    act(() => {
      input.props.onChangeText('hello world');
    });

    const buttons = root.findAllByType(TouchableOpacity);
    const submitButton = buttons.find(b => b.props.onPress && b.props.onPress.name === 'handleSubmit');
    act(() => {
      if (submitButton) submitButton.props.onPress();
    });

    expect(historyManager.addEntry).toHaveBeenCalledWith('test-context-1', 'hello world');
  });

  it('should NOT call historyManager.addEntry on submit when secret is true', () => {
    let component: renderer.ReactTestRenderer;
    act(() => {
      component = renderer.create(
        <AzTextBox hint="Enter password" secret={true} historyContext="test-context-2" />
      );
    });
    const root = component!.root;

    const input = root.findByType(TextInput);
    act(() => {
      input.props.onChangeText('my_secret_password');
    });

    const buttons = root.findAllByType(TouchableOpacity);
    const submitButton = buttons.find(b => b.props.onPress && b.props.onPress.name === 'handleSubmit');
    act(() => {
      if (submitButton) submitButton.props.onPress();
    });

    expect(historyManager.addEntry).not.toHaveBeenCalled();
  });
});
