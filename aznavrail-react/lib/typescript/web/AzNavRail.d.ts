export default AzNavRail;
/**
 * An M3-style navigation rail that expands into a menu drawer for web applications.
 *
 * @param {object} props - The component props.
 * @param {boolean} [props.initiallyExpanded=false] - Whether the navigation rail is expanded by default.
 * @param {Array<object>} props.content - An array of navigation item objects (tree structure).
 * @param {object} [props.settings={}] - An object containing settings.
 * @param {string} [props.currentDestination] - The current route/destination ID to determine active state.
 */
declare function AzNavRail({ initiallyExpanded, content, settings, currentDestination }: {
    initiallyExpanded?: boolean | undefined;
    content: Array<object>;
    settings?: object | undefined;
    currentDestination?: string | undefined;
}): React.JSX.Element;
import React from 'react';
//# sourceMappingURL=AzNavRail.d.ts.map