const fs = require('fs');

let scopePath = 'aznavrail-react/src/AzNavRailScope.tsx';
let scopeCode = fs.readFileSync(scopePath, 'utf8');

scopeCode = scopeCode.replace(
  /context\.checkId\(item\.id\);/,
  `context.checkId?.(item.id);`
);

fs.writeFileSync(scopePath, scopeCode);
