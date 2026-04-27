const fs = require('fs');

let navrail = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');
navrail = navrail.replace(/      onItemGloballyPositioned: dslOverrides\.onItemGloballyPositioned,\n      helpList: dslOverrides\.helpList \?\? helpList,\n      helpEnabled: dslOverrides\.helpEnabled \?\? false,\n      tutorials: dslOverrides\.tutorials \?\? props\.tutorials,\n/, '');
const correctMap = `      onItemGloballyPositioned: dslOverrides.onItemGloballyPositioned,
      helpList: dslOverrides.helpList ?? props.helpList ?? {},
      helpEnabled: dslOverrides.helpEnabled ?? props.helpEnabled ?? false,
      tutorials: dslOverrides.tutorials ?? props.tutorials,`;

// Ensure we don't have multiple 'helpList' assignments.
navrail = navrail.replace(/      onItemGloballyPositioned: dslOverrides\.onItemGloballyPositioned,\n      helpList: dslOverrides\.helpList \?\? helpList,\n/, correctMap + '\n');
navrail = navrail.replace(/      helpList = \{\},\n/, ''); // remove from props destructuring if it causes conflict
fs.writeFileSync('aznavrail-react/src/AzNavRail.tsx', navrail);

let btn = fs.readFileSync('aznavrail-react/src/components/AzButton.tsx', 'utf8');
btn = btn.replace(/style=\{styles\.button\} style=\{styles\.button\}/g, 'style={styles.button}');
fs.writeFileSync('aznavrail-react/src/components/AzButton.tsx', btn);
