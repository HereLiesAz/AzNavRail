const fs = require('fs');

let navrail = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');

// The Auto-Help Subversion -> config.helpEnabled isn't available from overrides if it wasn't added yet
navrail = navrail.replace(/      helpList: dslOverrides.helpList \?\? helpList,/, "      helpList: dslOverrides.helpList ?? helpList,\n      helpEnabled: dslOverrides.helpEnabled ?? false,");

// Phantom Footer crash -> We missed importing/fixing config.helpEnabled in props
const propsAdd = `  helpList?: Record<string, string>;
  helpEnabled?: boolean;
}

const AzNavRailInner: React.FC<AzNavRailProps> = (props) => {`;
navrail = navrail.replace(/  helpList\?: Record<string, string>;\n\}\n\nconst AzNavRailInner: React\.FC<AzNavRailProps> = \(props\) => \{/, propsAdd);

// Layout structural integrity enforcement check
// Android checks `LocalAzNavHostPresent`. We can check if we are in AzNavHost context.
// But there is no context in React. We could just use a quick hack like checking if parent is AzNavHost or just adding a warning when used outside
// We don't have AzNavHost scope context. But the prompt says:
// "7. The Absence of Strict Layout Enforcement
// Android: Defends its structural integrity. If AzNavRail is invoked outside of AzHostActivityLayout, it catches the missing LocalAzNavHostPresent, logs a CRITICAL ERROR, and renders a glaring red screen of death to prevent broken z-ordering and safe-zone violations.
// React: Has no wrapper enforcement. It renders into the void..."

const checkContextOld = `export const AzNavRail: React.FC<AzNavRailProps> = (props) => {
  return (
    <AzTutorialProvider>
      <AzNavRailInner {...props} />
    </AzTutorialProvider>
  );
};`;
const checkContextNew = `export const AzNavHostContext = React.createContext(false);

export const AzNavRail: React.FC<AzNavRailProps> = (props) => {
  const isHosted = useContext(AzNavHostContext);
  if (!isHosted && typeof __DEV__ !== 'undefined' && __DEV__) {
      console.error("[CRITICAL ERROR] AzNavRail used outside of AzNavHost/AzHostActivityLayout! This will cause broken z-ordering and safe-zone violations.");
      return (
          <View style={{ flex: 1, backgroundColor: 'red', alignItems: 'center', justifyContent: 'center' }}>
              <Text style={{ color: 'white', fontWeight: 'bold' }}>CRITICAL ERROR: AzNavRail must be placed inside AzNavHost</Text>
          </View>
      );
  }
  return (
    <AzTutorialProvider>
      <AzNavRailInner {...props} />
    </AzTutorialProvider>
  );
};`;
navrail = navrail.replace(checkContextOld, checkContextNew);

// Add useContext to imports in AzNavRail if it isn't there already
if (!navrail.includes("useContext(")) {
    navrail = navrail.replace(/import React, \{ useState, useCallback, useRef, useEffect, useMemo \} from 'react';/, "import React, { useState, useCallback, useRef, useEffect, useMemo, useContext } from 'react';");
}

fs.writeFileSync('aznavrail-react/src/AzNavRail.tsx', navrail);

// Now update AzNavHost to provide the context
// We also have to update the native/web AzNavHost to provide the context, wait, there is no native AzNavHost!
// The instructions said "7. The Absence of Strict Layout Enforcement ... AzHostActivityLayout ... missing LocalAzNavHostPresent".
