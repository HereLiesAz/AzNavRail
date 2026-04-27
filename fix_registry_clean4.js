const fs = require('fs');

let aznavrailPath = 'aznavrail-react/src/AzNavRail.tsx';
let aznavrailCode = fs.readFileSync(aznavrailPath, 'utf8');

// The reviewer noted two critical flaws:
// 1. Host Validation Crash: Children run useEffect before parents, so throwing on `!hostExists` crashes because the parent hasn't registered yet!
// 2. Broken Duplicate ID Check: activeIds cleared on render, but useEffects run once.
// "To fix this, validation must be deferred until after the entire tree has mounted, or handled via static analysis/prop checking rather than during state updates."

// So I will remove `hostExists` check from `register` and `activeIds` duplicate check.

aznavrailCode = aznavrailCode.replace(
  /if \(item\.isSubItem && item\.hostId\) \{\n\s*const hostExists = prev\.some\(i => i\.id === item\.hostId\);\n\s*if \(\!hostExists\) \{\n\s*throw new Error\(\`Host with ID \$\{item\.hostId\} not found\`\);\n\s*\}\n\s*\}/m,
  ''
);

aznavrailCode = aznavrailCode.replace(
  /const activeIds = useRef\(new Set<string>\(\)\);\n\s*activeIds\.current\.clear\(\); \/\/ Clear on each render pass to allow re-registration of the same valid components, but catch duplicates within the same tree./m,
  ''
);

aznavrailCode = aznavrailCode.replace(
  /const checkId = useCallback\(\(id: string\) => \{\n\s*\/\/ In React, strict synchronous duplicate ID checking during render can be tricky\n\s*\/\/ because StrictMode renders twice and unmounts aren't synchronous\.\n\s*\/\/ However, for perfect parity, we throw immediately on duplicate ID definition\.\n\s*if \(activeIds\.current\.has\(id\)\) \{\n\s*throw new Error\(\`Duplicate ID detected: \$\{id\}\`\);\n\s*\}\n\s*activeIds\.current\.add\(id\);\n\s*\}, \[\]\);/m,
  ''
);

aznavrailCode = aznavrailCode.replace(
  /const unregister = useCallback\(\(id: string\) => \{\n\s*activeIds\.current\.delete\(id\);/m,
  `const unregister = useCallback((id: string) => {`
);

aznavrailCode = aznavrailCode.replace(
  /<AzNavRailContext\.Provider value=\{\{ register, unregister, updateSettings, defaultShape: config\.defaultShape, getDividerId, checkId \}\}>/m,
  `<AzNavRailContext.Provider value={{ register, unregister, updateSettings, defaultShape: config.defaultShape, getDividerId }}>`
);

fs.writeFileSync(aznavrailPath, aznavrailCode);

let scopePath = 'aznavrail-react/src/AzNavRailScope.tsx';
let scopeCode = fs.readFileSync(scopePath, 'utf8');

// I will do duplicate ID checks and Host ID checks INSIDE useEffect but deferred!
// Wait! Actually, the reviewer said "handled via static analysis/prop checking rather than during state updates."
// What does that mean?
// Maybe just log a warning? No, perfect parity said "Add immediate throw logic for duplicate IDs, blank text, invalid cycler options, and missing hosts in useAzItem and the register function."
// BUT it crashes. How do we do it without crashing?
// Maybe throw in a `useEffect` inside `AzNavRailInner` that validates the ENTIRE `items` array AFTER render?
// Yes! If we do it in a `useEffect` inside `AzNavRailInner` whenever `items` changes, we can validate the whole tree.

scopeCode = scopeCode.replace(
  /if \(context\) \{\n\s*context\.checkId\?\.\(item\.id\);\n\s*\}/m,
  ''
);

scopeCode = scopeCode.replace(
  /checkId\?: \(\(id: string\) => void\) \| undefined;/m,
  ''
);

fs.writeFileSync(scopePath, scopeCode);

// Add global validation to AzNavRailInner
aznavrailCode = fs.readFileSync(aznavrailPath, 'utf8');

let validationEffect = `
  useEffect(() => {
    // Validate items globally after the tree has mounted and registered
    const ids = new Set<string>();
    items.forEach(item => {
        if (ids.has(item.id)) {
            throw new Error(\`Duplicate ID detected: \${item.id}\`);
        }
        ids.add(item.id);

        if (item.isSubItem && item.hostId) {
            const hostExists = items.some(i => i.id === item.hostId);
            if (!hostExists) {
                throw new Error(\`Host with ID \${item.hostId} not found\`);
            }
        }
    });
  }, [items]);
`;

aznavrailCode = aznavrailCode.replace(
  /useEffect\(\(\) => \{\n\s*Animated\.timing\(railWidthAnim/,
  `${validationEffect}\n  useEffect(() => {\n    Animated.timing(railWidthAnim`
);

fs.writeFileSync(aznavrailPath, aznavrailCode);
