/// <reference lib="dom" />
/**
 * @jest-environment jsdom
 */
import React from 'react';
import { render, act } from '@testing-library/react';
import { AzWebTutorialProvider, useAzWebTutorialController } from './AzTutorialController';

let ctrl: ReturnType<typeof useAzWebTutorialController> | null = null;

const Capture = () => {
  ctrl = useAzWebTutorialController();
  return null;
};

const wrap = () =>
  render(<AzWebTutorialProvider><Capture /></AzWebTutorialProvider>);

beforeEach(() => {
  ctrl = null;
  localStorage.clear();
});

describe('AzWebTutorialController', () => {
  it('startTutorial stores id and variables', () => {
    wrap();
    act(() => { ctrl!.startTutorial('t1', { level: 'advanced' }); });
    expect(ctrl!.activeTutorialId).toBe('t1');
    expect(ctrl!.currentVariables.level).toBe('advanced');
  });

  it('fireEvent sets pendingEvent', () => {
    wrap();
    act(() => { ctrl!.fireEvent('menu_opened'); });
    expect(ctrl!.pendingEvent).toBe('menu_opened');
  });

  it('consumeEvent clears pendingEvent', () => {
    wrap();
    act(() => { ctrl!.fireEvent('ev'); });
    act(() => { ctrl!.consumeEvent(); });
    expect(ctrl!.pendingEvent).toBeNull();
  });

  it('markTutorialRead persists to localStorage', () => {
    wrap();
    act(() => { ctrl!.markTutorialRead('t1'); });
    const stored = JSON.parse(localStorage.getItem('az_navrail_read_tutorials') ?? '[]');
    expect(stored).toContain('t1');
  });

  it('loads persisted read tutorials from localStorage on init', () => {
    localStorage.setItem('az_navrail_read_tutorials', JSON.stringify(['t-prev']));
    wrap();
    expect(ctrl!.isTutorialRead('t-prev')).toBe(true);
  });
});
