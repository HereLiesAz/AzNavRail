import React from 'react';
import { ViewStyle, ImageSourcePropType } from 'react-native';
import { AzButtonShape } from '../types';
/** Props for the shared `AzButton` component used by the main rail and components subdir. */
export interface AzButtonProps {
    /** Text label rendered inside the button. */
    text: string;
    /** Called when the button is pressed. */
    onClick: () => void;
    /** Border and default text color. */
    color?: string;
    /** Background fill color drawn inside the button shape. */
    fillColor?: string;
    /** Overrides the text color independently of the border color. */
    textColor?: string;
    /** Shape of the button container. */
    shape?: AzButtonShape;
    /** Additional style merged into the container. */
    style?: ViewStyle;
    /** When false, the button is rendered at 50% opacity and is non-interactive. */
    enabled?: boolean;
    /** When true, replaces the button content with an activity indicator. */
    isLoading?: boolean;
    /** Test identifier forwarded to the underlying `TouchableOpacity`. */
    testID?: string;
    /** When true, `content` is rendered instead of the text label. */
    hasCustomContent?: boolean;
    /**
     * Custom content rendered inside the button when `hasCustomContent` is true. May be a React
     * node (including an `<Image>` or a `react-native-svg` `<Svg>`) or an image source
     * (`require()` id / `{ uri }`). Graphics fill the shape (cover) and are clipped to it.
     */
    content?: React.ReactNode | ImageSourcePropType;
}
/** Shared touchable button with configurable shape, fill, text-color override, and optional custom content. */
export declare const AzButton: React.FC<AzButtonProps>;
//# sourceMappingURL=AzButton.d.ts.map