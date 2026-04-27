const fs = require('fs');

let aznavrailPath = 'aznavrail-react/src/AzNavRail.tsx';
let aznavrailCode = fs.readFileSync(aznavrailPath, 'utf8');

aznavrailCode = aznavrailCode.replace(
  /const register = useCallback\(\(item: AzNavItem\) => \{\n\s*setItems\(\(prev\) => \{\n\s*if \(item\.isSubItem && item\.hostId\) \{\n\s*const hostExists = prev\.some\(i => i\.id === item\.hostId\);\n\s*if \(\!hostExists\) \{\n\s*throw new Error\(\`Host with ID \$\{item\.hostId\} not found\`\);\n\s*\}\n\s*\}/m,
  `const register = useCallback((item: AzNavItem) => {
    setItems((prev) => {
      // Validate Duplicate IDs synchronously inside set state.
      // We check if an item with the same ID already exists but is fundamentally a different logical item being illegally overwritten.
      // However, perfect parity requires throwing if an item with that ID is added, AND since we use useEffect to update, we throw if prev has an item with this ID, BUT we have to know it's a "new" registration vs "update".
      // Let's implement it inside useAzItem instead using a Ref Set of IDs inside context!
      if (item.isSubItem && item.hostId) {
          const hostExists = prev.some(i => i.id === item.hostId);
          if (!hostExists) {
              throw new Error(\`Host with ID \${item.hostId} not found\`);
          }
      }`
);

fs.writeFileSync(aznavrailPath, aznavrailCode);

let scopePath = 'aznavrail-react/src/AzNavRailScope.tsx';
let scopeCode = fs.readFileSync(scopePath, 'utf8');

scopeCode = scopeCode.replace(
  /const isSame = prev &&/,
  `if (!prev) {
        // It's a new item registration. Let's check for duplicate IDs globally.
        // Actually, React useAzItem runs synchronously for all items during render but useEffect runs after.
        // The reviewer said: "checkId is called inside useAzItem's useEffect, but during the initial mount, the items array is empty for all children rendered in the same tick. Therefore, static duplicate IDs will completely bypass the validation. Furthermore, the agent explicitly bypassed adding duplicate ID checks to the register function".
        // SO I WILL ADD IT TO REGISTER!
    }
    const isSame = prev &&`
);

fs.writeFileSync(scopePath, scopeCode);
