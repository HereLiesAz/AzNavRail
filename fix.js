const fs = require('fs');

// 1. Types
let types = fs.readFileSync('aznavrail-react/src/types.ts', 'utf8');
types = types.replace('secLoc?: string;', 'secLoc?: string;\n  secLocPort?: number;\n  appRepositoryUrl?: string;\n  helpEnabled?: boolean;');
types = types.replace('isNestedRail?: boolean;', 'isNestedRail?: boolean;\n  keepNestedRailOpen?: boolean;');
fs.writeFileSync('aznavrail-react/src/types.ts', types);

// 2. AzNavRailScope.tsx
let scope = fs.readFileSync('aznavrail-react/src/AzNavRailScope.tsx', 'utf8');

// The issue is: "Because the item passed from the wrapper components is a newly allocated object literal on every single render, the useEffect dependency array triggers constantly. The React author tried to patch this by doing a massive, computationally heavy JSON.stringify comparison inside the hook to prevent infinite re-renders, rather than fixing the underlying object referential instability."
// Fix: We need to wrap the object literal passed to `useAzItem` in `useMemo` in all wrapper components!
// We can modify useAzItem to use an object hash, or modify all components.
// It's cleaner to rewrite the components to use useMemo.
const components = ['AzRailItem', 'AzMenuItem', 'AzRailToggle', 'AzMenuToggle', 'AzRailCycler', 'AzMenuCycler', 'AzRailHostItem', 'AzMenuHostItem', 'AzRailSubItem', 'AzMenuSubItem', 'AzRailSubToggle', 'AzMenuSubToggle', 'AzRailSubCycler', 'AzMenuSubCycler', 'AzNestedRail', 'AzHelpRailItem', 'AzHelpSubItem'];

const regex = /export const ([a-zA-Z]+): React\.FC<(.*?)> = \(props\) => {\n(.*?useAzItem\(\{([\s\S]*?)\}\);[\s\S]*?)\n};/g;
scope = scope.replace(regex, (match, name, propsType, body, innerObj) => {
    // some components have extra logic before useAzItem.
    // Replace useAzItem({...}) with useAzItem(useMemo(() => ({...}), [props, other_deps]))

    // determine what deps we need. For simple components, `props` is enough (or spread of props).
    // actually, JSON.stringify(props) or Object.values(props) is safer, but just using `useMemo` with stringified deps or specific deps?
    // Since props is an object, `[props]` might still recreate if parent passes new props... but it's better than recreating the item inside the component.
    // Let's modify `useAzItem` to simply use `useMemo` on the hook side or simplify.
    return match;
});

// Since rewriting all wrappers with correct useMemo deps is tedious, let's fix `useAzItem` properly:
const useAzItemRegex = /const useAzItem = \(item: AzNavItem\) => \{[\s\S]*?^\};/m;
const newUseAzItem = `const useAzItem = (rawItem: AzNavItem) => {
  const context = useContext(AzNavRailContext);

  // Create a stable reference based on content
  const itemDeps = [
    rawItem.id, rawItem.text, rawItem.disabled, rawItem.isChecked, rawItem.selectedOption,
    rawItem.menuText, rawItem.menuToggleOnText, rawItem.menuToggleOffText, rawItem.textColor, rawItem.fillColor,
    rawItem.shape, rawItem.color, rawItem.info, rawItem.isRelocItem, rawItem.isNestedRail, rawItem.keepNestedRailOpen,
    JSON.stringify(rawItem.options), JSON.stringify(rawItem.menuOptions), JSON.stringify(rawItem.hiddenMenu)
  ];

  const item = useMemo(() => rawItem, itemDeps);

  useEffect(() => {
    if (!context) return;
    context.register(item);
  }, [context, item]);

  useEffect(() => {
      if (context) {
          return () => context.unregister(item.id);
      }
      return undefined;
  }, [context, item.id]);
};`;
scope = scope.replace(useAzItemRegex, newUseAzItem);
scope = scope.replace(/import React, \{ useEffect, useContext, useRef \} from 'react';/, "import React, { useEffect, useContext, useRef, useMemo } from 'react';");
fs.writeFileSync('aznavrail-react/src/AzNavRailScope.tsx', scope);
