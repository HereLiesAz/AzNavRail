import React from 'react';
import { render, act } from '@testing-library/react-native';
import { AzTutorialProvider, useAzTutorialController } from '../tutorial/AzTutorialController';

let capturedController: ReturnType<typeof useAzTutorialController> | null = null;

const Capture = () => {
  capturedController = useAzTutorialController();
  return null;
};

const wrap = () =>
  render(
    <AzTutorialProvider>
      <Capture />
    </AzTutorialProvider>
  );

beforeEach(() => { capturedController = null; });

describe('AzTutorialController', () => {
  it('startTutorial stores id and variables', () => {
    wrap();
    act(() => { capturedController!.startTutorial('t1', { level: 'advanced' }); });
    expect(capturedController!.activeTutorialId).toBe('t1');
    expect(capturedController!.currentVariables.level).toBe('advanced');
  });

  it('startTutorial without variables defaults to empty', () => {
    wrap();
    act(() => { capturedController!.startTutorial('t1'); });
    expect(capturedController!.currentVariables).toEqual({});
  });

  it('fireEvent sets pendingEvent', () => {
    wrap();
    act(() => { capturedController!.fireEvent('menu_opened'); });
    expect(capturedController!.pendingEvent).toBe('menu_opened');
  });

  it('consumeEvent clears pendingEvent', () => {
    wrap();
    act(() => { capturedController!.fireEvent('menu_opened'); });
    act(() => { capturedController!.consumeEvent(); });
    expect(capturedController!.pendingEvent).toBeNull();
  });

  it('endTutorial clears all transient state', () => {
    wrap();
    act(() => { capturedController!.startTutorial('t1', { x: 1 }); capturedController!.fireEvent('ev'); });
    act(() => { capturedController!.endTutorial(); });
    expect(capturedController!.activeTutorialId).toBeNull();
    expect(capturedController!.currentVariables).toEqual({});
    expect(capturedController!.pendingEvent).toBeNull();
  });
});
