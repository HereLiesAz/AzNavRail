import React from 'react';
import { View, StyleSheet, ScrollView, Modal, TouchableWithoutFeedback, Dimensions } from 'react-native';
import { AzNavItem, AzNestedRailAlignment } from '../types';
import { AzNavRailDefaults } from '../AzNavRailDefaults';

interface AzNestedRailPopupProps {
    visible: boolean;
    onDismiss: () => void;
    items: AzNavItem[];
    alignment: AzNestedRailAlignment;
    renderItem: (item: AzNavItem, index: number) => React.ReactNode;
    anchorPosition?: { x: number, y: number, width: number, height: number };
    dockingSide: 'LEFT' | 'RIGHT';
    helpList?: Record<string, string>;
}

export const AzNestedRailPopup: React.FC<AzNestedRailPopupProps> = ({
    visible,
    onDismiss,
    items,
    alignment,
    renderItem,
    anchorPosition,
    dockingSide = 'LEFT',
    helpList = {}
}) => {
    if (!visible) return null;

    const isHorizontal = alignment === AzNestedRailAlignment.HORIZONTAL;
    const window = Dimensions.get('window');

    const popupStyle: any = {
        position: 'absolute',
        backgroundColor: '#f9f9f9',
        borderRadius: 8,
        padding: 8,
        elevation: 5,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.25,
        shadowRadius: 3.84,
    };

    if (anchorPosition) {
        if (isHorizontal) {
            popupStyle.top = anchorPosition.y;
            if (dockingSide === 'LEFT') {
                popupStyle.left = anchorPosition.x + anchorPosition.width + 8;
            } else {
                popupStyle.right = (window.width - anchorPosition.x) + 8;
            }
            popupStyle.maxWidth = window.width * 0.8;
            popupStyle.maxHeight = AzNavRailDefaults.ButtonWidth + 16;
        } else {
            // Vertical popup centered on screen as per Android spec if possible,
            // but usually anchored next to parent
            popupStyle.top = Math.max(window.height * 0.1, anchorPosition.y - (items.length * 30));
            if (dockingSide === 'LEFT') {
                popupStyle.left = anchorPosition.x + anchorPosition.width + 8;
            } else {
                popupStyle.right = (window.width - anchorPosition.x) + 8;
            }
            popupStyle.maxHeight = window.height * 0.8;
            popupStyle.width = AzNavRailDefaults.ButtonWidth + 16;
        }
    }

    return (
        <Modal
            transparent={true}
            visible={visible}
            onRequestClose={onDismiss}
            animationType="fade"
        >
            <TouchableWithoutFeedback onPress={onDismiss}>
                <View style={styles.overlay}>
                    <TouchableWithoutFeedback>
                        <View style={popupStyle}>
                            <ScrollView
                                horizontal={isHorizontal}
                                contentContainerStyle={isHorizontal ? styles.horizontalContent : styles.verticalContent}
                            >
                                {items.map((item, index) => renderItem(item, index))}
                            </ScrollView>
                        </View>
                    </TouchableWithoutFeedback>
                </View>
            </TouchableWithoutFeedback>
        </Modal>
    );
};

const styles = StyleSheet.create({
    overlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.1)',
    },
    horizontalContent: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    verticalContent: {
        flexDirection: 'column',
        alignItems: 'center',
    }
});
