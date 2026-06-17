import React from 'react';
import { AzButtonShape } from '../types';
import { ViewStyle } from 'react-native';
/** Props for the standalone `AzCycler` button used inside the rail to step through a list of options. */
export interface AzCyclerProps {
    /** Ordered list of option labels. */
    options: string[];
    /** Currently selected option (must be a member of `options`). */
    selectedOption: string;
    /**
     * Called with the next option after a one-second debounce, so the user can rapidly
     * tap-preview values without firing a callback for each tap.
     */
    onCycle: (option: string) => void;
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
    /** Subset of `options` that should be skipped while cycling. */
    disabledOptions?: string[];
    /** Test identifier forwarded to the underlying touchable. */
    testID?: string;
}
/**
 * Button that cycles forward through `options` on each tap with a 1 s commit debounce
 * before `onCycle` is invoked with the final selected value.
 */
export declare const AzCycler: React.FC<AzCyclerProps>;
//# sourceMappingURL=AzCycler.d.ts.map