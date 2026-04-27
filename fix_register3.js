const fs = require('fs');

let aznavrailPath = 'aznavrail-react/src/AzNavRail.tsx';
let aznavrailCode = fs.readFileSync(aznavrailPath, 'utf8');

// I will add a Ref to keep track of ALL IDs seen in the current mount.
aznavrailCode = aznavrailCode.replace(
  /const dividerCounter = useRef\(0\);/,
  `const dividerCounter = useRef(0);
  const activeIds = useRef(new Set<string>());`
);

aznavrailCode = aznavrailCode.replace(
  /const checkId = useCallback\(\(id: string\) => \{\n\s*if \(items\.some\(i => i\.id === id\)\) \{\n\s*throw new Error\(\`Item with id \$\{id\} already exists\`\);\n\s*\}\n\s*\}, \[items\]\);/,
  `const checkId = useCallback((id: string) => {
    if (activeIds.current.has(id)) {
        throw new Error(\`Duplicate ID detected: \${id}\`);
    }
    activeIds.current.add(id);
  }, []);`
);

aznavrailCode = aznavrailCode.replace(
  /const unregister = useCallback\(\(id: string\) => \{/,
  `const unregister = useCallback((id: string) => {
    activeIds.current.delete(id);`
);

fs.writeFileSync(aznavrailPath, aznavrailCode);

let scopePath = 'aznavrail-react/src/AzNavRailScope.tsx';
let scopeCode = fs.readFileSync(scopePath, 'utf8');

// Actually, we must throw IMMEDIATELY inside the hook body.
let hookRegex = /const previousItem = useRef<AzNavItem \| null>\(null\);/m;
let hookReplacement = `const previousItem = useRef<AzNavItem | null>(null);

  // Validate duplicate IDs synchronously
  if (context && !previousItem.current) {
      context.checkId(item.id);
  }
`;
scopeCode = scopeCode.replace(hookRegex, hookReplacement);

// remove the old one
let oldCheckRegex = /if \(context && !previousItem\.current\) \{\n\s*context\.checkId\(item\.id\);\n\s*\}/m;
scopeCode = scopeCode.replace(oldCheckRegex, '');

fs.writeFileSync(scopePath, scopeCode);
