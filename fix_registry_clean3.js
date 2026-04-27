const fs = require('fs');

let scopePath = 'aznavrail-react/src/AzNavRailScope.tsx';
let scopeCode = fs.readFileSync(scopePath, 'utf8');

scopeCode = scopeCode.replace(
  /if \(context && !previousItem\.current\) \{\n\s*context\.checkId\?\.const isSame = prev &&/m, // Wait, regex was messy before
  ''
);

let cleanUseAz = scopeCode.split('const isSame = prev &&');
if (cleanUseAz.length > 1) {
    // Just find `if (context && !previousItem.current)` up to `const isSame = prev &&` and replace it.
    scopeCode = scopeCode.replace(
        /if \(context && \!previousItem\.current\) \{\n\s*context\.checkId\?\.\(item\.id\);\n\s*\}\n\s*const isSame = prev &&/g,
        `if (context) {\n        context.checkId?.(item.id);\n    }\n    const isSame = prev &&`
    );
}

fs.writeFileSync(scopePath, scopeCode);
