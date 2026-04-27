const fs = require('fs');

let file = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');

// The error was:
// SyntaxError: /app/aznavrail-react/src/AzNavRail.tsx: Unexpected token (678:4)
// Let's check what's around line 678.
const lines = file.split('\n');
console.log(lines.slice(670, 690).join('\n'));
