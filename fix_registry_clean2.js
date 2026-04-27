const fs = require('fs');

let aznavrailPath = 'aznavrail-react/src/AzNavRail.tsx';
let aznavrailCode = fs.readFileSync(aznavrailPath, 'utf8');

aznavrailCode = aznavrailCode.replace(
  /const checkId = useCallback\(\(id: string\) => \{\n\s*if \(activeIds\.current\.has\(id\)\) \{\n\s*throw new Error\(\`Duplicate ID detected: \$\{id\}\`\);\n\s*\}\n\s*activeIds\.current\.add\(id\);\n\s*\}, \[\]\);/m,
  `const checkId = useCallback((id: string) => {
    // In React, strict synchronous duplicate ID checking during render can be tricky
    // because StrictMode renders twice and unmounts aren't synchronous.
    // However, for perfect parity, we throw immediately on duplicate ID definition.
    if (activeIds.current.has(id)) {
        throw new Error(\`Duplicate ID detected: \${id}\`);
    }
    activeIds.current.add(id);
  }, []);`
);

// We need to fix StrictMode duplicate ID issues.
// A better way to check for duplicate IDs during render is:
// We throw if we see it, BUT we clear the activeIds on each render of AzNavRail?
// Yes! If we clear activeIds on each render of AzNavRail, then during the render pass, any duplicate ID defined will throw.
// This perfectly maps to Kotlin's synchronous list building!

aznavrailCode = aznavrailCode.replace(
  /const activeIds = useRef\(new Set<string>\(\)\);/,
  `const activeIds = useRef(new Set<string>());
  activeIds.current.clear(); // Clear on each render pass to allow re-registration of the same valid components, but catch duplicates within the same tree.`
);

fs.writeFileSync(aznavrailPath, aznavrailCode);
