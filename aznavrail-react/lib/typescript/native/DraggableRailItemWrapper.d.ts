import React from 'react';
import { Animated } from 'react-native';
import { AzNavItem } from '../types';
interface DraggableRailItemWrapperProps {
    item: AzNavItem;
    index: number;
    totalItems: number;
    onDragStart: (index: number) => void;
    onDragEnd: (index: number) => void;
    onDragMove: (dy: number, index: number) => void;
    offsetY: Animated.Value;
    style?: any;
    translucentBackground?: string;
}
export declare const DraggableRailItemWrapper: React.FC<DraggableRailItemWrapperProps>;
export {};
//# sourceMappingURL=DraggableRailItemWrapper.d.ts.map