/// <reference lib="dom" />
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { AzTutorial } from '../types';
import { useAzWebTutorialController } from './AzTutorialController';

/** Props for {@link AzWebTutorialOverlay}. */
interface AzWebTutorialOverlayProps {
  /** Unique identifier of the tutorial being played; used to mark it as read on completion. */
  tutorialId: string;
  /** The tutorial definition containing scenes, cards, and branching logic. */
  tutorial: AzTutorial;
  /** Called when the tutorial ends (completed or skipped) so the host can unmount the overlay. */
  onDismiss: () => void;
  /**
   * Pre-measured layout rectangles for nav items keyed by item ID; used instead of a live
   * `getBoundingClientRect` query when the host already has these values cached.
   */
  itemBoundsCache?: Record<string, { x: number; y: number; width: number; height: number }>;
}

/**
 * Full-screen tutorial overlay that walks the user through an {@link AzTutorial} scene by scene,
 * dimming the background with a CSS `box-shadow` punch-out around the highlighted item and
 * positioning a floating card above or below the highlight based on its vertical position on screen.
 *
 * Supports four advance conditions — `Button`, `TapTarget`, `TapAnywhere`, and `Event` — as well
 * as tap-target branching (`card.branches`), variable branching (`scene.branchVar`/`scene.branches`),
 * checklist cards with a gated Next button, and optional media cards.
 *
 * @param props.tutorialId - Identifier used to mark the tutorial read in persistent storage.
 * @param props.tutorial - The tutorial definition to render.
 * @param props.onDismiss - Callback invoked when the overlay should be removed.
 * @param props.itemBoundsCache - Optional pre-measured bounds for nav items to avoid live DOM queries.
 */
export const AzWebTutorialOverlay: React.FC<AzWebTutorialOverlayProps> = ({
  tutorialId, tutorial, onDismiss, itemBoundsCache = {},
}) => {
  const [currentSceneIndex, setCurrentSceneIndex] = useState(0);
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [checkedIndices, setCheckedIndices] = useState<Set<number>>(new Set());
  const visitedSceneIds = useRef<Set<string>>(new Set());
  const tutorialController = useAzWebTutorialController();
  const { currentVariables, pendingEvent } = tutorialController;

  const indexOfScene = useCallback(
    (id: string) => tutorial.scenes.findIndex((s) => s.id === id),
    [tutorial.scenes]
  );

  const navigateToScene = useCallback((id: string) => {
    const idx = indexOfScene(id);
    if (idx !== -1) {
      setCurrentSceneIndex(idx);
      setCurrentCardIndex(0);
      setCheckedIndices(new Set());
    }
  }, [indexOfScene]);

  const advanceCard = useCallback(() => {
    setCheckedIndices(new Set());
    const scene = tutorial.scenes[currentSceneIndex];
    if (!scene) return;
    if (currentCardIndex + 1 >= scene.cards.length) {
      scene.onComplete?.();
      setCurrentSceneIndex((p) => p + 1);
      setCurrentCardIndex(0);
    } else {
      setCurrentCardIndex((p) => p + 1);
    }
  }, [tutorial.scenes, currentSceneIndex, currentCardIndex]);

  // Variable branching
  useEffect(() => {
    if (currentSceneIndex >= tutorial.scenes.length) return;
    const scene = tutorial.scenes[currentSceneIndex];
    if (scene.branchVar && scene.branches && Object.keys(scene.branches).length > 0) {
      const varValue = String(currentVariables[scene.branchVar] ?? '');
      const targetId = scene.branches[varValue];
      if (targetId) {
        if (visitedSceneIds.current.has(targetId)) {
          const next = currentSceneIndex + 1;
          if (next >= tutorial.scenes.length) {
            tutorialController.markTutorialRead(tutorialId);
            tutorial.onComplete?.();
            onDismiss();
          } else {
            setCurrentSceneIndex(next);
            setCurrentCardIndex(0);
            setCheckedIndices(new Set());
          }
        } else {
          visitedSceneIds.current.add(scene.id);
          navigateToScene(targetId);
        }
        return;
      }
    }
    visitedSceneIds.current.add(scene.id);
  }, [currentSceneIndex]); // eslint-disable-line react-hooks/exhaustive-deps

  // Completion
  useEffect(() => {
    if (currentSceneIndex >= tutorial.scenes.length) {
      tutorialController.markTutorialRead(tutorialId);
      tutorial.onComplete?.();
      onDismiss();
    }
  }, [currentSceneIndex, tutorial.scenes.length]); // eslint-disable-line react-hooks/exhaustive-deps

  // Event-driven advance
  useEffect(() => {
    if (!pendingEvent || currentSceneIndex >= tutorial.scenes.length) return;
    const card = tutorial.scenes[currentSceneIndex]?.cards[currentCardIndex];
    if (card?.advanceCondition?.type === 'Event' && card.advanceCondition.name === pendingEvent) {
      tutorialController.consumeEvent();
      advanceCard();
    }
  }, [pendingEvent]); // eslint-disable-line react-hooks/exhaustive-deps

  if (currentSceneIndex >= tutorial.scenes.length) return null;

  const currentScene = tutorial.scenes[currentSceneIndex];
  const currentCard = currentScene?.cards[currentCardIndex];
  if (!currentCard) return null;

  const getBounds = () => {
    const h = currentCard.highlight;
    if (!h) return null;
    if (h.type === 'Area') return h.bounds;
    if (h.type === 'Item') {
      const cached = itemBoundsCache[h.id];
      if (cached) return cached;
      const el = document.querySelector(`[data-az-nav-id="${h.id}"]`);
      if (el) {
        const r = el.getBoundingClientRect();
        return { x: r.left, y: r.top, width: r.width, height: r.height };
      }
    }
    return null;
  };

  const bounds = getBounds();
  const isFullScreen = currentCard.highlight?.type === 'FullScreen';
  const isTapAnywhere = currentCard.advanceCondition?.type === 'TapAnywhere';
  const isTapTarget = currentCard.advanceCondition?.type === 'TapTarget';
  const highlightItemId = currentCard.highlight?.type === 'Item' ? currentCard.highlight.id : null;
  const showButton = !currentCard.advanceCondition || currentCard.advanceCondition.type === 'Button' || !!currentCard.checklistItems;
  const allChecked = !currentCard.checklistItems || checkedIndices.size === currentCard.checklistItems.length;

  const highlightCenterY = bounds ? bounds.y + bounds.height / 2 : 0;
  const cardAtTop = highlightCenterY > window.innerHeight * 0.6;

  const handleSkip = () => {
    tutorialController.markTutorialRead(tutorialId);
    tutorial.onSkip?.();
    onDismiss();
  };

  const handleTargetTap = () => {
    if (highlightItemId) {
      const targetId = currentCard.branches?.[highlightItemId];
      if (targetId) { navigateToScene(targetId); return; }
    }
    advanceCard();
  };

  const toggleCheck = (idx: number) => {
    setCheckedIndices((prev) => {
      const next = new Set(prev);
      if (next.has(idx)) next.delete(idx); else next.add(idx);
      return next;
    });
  };

  return (
    <div style={styles.root}>
      {/* 1. Scene content */}
      <div style={styles.sceneContent}>{currentScene.content()}</div>

      {/* 2. Dim overlay (no bounds: full dim) */}
      {!isFullScreen && !bounds && (
        <div
          style={{ ...styles.fullDim, cursor: isTapAnywhere ? 'pointer' : 'default' }}
          onClick={isTapAnywhere ? advanceCard : undefined}
        />
      )}

      {/* 3. Cutout highlight via box-shadow */}
      {!isFullScreen && bounds && (
        <>
          {/* Stray-tap absorber for TapTarget; clickable dim for TapAnywhere */}
          <div
            style={{
              position: 'fixed', inset: 0, zIndex: 9997,
              cursor: isTapAnywhere ? 'pointer' : 'default',
              pointerEvents: (isTapAnywhere || isTapTarget) ? 'auto' : 'none',
            }}
            onClick={isTapAnywhere ? advanceCard : undefined}
          />
          {/* The highlight element — its box-shadow creates the dim effect */}
          <div
            style={{
              position: 'fixed',
              left: bounds.x, top: bounds.y,
              width: bounds.width, height: bounds.height,
              borderRadius: 16,
              boxShadow: '0 0 0 9999px rgba(0,0,0,0.7)',
              zIndex: 9998,
              cursor: isTapTarget ? 'pointer' : 'default',
              pointerEvents: isTapTarget ? 'auto' : 'none',
            }}
            onClick={isTapTarget ? handleTargetTap : undefined}
          />
        </>
      )}

      {/* Full-screen dim for FullScreen highlight */}
      {isFullScreen && (
        <div
          style={{ ...styles.fullDim, cursor: isTapAnywhere ? 'pointer' : 'default' }}
          onClick={isTapAnywhere ? advanceCard : undefined}
        />
      )}

      {/* 4. Card UI */}
      <div style={{ ...styles.cardContainer, ...(cardAtTop ? styles.cardTop : styles.cardBottom) }}>
        <div style={styles.card}>
          <h3 style={styles.cardTitle}>{currentCard.title}</h3>

          {currentCard.mediaContent && (
            <div style={styles.mediaContainer}>{currentCard.mediaContent()}</div>
          )}

          <p style={styles.cardText}>{currentCard.text}</p>

          {currentCard.checklistItems && (
            <div style={styles.checklist}>
              {currentCard.checklistItems.map((item, idx) => (
                <label key={idx} style={styles.checklistRow}>
                  <input
                    type="checkbox"
                    checked={checkedIndices.has(idx)}
                    onChange={() => toggleCheck(idx)}
                    style={{ marginRight: 8, accentColor: '#6200EE' }}
                  />
                  <span style={styles.checklistText}>{item}</span>
                </label>
              ))}
            </div>
          )}

          <div style={styles.buttonRow}>
            <button onClick={handleSkip} style={styles.skipButton}>Skip Tutorial</button>
            {showButton && (
              <button
                onClick={() => { currentCard.onAction?.(); advanceCard(); }}
                style={{ ...styles.actionButton, ...(allChecked ? {} : styles.actionButtonDisabled) }}
                disabled={!allChecked}
              >
                {currentCard.actionText || 'Next'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  root: { position: 'fixed', inset: 0, zIndex: 9996, pointerEvents: 'none' },
  sceneContent: { position: 'absolute', inset: 0 },
  fullDim: {
    position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.7)',
    zIndex: 9997, pointerEvents: 'auto',
  },
  cardContainer: {
    position: 'fixed', left: 0, right: 0, zIndex: 10000,
    display: 'flex', justifyContent: 'center', padding: 32, pointerEvents: 'auto',
  },
  cardTop: { top: 0 },
  cardBottom: { bottom: 0 },
  card: {
    background: '#fff', borderRadius: 16, padding: 24,
    width: '85%', maxWidth: 420,
    boxShadow: '0 4px 24px rgba(0,0,0,0.3)',
  },
  cardTitle: { margin: '0 0 12px', fontSize: 20, fontWeight: 'bold', color: '#000' },
  mediaContainer: {
    width: '100%', height: 120, borderRadius: 8,
    overflow: 'hidden', marginBottom: 12,
  },
  cardText: { margin: '0 0 16px', fontSize: 16, color: '#333' },
  checklist: { marginBottom: 16 },
  checklistRow: { display: 'flex', alignItems: 'center', marginBottom: 8, cursor: 'pointer' },
  checklistText: { fontSize: 14, color: '#333' },
  buttonRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 },
  skipButton: {
    background: 'none', border: 'none', color: '#6200EE',
    fontSize: 14, fontWeight: 600, cursor: 'pointer', padding: '8px 0',
  },
  actionButton: {
    background: '#6200EE', color: '#fff', border: 'none',
    borderRadius: 20, padding: '10px 24px', fontSize: 14,
    fontWeight: 600, cursor: 'pointer',
  },
  actionButtonDisabled: { background: '#ccc', cursor: 'default' },
};
