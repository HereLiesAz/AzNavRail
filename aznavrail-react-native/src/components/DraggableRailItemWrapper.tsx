import React, { useRef, useState, useEffect } from 'react';
import {
  View,
  PanResponder,
  Animated,
  StyleSheet,
  TouchableOpacity,
  Text,
  Modal,
} from 'react-native';
import { AzNavItem } from '../types';
import { AzButton } from './AzButton';

interface DraggableRailItemWrapperProps {
  item: AzNavItem;
  index: number;
  totalItems: number;
  onDragStart: (index: number) => void;
  onDragEnd: (index: number) => void;
  onDragMove: (dy: number, index: number) => void;
  offsetY: Animated.Value;
  style?: any;
}

export const DraggableRailItemWrapper: React.FC<DraggableRailItemWrapperProps> = ({
  item,
  index,
  onDragStart,
  onDragEnd,
  onDragMove,
  offsetY,
  style,
}) => {
  const pan = useRef(new Animated.ValueXY()).current;
  const [isDragging, setIsDragging] = useState(false);
  const [showHiddenMenu, setShowHiddenMenu] = useState(false);
  const [menuPosition, setMenuPosition] = useState({ x: 0, y: 0 });

  // Create a listener for offset updates
  useEffect(() => {
     // If this item is being displaced by another item being dragged
     // offsetY prop drives the displacement animation
  }, [offsetY]);

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => false,
      onMoveShouldSetPanResponder: (_, gestureState) => {
          // Only start drag if vertical movement is significant
          return Math.abs(gestureState.dy) > 10;
      },
      onPanResponderGrant: () => {
        setIsDragging(true);
        pan.setOffset({
           x: 0, // Constrain horizontal
           y: (pan.y as any)._value
        });
        onDragStart(index);
      },
      onPanResponderMove: (_, gestureState) => {
          // Constrain to vertical
          pan.setValue({ x: 0, y: gestureState.dy });
          onDragMove(gestureState.dy, index);
      },
      onPanResponderRelease: () => {
        pan.flattenOffset();
        setIsDragging(false);
        onDragEnd(index);

        // Reset position visually - the list reorder handles the actual move
        Animated.spring(pan, {
            toValue: { x: 0, y: 0 },
            useNativeDriver: false
        }).start();
      },
    })
  ).current;

  const handleLongPress = (event: any) => {
      // Show hidden menu
      const { pageX, pageY } = event.nativeEvent;
      setMenuPosition({ x: pageX + 60, y: pageY - 20 }); // Offset slightly
      setShowHiddenMenu(true);
  };

  const renderHiddenMenu = () => {
      if (!showHiddenMenu || !item.hiddenMenu) return null;

      return (
          <Modal
              transparent={true}
              visible={showHiddenMenu}
              onRequestClose={() => setShowHiddenMenu(false)}
          >
              <TouchableOpacity
                  style={styles.modalOverlay}
                  activeOpacity={1}
                  onPress={() => setShowHiddenMenu(false)}
              >
                  <View style={[styles.hiddenMenu, { top: menuPosition.y, left: menuPosition.x }]}>
                       {item.hiddenMenu.map((menuItem, i) => (
                           <TouchableOpacity
                               key={i}
                               style={styles.hiddenMenuItem}
                               onPress={() => {
                                   menuItem.onClick();
                                   setShowHiddenMenu(false);
                               }}
                           >
                               <Text style={styles.hiddenMenuItemText}>{menuItem.text}</Text>
                           </TouchableOpacity>
                       ))}
                  </View>
              </TouchableOpacity>
          </Modal>
      );
  };

  // Combine dragging transform with displacement transform
  const transform = [
      { translateY: Animated.add(pan.y, offsetY) },
      { scale: isDragging ? 1.1 : 1 }
  ];

  return (
    <View style={[styles.container, style, { zIndex: isDragging ? 100 : 1 }]}>
        <Animated.View
            style={{ transform }}
            {...panResponder.panHandlers}
        >
             <TouchableOpacity
                 activeOpacity={0.8}
                 onLongPress={handleLongPress}
                 onPress={() => item.onClick && item.onClick()}
                 delayLongPress={500}
             >
                <AzButton
                    text={item.text}
                    color={item.color}
                    shape={item.shape}
                    disabled={item.disabled}
                    // Pass a dummy click handler since we handle clicks on the wrapper for gesture conflict resolution
                    onClick={() => {}}
                />
             </TouchableOpacity>
        </Animated.View>
        {renderHiddenMenu()}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
      marginBottom: 8, // Standard spacing
  },
  modalOverlay: {
      flex: 1,
      backgroundColor: 'transparent',
  },
  hiddenMenu: {
      position: 'absolute',
      backgroundColor: 'white',
      borderRadius: 4,
      borderWidth: 2,
      borderColor: '#6200ee', // Primary color ideally
      padding: 8,
      minWidth: 150,
      elevation: 5,
      shadowColor: '#000',
      shadowOffset: { width: 0, height: 2 },
      shadowOpacity: 0.25,
      shadowRadius: 3.84,
  },
  hiddenMenuItem: {
      paddingVertical: 8,
      paddingHorizontal: 12,
  },
  hiddenMenuItemText: {
      fontSize: 16,
      color: 'black',
  }
});
