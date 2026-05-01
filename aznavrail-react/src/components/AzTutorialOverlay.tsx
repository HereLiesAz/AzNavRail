import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity, Dimensions,
} from 'react-native';
import { AzTutorial } from '../types';
import { useAzTutorialController } from '../tutorial/AzTutorialController';

interface AzTutorialOverlayProps {
  tutorialId: string;
  tutorial: AzTutorial;
  onDismiss: () => void;
  itemBoundsCache: Record<string, { x: number; y: number; width: number; height: number }>;
}

const TAG = 'AzTutorialOverlay';

export const AzTutorialOverlay: React.FC<AzTutorialOverlayProps> = ({
  tutorialId, tutorial, onDismiss, itemBoundsCache,
}) => {
  const [currentSceneIndex, setCurrentSceneIndex] = useState(0);
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [checkedIndices, setCheckedIndices] = useState<Set<number>>(new Set());
  const visitedSceneIds = React.useRef<Set<string>>(new Set());
  const tutorialController = useAzTutorialController();
  const { currentVariables, pendingEvent } = tutorialController;
  const screenHeight = Dimensions.get('window').height;

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
    } else {
      console.warn(`[${TAG}] Scene '${id}' not found`);
    }
  }, [indexOfScene]);

  const advanceCard = useCallback(() => {
    setCheckedIndices(new Set());
    const scene = tutorial.scenes[currentSceneIndex];
    if (!scene) return;
    if (currentCardIndex + 1 >= scene.cards.length) {
      scene.onComplete?.();
      setCurrentSceneIndex((prev) => prev + 1);
      setCurrentCardIndex(0);
    } else {
      setCurrentCardIndex((prev) => prev + 1);
    }
  }, [tutorial.scenes, currentSceneIndex, currentCardIndex]);

  // Variable branching on scene change
  useEffect(() => {
    if (currentSceneIndex >= tutorial.scenes.length) return;
    const scene = tutorial.scenes[currentSceneIndex];
    const bv = scene.branchVar;
    if (bv && scene.branches && Object.keys(scene.branches).length > 0) {
      const varValue = String(currentVariables[bv] ?? '');
      const targetId = scene.branches[varValue];
      if (targetId) {
        if (visitedSceneIds.current.has(targetId)) {
          console.warn(`[${TAG}] Circular branch at '${scene.id}' → '${targetId}', advancing linearly`);
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

  // Tutorial completion
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
    const scene = tutorial.scenes[currentSceneIndex];
    const card = scene?.cards[currentCardIndex];
    if (!card) return;
    const cond = card.advanceCondition;
    if (cond?.type === 'Event' && cond.name === pendingEvent) {
      tutorialController.consumeEvent();
      advanceCard();
    }
  }, [pendingEvent]); // eslint-disable-line react-hooks/exhaustive-deps

  if (currentSceneIndex >= tutorial.scenes.length) return null;

  const currentScene = tutorial.scenes[currentSceneIndex];
  const currentCard = currentScene?.cards[currentCardIndex];
  if (!currentCard) return null;

  const highlightBounds = (() => {
    const h = currentCard.highlight;
    if (!h) return null;
    if (h.type === 'Area') return h.bounds;
    if (h.type === 'Item') return itemBoundsCache[h.id] ?? null;
    return null;
  })();

  const isFullScreen = currentCard.highlight?.type === 'FullScreen';
  const isTapAnywhere = currentCard.advanceCondition?.type === 'TapAnywhere';
  const isTapTarget = currentCard.advanceCondition?.type === 'TapTarget';
  const highlightItemId = currentCard.highlight?.type === 'Item' ? currentCard.highlight.id : null;

  const highlightCenterY = highlightBounds
    ? highlightBounds.y + highlightBounds.height / 2
    : 0;
  const cardAtTop = highlightCenterY > screenHeight * 0.6;

  const showButton =
    !currentCard.advanceCondition ||
    currentCard.advanceCondition.type === 'Button' ||
    !!currentCard.checklistItems;
  const allChecked =
    !currentCard.checklistItems ||
    checkedIndices.size === currentCard.checklistItems.length;

  const handleSkip = () => {
    tutorialController.markTutorialRead(tutorialId);
    tutorial.onSkip?.();
    onDismiss();
  };

  const handleTapTarget = () => {
    if (highlightItemId) {
      const targetSceneId = currentCard.branches?.[highlightItemId];
      if (targetSceneId) { navigateToScene(targetSceneId); return; }
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
    <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
      {/* 1. Scene content */}
      <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
        {currentScene.content()}
      </View>

      {/* 2. Dimmed overlay + cutout */}
      {!isFullScreen && (
        <View
          style={styles.overlayMask}
          pointerEvents={isTapAnywhere ? 'auto' : 'box-none'}
        >
          {isTapAnywhere && (
            <TouchableOpacity
              style={StyleSheet.absoluteFill}
              activeOpacity={1}
              onPress={advanceCard}
            />
          )}
          {highlightBounds && (
            <View
              style={[styles.cutoutOuter, {
                left: highlightBounds.x, top: highlightBounds.y,
                width: highlightBounds.width, height: highlightBounds.height,
              }]}
              pointerEvents="none"
            >
              <View style={styles.cutoutInner} />
            </View>
          )}
        </View>
      )}

      {/* 3. TapTarget clickable area */}
      {isTapTarget && highlightBounds && highlightItemId && (
        <TouchableOpacity
          style={[styles.tapTargetArea, {
            left: highlightBounds.x, top: highlightBounds.y,
            width: highlightBounds.width, height: highlightBounds.height,
          }]}
          onPress={handleTapTarget}
          activeOpacity={0.8}
        />
      )}
      {isTapTarget && !highlightItemId && (
        <TouchableOpacity style={StyleSheet.absoluteFill} onPress={advanceCard} activeOpacity={1} />
      )}

      {/* 4. Card UI */}
      <View
        style={[styles.cardContainer, cardAtTop ? styles.cardTop : styles.cardBottom]}
        pointerEvents="box-none"
      >
        <View style={styles.card}>
          <Text style={styles.cardTitle}>{currentCard.title}</Text>

          {currentCard.mediaContent && (
            <View style={styles.mediaContainer}>
              {currentCard.mediaContent()}
            </View>
          )}

          <Text style={styles.cardText}>{currentCard.text}</Text>

          {currentCard.checklistItems && (
            <View style={styles.checklist}>
              {currentCard.checklistItems.map((item, idx) => (
                <TouchableOpacity
                  key={idx}
                  style={styles.checklistRow}
                  onPress={() => toggleCheck(idx)}
                  activeOpacity={0.7}
                >
                  <View style={[styles.checkbox, checkedIndices.has(idx) && styles.checkboxChecked]}>
                    {checkedIndices.has(idx) && <Text style={styles.checkmark}>{'✓'}</Text>}
                  </View>
                  <Text style={styles.checklistText}>{item}</Text>
                </TouchableOpacity>
              ))}
            </View>
          )}

          <View style={styles.buttonRow}>
            <TouchableOpacity onPress={handleSkip} style={styles.skipButton}>
              <Text style={styles.skipButtonText}>Skip Tutorial</Text>
            </TouchableOpacity>
            {showButton && (
              <TouchableOpacity
                onPress={() => { currentCard.onAction?.(); advanceCard(); }}
                style={[styles.actionButton, !allChecked && styles.actionButtonDisabled]}
                disabled={!allChecked}
              >
                <Text style={styles.actionButtonText}>{currentCard.actionText || 'Next'}</Text>
              </TouchableOpacity>
            )}
          </View>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  overlayMask: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.7)',
    zIndex: 9998,
    overflow: 'hidden',
  },
  cutoutOuter: {
    position: 'absolute',
    borderRadius: 16,
    overflow: 'hidden',
  },
  cutoutInner: {
    ...StyleSheet.absoluteFillObject,
    borderRadius: 16,
    borderColor: 'rgba(0,0,0,0.7)',
    borderWidth: 9999,
    margin: -9999,
  },
  tapTargetArea: {
    position: 'absolute',
    borderRadius: 16,
    zIndex: 9999,
  },
  cardContainer: {
    ...StyleSheet.absoluteFillObject,
    alignItems: 'center',
    padding: 32,
    zIndex: 10000,
  },
  cardTop: { justifyContent: 'flex-start' },
  cardBottom: { justifyContent: 'flex-end' },
  card: {
    width: '85%',
    maxWidth: 420,
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 24,
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
  cardTitle: { fontSize: 20, fontWeight: 'bold', color: '#000', marginBottom: 12 },
  mediaContainer: {
    width: '100%',
    height: 120,
    borderRadius: 8,
    overflow: 'hidden',
    marginBottom: 12,
  },
  cardText: { fontSize: 16, color: '#333' },
  checklist: { marginTop: 16 },
  checklistRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
  checkbox: {
    width: 22, height: 22, borderRadius: 4,
    borderWidth: 2, borderColor: '#6200EE',
    justifyContent: 'center', alignItems: 'center', marginRight: 10,
  },
  checkboxChecked: { backgroundColor: '#6200EE' },
  checkmark: { color: '#fff', fontSize: 14, fontWeight: 'bold' },
  checklistText: { fontSize: 14, color: '#333', flex: 1 },
  buttonRow: {
    flexDirection: 'row', justifyContent: 'space-between',
    alignItems: 'center', marginTop: 24,
  },
  skipButton: { padding: 8 },
  skipButtonText: { color: '#6200EE', fontSize: 14, fontWeight: '600' },
  actionButton: {
    backgroundColor: '#6200EE',
    paddingHorizontal: 24, paddingVertical: 10, borderRadius: 20,
  },
  actionButtonDisabled: { backgroundColor: '#ccc' },
  actionButtonText: { color: '#fff', fontSize: 14, fontWeight: '600' },
});
