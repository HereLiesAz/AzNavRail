import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
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

// These used to reach through react-test-renderer for the TouchableOpacity whose `onPress`
// happened to be named `handleSubmit`. The submit affordance the user actually sees is the
// button labelled GO, so that is what these press — a rename of the handler no longer breaks
// the test, and a submit button that stops rendering now does.
describe('AzTextBox', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should call historyManager.addEntry on submit when secret is false', async () => {
    const { getByPlaceholderText, getByText } = await render(
      <AzTextBox hint="Enter text" historyContext="test-context-1" />
    );

    await fireEvent.changeText(getByPlaceholderText('Enter text'), 'hello world');
    await fireEvent.press(getByText('GO'));

    expect(historyManager.addEntry).toHaveBeenCalledWith('test-context-1', 'hello world');
  });

  it('should NOT call historyManager.addEntry on submit when secret is true', async () => {
    const { getByPlaceholderText, getByText } = await render(
      <AzTextBox hint="Enter password" secret={true} historyContext="test-context-2" />
    );

    await fireEvent.changeText(getByPlaceholderText('Enter password'), 'my_secret_password');
    await fireEvent.press(getByText('GO'));

    expect(historyManager.addEntry).not.toHaveBeenCalled();
  });
});
