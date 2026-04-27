const fs = require('fs');

let navrail = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');

// I replaced `onDismiss: () => void` with ``, leaving `  ;` and messing up the props definition:
// const TutorialOverlayWrapper: React.FC<{
//   tutorials?: Record<string, import('./types').AzTutorial>;
//   itemBounds: Record<string, any>;
//   ;
// }> = ({ tutorials, itemBounds }) => {
navrail = navrail.replace(/  itemBounds: Record<string, any>;\n  ;\n\}>/, '  itemBounds: Record<string, any>;\n}>');
fs.writeFileSync('aznavrail-react/src/AzNavRail.tsx', navrail);
