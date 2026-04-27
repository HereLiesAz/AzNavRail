const fs = require('fs');

let navrail = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');
navrail = navrail.replace(/onDismiss=\{\(\) => \{\}\}/, '');
fs.writeFileSync('aznavrail-react/src/AzNavRail.tsx', navrail);
