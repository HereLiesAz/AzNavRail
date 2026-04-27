const fs = require('fs');

let navrail = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');

// There are duplicate assignments of `helpList` and `helpEnabled`. Let's just fix it completely.
// Let's replace the whole config map declaration.
const configMatch = /const config = \{\n[\s\S]*?\n  \};/;
const newConfig = `const config = {
      displayAppNameInHeader: dslOverrides.displayAppNameInHeader ?? displayAppNameInHeader,
      packRailButtons: dslOverrides.packRailButtons ?? props.packRailButtons,
      expandedRailWidth: dslOverrides.expandedRailWidth ?? expandedRailWidth,
      collapsedRailWidth: dslOverrides.collapsedRailWidth ?? collapsedRailWidth,
      showFooter: dslOverrides.showFooter ?? showFooter,
      isLoading: dslOverrides.isLoading ?? isLoading,
      defaultShape: dslOverrides.defaultShape ?? defaultShape,
      enableRailDragging: dslOverrides.enableRailDragging ?? enableRailDragging,
      dockingSide: dslOverrides.dockingSide ?? dockingSide,
      noMenu: dslOverrides.noMenu ?? noMenu,
      infoScreen: dslOverrides.infoScreen ?? infoScreen,
      activeColor: dslOverrides.activeColor ?? activeColor,
      headerIconShape: dslOverrides.headerIconShape ?? headerIconShape,
      translucentBackground: dslOverrides.translucentBackground ?? translucentBackground,
      vibrate: dslOverrides.vibrate ?? vibrate,
      onItemGloballyPositioned: dslOverrides.onItemGloballyPositioned,
      helpList: dslOverrides.helpList ?? props.helpList ?? {},
      helpEnabled: dslOverrides.helpEnabled ?? props.helpEnabled ?? false,
      tutorials: dslOverrides.tutorials ?? props.tutorials,
  };`;
navrail = navrail.replace(configMatch, newConfig);

// `onDismiss` is declared but its value is never read in `AzNavRail.tsx` inside `TutorialOverlayWrapper`
navrail = navrail.replace(/onDismiss: \(\) => void/g, '');
navrail = navrail.replace(/, onDismiss \}/g, ' }');

fs.writeFileSync('aznavrail-react/src/AzNavRail.tsx', navrail);

let btn = fs.readFileSync('aznavrail-react/src/components/AzButton.tsx', 'utf8');
// remove multiple style attribute:
btn = btn.replace(/style=\{styles\.button\} style=\{styles\.button\}/g, 'style={styles.button}');
// remove duplicate onPress:
btn = btn.replace(/onPress=\{onClickOverride \|\| handleClick\} onPress=\{onClickOverride \|\| handleClick\}/g, 'onPress={onClickOverride || handleClick}');
// check if there is an explicit double attribute using regex:
btn = btn.replace(/([a-zA-Z]+)=\{([^}]+)\} \1=\{\2\}/g, '$1={$2}');
fs.writeFileSync('aznavrail-react/src/components/AzButton.tsx', btn);
