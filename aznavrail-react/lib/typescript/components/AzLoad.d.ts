import React from 'react';
/** Props for the `AzLoad` activity indicator. */
export interface AzLoadProps {
    /** Spinner diameter — accepts the platform `ActivityIndicator` size values or a numeric pixel size. */
    size?: number | 'small' | 'large';
    /** Spinner stroke color. Defaults to the library primary color. */
    color?: string;
}
/** Card-style activity indicator overlay used by the rail when `isLoading` is set. */
export declare const AzLoad: React.FC<AzLoadProps>;
//# sourceMappingURL=AzLoad.d.ts.map