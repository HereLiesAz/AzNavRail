import React, { useState, useMemo, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Dimensions } from 'react-native';
import { AzTutorial, AzHighlight } from '../types';
import { useAzTutorialController } from '../tutorial/AzTutorialController';

interface AzTutorialOverlayProps {
  tutorialId: string;
  tutorial: AzTutorial;
  onDismiss: () => void;
  itemBoundsCache: Record<string, { x: number; y: number; width: number; height: number }>;
}

export const AzTutorialOverlay: React.FC<AzTutorialOverlayProps> = ({
  tutorialId,
  tutorial,
  onDismiss,
  itemBoundsCache,
}) => {
  const [currentSceneIndex, setCurrentSceneIndex] = useState(0);
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const tutorialController = useAzTutorialController();

  useEffect(() => {
    if (currentSceneIndex >= tutorial.scenes.length) {
      tutorialController.markTutorialRead(tutorialId);
      onDismiss();
    }
  }, [currentSceneIndex, tutorial.scenes.length, tutorialId, onDismiss, tutorialController]);

  if (currentSceneIndex >= tutorial.scenes.length) {
    return null;
  }

  const currentScene = tutorial.scenes[currentSceneIndex];
  const currentCard = currentScene.cards[currentCardIndex];

  if (!currentCard) {
    return null;
  }

  const highlightBounds = useMemo(() => {
    const highlight = currentCard.highlight;
    if (!highlight) return null;

    if (highlight.type === 'Area') {
      return highlight.bounds;
    } else if (highlight.type === 'Item') {
      return itemBoundsCache[highlight.id];
    }
    return null;
  }, [currentCard.highlight, itemBoundsCache]);

  const isFullScreenHighlight = currentCard.highlight?.type === 'FullScreen';

  const window = Dimensions.get('window');

  const handleAction = () => {
    if (currentCard.onAction) {
      currentCard.onAction();
    }

    if (currentCardIndex + 1 >= currentScene.cards.length) {
      if (currentScene.onComplete) {
        currentScene.onComplete();
      }
      setCurrentSceneIndex((prev) => prev + 1);
      setCurrentCardIndex(0);
    } else {
      setCurrentCardIndex((prev) => prev + 1);
    }
  };

  const handleSkip = () => {
    tutorialController.markTutorialRead(tutorialId);
    onDismiss();
  };

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
      {/* 1. Render Scene Content underneath */}
      <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
        {currentScene.content()}
      </View>

      {/* 2. Dimmed Overlay with Cutout */}
      {!isFullScreenHighlight && (
        <View style={styles.overlayMask} pointerEvents="auto">
          {highlightBounds && (
            <View
              style={[
                styles.cutout,
                {
                  left: highlightBounds.x,
                  top: highlightBounds.y,
                  width: highlightBounds.width,
                  height: highlightBounds.height,
                },
              ]}
              pointerEvents="none"
            >
              <View style={styles.cutoutInner} />
            </View>
          )}
        </View>
      )}

      {/* 3. Card UI */}
      <View style={styles.cardContainer} pointerEvents="box-none">
        <View style={styles.card}>
          <Text style={styles.cardTitle}>{currentCard.title}</Text>
          <Text style={styles.cardText}>{currentCard.text}</Text>

          <View style={styles.buttonRow}>
            <TouchableOpacity onPress={handleSkip} style={styles.skipButton}>
              <Text style={styles.skipButtonText}>Skip Tutorial</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={handleAction} style={styles.actionButton}>
              <Text style={styles.actionButtonText}>
                {currentCard.actionText || 'Next'}
              </Text>
            </TouchableOpacity>
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
  cutout: {
    position: 'absolute',
    borderRadius: 16,
    // The "box-shadow" approach to create a transparent hole.
    // React Native doesn't perfectly support massive box shadows like web,
    // so for a true cross-platform cutout, we often use bordered absolute views.
    // We will simulate it by rendering border on the cutout inner view.
  },
  cutoutInner: {
    ...StyleSheet.absoluteFillObject,
    borderRadius: 16,
    borderColor: 'rgba(0,0,0,0.7)',
    borderWidth: 9999, // Extremely large border
    margin: -9999,     // Offset by border width
  },
  cardContainer: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'flex-end',
    alignItems: 'center',
    padding: 32,
    zIndex: 9999,
  },
  card: {
    width: '80%',
    maxWidth: 400,
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  cardTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 16,
    color: '#000',
  },
  cardText: {
    fontSize: 16,
    color: '#333',
    marginBottom: 24,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  skipButton: {
    padding: 8,
  },
  skipButtonText: {
    color: '#6200EE',
    fontSize: 14,
    fontWeight: '600',
  },
  actionButton: {
    backgroundColor: '#6200EE',
    paddingHorizontal: 24,
    paddingVertical: 10,
    borderRadius: 20,
  },
  actionButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
});
