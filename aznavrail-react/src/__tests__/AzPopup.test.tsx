import React from 'react';
import { Text } from 'react-native';
import { render, fireEvent, act } from '@testing-library/react-native';
import { AzNavRail } from '../AzNavRail';
import { AzRailItem } from '../AzNavRailScope';
import {
  AzPopup,
  AzPopupKind,
  useAzPopupController,
} from '../components/AzPopup';
import { AzItemAlert, azItemAlertColor } from '../types';
import { clearItemOverride } from '../services/itemOverrides';

describe('AzPopup', () => {
  // `itemOverrides` is a module-level store outside React (see its own docs — the write side of
  // what `AzRailPalette` publishes the other way), so it outlives any one test's render tree.
  // `<AzNavRail>` itself needs fake timers, same as AzNavRailFull.test.tsx — otherwise its
  // background About-prefetch fetch keeps a real async op alive past the test's own teardown.
  beforeEach(() => jest.useFakeTimers());
  afterEach(async () => {
    await act(async () => clearItemOverride('sync'));
    jest.useRealTimers();
  });

  it('is not rendered until shown, then renders its title/message', async () => {
    const Harness = () => {
      const controller = useAzPopupController();
      React.useEffect(() => {
        controller.show({ title: 'Offline', message: 'Changes are queued.' });
      }, [controller]);
      return <AzPopup controller={controller} />;
    };
    const { queryByText } = await render(<Harness />);
    expect(queryByText('Offline')).not.toBeNull();
    expect(queryByText('Changes are queued.')).not.toBeNull();
  });

  it('dismisses via the default body OK button', async () => {
    const Harness = () => {
      const controller = useAzPopupController();
      React.useEffect(() => {
        controller.show({ title: 'Hi' });
      }, [controller]);
      return <AzPopup controller={controller} />;
    };
    const { queryByText, getByText } = await render(<Harness />);
    expect(queryByText('Hi')).not.toBeNull();
    await act(async () => fireEvent.press(getByText('OK')));
    expect(queryByText('Hi')).toBeNull();
  });

  it('dismisses on outside-scrim tap by default', async () => {
    const Harness = () => {
      const controller = useAzPopupController();
      React.useEffect(() => {
        controller.show({ title: 'Tap outside' });
      }, [controller]);
      return <AzPopup controller={controller} />;
    };
    const { queryByText, getByTestId } = await render(<Harness />);
    expect(queryByText('Tap outside')).not.toBeNull();
    await act(async () => fireEvent.press(getByTestId('az-popup-scrim')));
    expect(queryByText('Tap outside')).toBeNull();
  });

  it('renders a custom body via the children render prop instead of AzPopupBody', async () => {
    const Harness = () => {
      const controller = useAzPopupController();
      React.useEffect(() => {
        controller.show({ title: 'Ignored' });
      }, [controller]);
      return (
        <AzPopup controller={controller}>
          {(scope) => <Text>{scope.title}-custom</Text>}
        </AzPopup>
      );
    };
    const { queryByText } = await render(<Harness />);
    expect(queryByText('Ignored-custom')).not.toBeNull();
  });

  it('flags the bound rail item with the alert colour while a WARNING popup is open, and clears it on dismiss', async () => {
    const Harness = () => {
      const controller = useAzPopupController();
      React.useEffect(() => {
        controller.show({ itemId: 'sync', kind: AzPopupKind.WARNING });
      }, [controller]);
      return (
        <>
          <AzNavRail>
            <AzRailItem id="sync" text="Sync" onClick={() => {}} />
          </AzNavRail>
          <AzPopup controller={controller} />
        </>
      );
    };
    const { getByTestId } = await render(<Harness />);
    const borderColorOf = () =>
      (getByTestId('sync').parent!.props.style as { borderColor?: string })
        .borderColor;

    expect(borderColorOf()).toBe(azItemAlertColor(AzItemAlert.WARNING));

    await act(async () => fireEvent.press(getByTestId('az-popup-scrim')));

    expect(borderColorOf()).not.toBe(azItemAlertColor(AzItemAlert.WARNING));
  });

  it('writes a badge onto the bound rail item via the item handle', async () => {
    let handleRef: { setBadge: (t: string | null) => void } | null = null;
    const Harness = () => {
      const controller = useAzPopupController();
      React.useEffect(() => {
        controller.show({ itemId: 'sync', title: 'Syncing' });
      }, [controller]);
      return (
        <>
          <AzNavRail>
            <AzRailItem id="sync" text="Sync" onClick={() => {}} />
          </AzNavRail>
          <AzPopup controller={controller}>
            {(scope) => {
              handleRef = scope.item ?? null;
              return null;
            }}
          </AzPopup>
        </>
      );
    };
    const { queryByText } = await render(<Harness />);
    expect(handleRef).not.toBeNull();
    await act(async () => handleRef!.setBadge('3'));
    expect(queryByText('3')).not.toBeNull();
    await act(async () => handleRef!.setBadge(null));
    expect(queryByText('3')).toBeNull();
  });
});
