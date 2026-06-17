import React from 'react';
import { AzButtonShape } from '../types';
import { ViewStyle } from 'react-native';
/** Props for the standalone `AzToggle` button used inside the rail to render a two-state item. */
export interface AzToggleProps {
    /** Current checked/on state. */
    isChecked: boolean;
    /** Called when the user taps the button. */
    onToggle: () => void;
    /** Label shown when `isChecked` is true. */
    toggleOnText: string;
    /** Label shown when `isChecked` is false. */
    toggleOffText: string;
    /** Border and text color. */
    color?: string;
    /** Background fill color inside the button shape. */
    fillColor?: string;
    /** Button shape. */
    shape?: AzButtonShape;
    /** Optional style merged into the container. */
    style?: ViewStyle;
    /** When true, the button is rendered at 50% opacity and is non-interactive. */
    disabled?: boolean;
    /** Test identifier forwarded to the underlying touchable. */
    testID?: string;
}
/** Two-state toggle button — internally an `AzButton` whose label is driven by `isChecked`. */
export declare const AzToggle: React.FC<AzToggleProps>;
//# sourceMappingURL=AzToggle.d.ts.map