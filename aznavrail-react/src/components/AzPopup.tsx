import React, { useMemo, useRef, useSyncExternalStore } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { AzWindow } from './AzWindow';
import { AzButton } from './AzButton';
import { useAzAccent } from '../AzRailPalette';
import { setItemOverride, clearItemOverride } from '../services/itemOverrides';
import { AzItemAlert, azItemAlertColor } from '../types';

/** How loud an `AzPopup` is, and what it does to the rail item that raised it. */
export enum AzPopupKind {
  /** Ordinary information. The source item is left looking like itself. */
  INFO = 'INFO',
  /** A notice. The source item is flagged with the softer `AzItemAlert.NOTICE` accent. */
  NOTICE = 'NOTICE',
  /** A warning. Same flag, in the louder `AzItemAlert.WARNING` accent. */
  WARNING = 'WARNING',
}

/** One live show request, describing what the popup is about and which item raised it. */
export interface AzPopupRequest {
  /** The id of the rail item this popup is bound to. Leave unset to bind to no item at all. */
  itemId?: string;
  kind?: AzPopupKind;
  title?: string;
  message?: string;
  /** Anything else the popup body needs. Passed straight through, untouched. */
  payload?: unknown;
}

/**
 * The handle an `AzPopup` gets on the rail item that raised it — the write side of the channel a
 * popup uses to affect the item that opened it: spin its loading animation, drop a badge on it, or
 * flag it as wanting attention. Writes go through `services/itemOverrides` and are automatically
 * cleared when the popup dismisses.
 *
 * Unlike the Kotlin `AzPopupItemHandle`, this has no read side (`item: AzNavItem?`): a popup is a
 * sibling of the rail it affects (declared alongside `<AzNavRail>`, not inside it), and React has no
 * `AzHostActivityLayout`-style shared scope object yet for a sibling to read the rail's live item
 * list back out of. See `KNOWN_GAPS.md`.
 */
export interface AzPopupItemHandle {
  readonly id: string;
  setBadge(text: string | null, persistent?: boolean): void;
  setLoading(loading: boolean): void;
  setAlert(alert: AzItemAlert | null): void;
  clear(): void;
}

function createItemHandle(id: string): AzPopupItemHandle {
  return {
    id,
    setBadge: (text, persistent = false) =>
      setItemOverride(id, { badge: text, persistentBadge: persistent }),
    setLoading: (loading) => setItemOverride(id, { isLoading: loading }),
    setAlert: (alert) => setItemOverride(id, { alert }),
    clear: () => clearItemOverride(id),
  };
}

/** What an `AzPopup`'s body is given: the request that opened it, its item, and a way out. */
export interface AzPopupScope {
  kind: AzPopupKind;
  title?: string;
  message?: string;
  payload?: unknown;
  /** The rail item this popup is bound to, or undefined when it was raised with no `itemId`. */
  item?: AzPopupItemHandle;
  /** Closes the popup (and reverts anything it did to `item` that it did not make permanent). */
  dismiss(): void;
}

/**
 * Opens and closes an `AzPopup`, and names the rail item it belongs to.
 *
 * Create one with `useAzPopupController`, render an `<AzPopup controller={...}>` alongside the
 * rail, and raise it from anywhere — an item's `onClick`, a promise, a timer:
 *
 * ```tsx
 * const alerts = useAzPopupController();
 * <AzRailItem id="sync" text="Sync" onClick={() => alerts.show({ itemId: 'sync', title: 'Syncing…' })} />
 * <AzPopup controller={alerts} />
 * ```
 */
export class AzPopupController {
  private _request: AzPopupRequest | null = null;
  private readonly subscribers = new Set<() => void>();

  get request(): AzPopupRequest | null {
    return this._request;
  }

  get isVisible(): boolean {
    return this._request !== null;
  }

  /**
   * Raises the popup.
   *
   * `itemId` unset raises the popup with no bound item — there is no Android-style "last touched
   * item" fallback here, since React's `AzNavRailScope` does not publish that id to siblings.
   */
  show(request: AzPopupRequest): void {
    this._request = { kind: AzPopupKind.INFO, ...request };
    this.notify();
  }

  /** Closes the popup. */
  dismiss(): void {
    this._request = null;
    this.notify();
  }

  subscribe = (onChange: () => void): (() => void) => {
    this.subscribers.add(onChange);
    return () => {
      this.subscribers.delete(onChange);
    };
  };

  getSnapshot = (): AzPopupRequest | null => this._request;

  private notify(): void {
    this.subscribers.forEach((fn) => fn());
  }
}

/** Creates the `AzPopupController` for one popup, stable across re-renders. */
export function useAzPopupController(): AzPopupController {
  const ref = useRef<AzPopupController | null>(null);
  if (!ref.current) ref.current = new AzPopupController();
  return ref.current;
}

/**
 * The built-in popup body: the request's title and message, with a single "OK" that closes it. It
 * is what `<AzPopup>` renders when it is given no `children` render function.
 */
export const AzPopupBody: React.FC<{ scope: AzPopupScope }> = ({ scope }) => {
  const railAccent = useAzAccent();
  const accent =
    scope.kind === AzPopupKind.NOTICE
      ? azItemAlertColor(AzItemAlert.NOTICE)
      : scope.kind === AzPopupKind.WARNING
        ? azItemAlertColor(AzItemAlert.WARNING)
        : railAccent;
  return (
    <View style={styles.body}>
      {!!scope.title && (
        <Text style={[styles.title, { color: accent }]}>{scope.title}</Text>
      )}
      {!!scope.message && <Text style={styles.message}>{scope.message}</Text>}
      <AzButton onClick={scope.dismiss} text="OK" color={accent} />
    </View>
  );
};

export interface AzPopupProps {
  controller: AzPopupController;
  /** Closes the popup when the scrim behind it is tapped. Default true. */
  dismissOnOutsideTap?: boolean;
  /** Custom body. Omit to render the default title/message/OK layout (`AzPopupBody`). */
  children?: (scope: AzPopupScope) => React.ReactNode;
}

/**
 * Renders one controller's live request as a centred `AzWindow` over a scrim.
 *
 * While a `NOTICE` / `WARNING` popup is up, the bound rail item (if any) is flagged via
 * `services/itemOverrides` — see `AzItemAlert` — and the flag is lifted again when the popup closes
 * or the bound item id changes, so an item never keeps an alert a popup no longer owns.
 */
export const AzPopup: React.FC<AzPopupProps> = ({
  controller,
  dismissOnOutsideTap = true,
  children,
}) => {
  const request = useSyncExternalStore(
    controller.subscribe,
    controller.getSnapshot,
    controller.getSnapshot
  );
  const railAccent = useAzAccent();

  const handle = useMemo(
    () => (request?.itemId ? createItemHandle(request.itemId) : undefined),
    [request?.itemId]
  );

  React.useEffect(() => {
    if (!handle || !request) return undefined;
    const alert =
      request.kind === AzPopupKind.NOTICE
        ? AzItemAlert.NOTICE
        : request.kind === AzPopupKind.WARNING
          ? AzItemAlert.WARNING
          : null;
    if (alert) handle.setAlert(alert);
    return () => {
      if (alert) handle.setAlert(null);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [handle, request?.kind]);

  if (!request) return null;

  const dismiss = () => controller.dismiss();
  const scope: AzPopupScope = {
    kind: request.kind ?? AzPopupKind.INFO,
    title: request.title,
    message: request.message,
    payload: request.payload,
    item: handle,
    dismiss,
  };
  const accent =
    request.kind === AzPopupKind.NOTICE
      ? azItemAlertColor(AzItemAlert.NOTICE)
      : request.kind === AzPopupKind.WARNING
        ? azItemAlertColor(AzItemAlert.WARNING)
        : railAccent;

  return (
    <Pressable
      testID="az-popup-scrim"
      style={styles.scrim}
      onPress={dismissOnOutsideTap ? dismiss : undefined}
    >
      <Pressable onPress={() => {}}>
        <AzWindow
          testID="az-popup"
          accent={accent}
          style={styles.window}
          onDismiss={dismiss}
        >
          <View style={styles.content}>
            {children ? children(scope) : <AzPopupBody scope={scope} />}
          </View>
        </AzWindow>
      </Pressable>
    </Pressable>
  );
};

const styles = StyleSheet.create({
  scrim: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.45)',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
  },
  window: {
    minWidth: 240,
    maxWidth: 420,
  },
  content: {
    padding: 24,
  },
  body: {
    alignItems: 'center',
    gap: 16,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  message: {
    fontSize: 16,
    textAlign: 'center',
  },
});

export default AzPopup;
