const fs = require('fs');

// src/AzNavRail.tsx errors
let navrail = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');
navrail = navrail.replace(/      helpList: dslOverrides\.helpList \?\? helpList,\n      helpEnabled: dslOverrides\.helpEnabled \?\? false,\n      tutorials: dslOverrides\.tutorials \?\? props\.tutorials,\n/, '');
// there were duplicate declarations in config
fs.writeFileSync('aznavrail-react/src/AzNavRail.tsx', navrail);

// src/__tests__/AzNavRailFull.test.tsx errors
let fullTest = fs.readFileSync('aznavrail-react/src/__tests__/AzNavRailFull.test.tsx', 'utf8');
fullTest = fullTest.replace(/hostBtn\./g, 'hostBtn?.');
fullTest = fullTest.replace(/subBtn\./g, 'subBtn?.');
fullTest = fullTest.replace(/firstWrapper\./g, 'firstWrapper?.');
fs.writeFileSync('aznavrail-react/src/__tests__/AzNavRailFull.test.tsx', fullTest);

// src/components/AzButton.tsx(115,7): error TS17001: JSX elements cannot have multiple attributes with the same name.
let btn = fs.readFileSync('aznavrail-react/src/components/AzButton.tsx', 'utf8');
btn = btn.replace(/onPress=\{onClickOverride \|\| handleClick\} onPress=\{onClickOverride \|\| handleClick\}/g, 'onPress={onClickOverride || handleClick}');
// Or maybe it's `style` attribute duplicated
btn = btn.replace(/style=\{styles\.button\} style=\{styles\.button\}/g, 'style={styles.button}');
fs.writeFileSync('aznavrail-react/src/components/AzButton.tsx', btn);
