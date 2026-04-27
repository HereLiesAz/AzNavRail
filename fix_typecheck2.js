const fs = require('fs');

// 1. src/AzNavRail.tsx errors
let navrail = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');

// remove unused imports/variables
navrail = navrail.replace(/  Alert,\n/, '');
navrail = navrail.replace(/  findNodeHandle,\n/, '');
navrail = navrail.replace(/  const \[secLocVisible, setSecLocVisible\] = useState\(false\);\n/, '');
navrail = navrail.replace(/setSecLocVisible\(true\);/, '// setSecLocVisible(true);');
navrail = navrail.replace(/  const railItemsCount = useMemo\(\(\) => \{\n      \/\/ Includes expanded sub-items for height calculation\n      return effectiveRailItems\.length;\n  \}, \[effectiveRailItems\]\);\n/, '');

// Fix 'list' implicitly has type 'any[]'
navrail = navrail.replace(/const list = \[\];/, 'const list: AzNavItem[] = [];');

// Property 'tutorials' does not exist on type ... config
const tutorialsMap = `      onItemGloballyPositioned: dslOverrides.onItemGloballyPositioned,
      helpList: dslOverrides.helpList ?? helpList,
      helpEnabled: dslOverrides.helpEnabled ?? false,
      tutorials: dslOverrides.tutorials ?? props.tutorials,`;
navrail = navrail.replace(/      helpList: dslOverrides\.helpList \?\? helpList,\n      helpEnabled: dslOverrides\.helpEnabled \?\? false,/, tutorialsMap);


// 'onDismiss' is declared but its value is never read
navrail = navrail.replace(/const TutorialOverlayWrapper: React\.FC<\{ tutorials: Record<string, AzTutorial>, onDismiss: \(\) => void \}> = \(\{ tutorials, onDismiss \}\) => \{/, 'const TutorialOverlayWrapper: React.FC<{ tutorials: Record<string, AzTutorial> }> = ({ tutorials }) => {');
navrail = navrail.replace(/<TutorialOverlayWrapper tutorials=\{config\.tutorials\} onDismiss=\{\(\) => \{\}\} \/>/, '<TutorialOverlayWrapper tutorials={config.tutorials || {}} />');

fs.writeFileSync('aznavrail-react/src/AzNavRail.tsx', navrail);

// 2. src/AzNavRailScope.tsx
let scope = fs.readFileSync('aznavrail-react/src/AzNavRailScope.tsx', 'utf8');
scope = scope.replace(/AzItemConfig \} from '.\/types';/, "} from './types';");

// The Help items lack mandatory fields.
const helpItemProps = `...props,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: true,
        disabled: false,
        isHost: false,
        isExpanded: false,
        shape: props.shape || AzButtonShape.CIRCLE,
        toggleOnText: '',
        toggleOffText: '',`;

scope = scope.replace(/useAzItem\(\{\n        \.\.\.props,\n        isRailItem: true,\n        isHelpItem: true\n    \}\);/, `useAzItem({\n        ${helpItemProps}\n        isRailItem: true,\n        isHelpItem: true\n    } as AzNavItem);`);
scope = scope.replace(/useAzItem\(\{\n        \.\.\.props,\n        isRailItem: true,\n        isHelpItem: true,\n        isSubItem: true\n    \}\);/, `useAzItem({\n        ${helpItemProps}\n        isRailItem: true,\n        isHelpItem: true,\n        isSubItem: true\n    } as AzNavItem);`);

fs.writeFileSync('aznavrail-react/src/AzNavRailScope.tsx', scope);
