import React, { useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import { AzNavItem, AzTutorial } from '../types';
import { useAzTutorialController } from '../tutorial/AzTutorialController';

interface HelpOverlayProps {
    items: AzNavItem[];
    onDismiss: () => void;
    helpList: Record<string, string>;
    itemBounds: Record<string, { x: number, y: number, width: number, height: number }>;
    nestedRailVisibleId?: string | null;
    tutorials?: Record<string, AzTutorial>;
}

export const HelpOverlay: React.FC<HelpOverlayProps> = ({ items, onDismiss, helpList, itemBounds, nestedRailVisibleId = null, tutorials = {} }) => {
    const [expandedItemId, setExpandedItemId] = useState<string | null>(null);
    const [cardBounds, setCardBounds] = useState<Record<string, { x: number, y: number, width: number, height: number }>>({});
    const tutorialController = useAzTutorialController();

    const isNestedRailOpen = nestedRailVisibleId !== null;
    const paddingLeft = isNestedRailOpen ? 240 : 120;

    const allItems = React.useMemo(() => {
        const list = [...items];
        if (nestedRailVisibleId) {
            const nestedHost = items.find(i => i.id === nestedRailVisibleId);
            if (nestedHost?.nestedRailItems) {
                list.push(...nestedHost.nestedRailItems);
            }
        }
        return list;
    }, [items, nestedRailVisibleId]);

    const [scrollY, setScrollY] = useState(0);

    const itemsWithInfo = React.useMemo(() => {
        return allItems.filter(i => {
            const infoText = i.info?.trim();
            const listText = helpList?.[i.id]?.trim();
            return infoText || listText;
        });
    }, [allItems, helpList]);

    return (
        <View style={styles.overlay}>
            {/* Draw Lines */}
            <View style={StyleSheet.absoluteFill} pointerEvents="none">
                {itemsWithInfo.map(item => {
                    const navBounds = itemBounds[item.id];
                    const descBounds = cardBounds[item.id];

                    if (navBounds && descBounds) {
                        const startX = navBounds.x + navBounds.width;
                        const startY = navBounds.y + navBounds.height / 2;
                        const endX = descBounds.x;
                        // Apply scroll offset to description card Y coordinate for lines
                        const endY = descBounds.y + descBounds.height / 2 - scrollY;
                        const elbowX = (startX + endX) / 2;

                        return (
                            <React.Fragment key={`line-${item.id}`}>
                                {/* Horizontal segment from button to elbow */}
                                <View style={{
                                    position: 'absolute',
                                    left: Math.min(startX, elbowX),
                                    top: startY,
                                    width: Math.abs(elbowX - startX),
                                    height: 2,
                                    backgroundColor: 'yellow'
                                }} />
                                {/* Vertical segment from elbow to desc Y */}
                                <View style={{
                                    position: 'absolute',
                                    left: elbowX,
                                    top: Math.min(startY, endY),
                                    width: 2,
                                    height: Math.abs(endY - startY),
                                    backgroundColor: 'yellow'
                                }} />
                                {/* Horizontal segment from elbow to desc */}
                                <View style={{
                                    position: 'absolute',
                                    left: Math.min(elbowX, endX),
                                    top: endY,
                                    width: Math.abs(endX - elbowX),
                                    height: 2,
                                    backgroundColor: 'yellow'
                                }} />
                            </React.Fragment>
                        );
                    }
                    return null;
                })}
            </View>

            {/* Background Tap to Dismiss */}
            <TouchableOpacity style={StyleSheet.absoluteFill} onPress={onDismiss} activeOpacity={1} />

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={[styles.scrollContent, { paddingLeft }]}
                scrollEventThrottle={16}
                onScroll={(e) => setScrollY(e.nativeEvent.contentOffset.y)}
            >
                {itemsWithInfo.map(i => {
                    const infoText = i.info?.trim();
                    const listText = helpList?.[i.id]?.trim();
                    const titleText = i.text?.trim() || `Item ${i.id}`;
                    const isExpanded = expandedItemId === i.id;

                    const hasTutorial = !!tutorials[i.id];

                    return (
                        <TouchableOpacity
                            key={i.id}
                            style={styles.card}
                            activeOpacity={0.8}
                            onPress={() => setExpandedItemId(isExpanded ? null : i.id)}
                            onLayout={(e) => {
                                const layout = e.nativeEvent.layout;
                                setCardBounds(prev => ({
                                    ...prev,
                                    [i.id]: layout
                                }));
                            }}
                        >
                            <Text style={styles.cardTitle}>{titleText}</Text>
                            {infoText && (
                                <Text
                                    style={styles.cardText}
                                    numberOfLines={isExpanded ? undefined : 1}
                                >
                                    {infoText}
                                </Text>
                            )}
                            {listText && (
                                <Text
                                    style={[styles.cardText, infoText ? { marginTop: 8 } : {}]}
                                    numberOfLines={isExpanded ? undefined : 1}
                                >
                                    {listText}
                                </Text>
                            )}
                            {hasTutorial && !isExpanded && (
                                <Text style={styles.tutorialHint}>Tutorial available</Text>
                            )}
                            {hasTutorial && isExpanded && (
                                <TouchableOpacity
                                    style={styles.startTutorialButton}
                                    onPress={() => {
                                        tutorialController.startTutorial(i.id);
                                        onDismiss();
                                    }}
                                >
                                    <Text style={styles.startTutorialText}>Start Tutorial</Text>
                                </TouchableOpacity>
                            )}
                            {isExpanded && !hasTutorial && (
                                <Text style={styles.tapToCollapse}>Tap to collapse</Text>
                            )}
                        </TouchableOpacity>
                    );
                })}
            </ScrollView>
        </View>
    );
};

const styles = StyleSheet.create({
    overlay: {
        ...StyleSheet.absoluteFillObject,
        backgroundColor: 'rgba(0,0,0,0.7)',
        zIndex: 9999,
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingVertical: 32,
        paddingRight: 16,
    },
    card: {
        backgroundColor: '#333',
        padding: 16,
        marginBottom: 16,
        borderRadius: 8,
    },
    cardTitle: {
        color: 'yellow',
        fontWeight: 'bold',
        fontSize: 16,
        marginBottom: 4,
    },
    cardText: {
        color: 'white',
        fontSize: 14,
    },
    tapToCollapse: {
        color: 'gray',
        fontSize: 12,
        marginTop: 8,
    },
    tutorialHint: {
        color: '#aaa',
        fontSize: 11,
        marginTop: 6,
        fontStyle: 'italic',
    },
    startTutorialButton: {
        marginTop: 12,
        backgroundColor: '#6200EE',
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 16,
        alignSelf: 'flex-start',
    },
    startTutorialText: {
        color: '#fff',
        fontSize: 13,
        fontWeight: '600',
    },
});