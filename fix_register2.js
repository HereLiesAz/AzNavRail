const fs = require('fs');

let aznavrailPath = 'aznavrail-react/src/AzNavRail.tsx';
let aznavrailCode = fs.readFileSync(aznavrailPath, 'utf8');

aznavrailCode = aznavrailCode.replace(
  /const index = prev\.findIndex\(\(i\) => i\.id === item\.id\);\n\s*if \(index >= 0\) \{/m,
  `const index = prev.findIndex((i) => i.id === item.id);
      if (index >= 0) {
        // If an item with this ID already exists, but its object reference or key fields don't match typical updates, it might be a duplicate ID from another component.
        // Wait, how do we distinguish a duplicate ID vs an update to the SAME component?
        // useAzItem only calls register when isSame is false.
        // But what if there are two DIFFERENT components rendering with the SAME ID?
        // They will BOTH call register.
        // Actually, the prompt says: "Validation: Add immediate throw logic for duplicate IDs... in useAzItem and the register function."
        // Let's add it to the register function.
        // If prev[index] exists, and we are registering, is it an update?
        // In Android, it throws IMMEDIATELY upon definition.
        // In React, we can check if there are duplicate IDs in the \`children\` prop, but that's hard.
        // Alternatively, we can use a Set inside AzNavRail to track registered IDs for the *current render cycle*!
`
);

fs.writeFileSync(aznavrailPath, aznavrailCode);

let scopePath = 'aznavrail-react/src/AzNavRailScope.tsx';
let scopeCode = fs.readFileSync(scopePath, 'utf8');

// I will use a global (or context-level) Set to track duplicate IDs synchronously.
let ctxRegex = /export const AzNavRailContext = React\.createContext<\{\n\s*register: \(item: AzNavItem\) => void;\n\s*unregister: \(id: string\) => void;\n\s*updateSettings: \(settings: any\) => void;\n\s*defaultShape\?: import\('\.\/types'\)\.AzButtonShape;\n\s*getDividerId: \(\) => string;\n\s*checkId\?: \(\(id: string\) => void\) \| undefined;\n\} \| null>\(null\);/m;

let newCtx = `export const AzNavRailContext = React.createContext<{
  register: (item: AzNavItem) => void;
  unregister: (id: string) => void;
  updateSettings: (settings: any) => void;
  defaultShape?: import('./types').AzButtonShape;
  getDividerId: () => string;
  checkId: (id: string) => void;
} | null>(null);`;
scopeCode = scopeCode.replace(ctxRegex, newCtx);

let useAzRegex = /if \(context && context\.checkId && \!previousItem\.current\) \{\n\s*context\.checkId\(item\.id\);\n\s*\}/m;
let newUseAz = `
    // Synchronously check for duplicate IDs during render!
    // useEffect is too late, but we can't throw inside useEffect for render errors reliably.
    // Actually, throwing inside the component body is exactly what Android does (throws during execution of the scope builder).
    if (context && !previousItem.current) {
        context.checkId(item.id);
    }
`;
scopeCode = scopeCode.replace(useAzRegex, newUseAz);

fs.writeFileSync(scopePath, scopeCode);
