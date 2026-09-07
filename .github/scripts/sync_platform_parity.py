from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]

def read(path): return path.read_text(encoding='utf-8')
def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')
def replace_once(path, old, new):
    text = read(path)
    if text.count(old) != 1:
        raise RuntimeError(f'{path}: expected one marker, found {text.count(old)}: {old[:100]!r}')
    write(path, text.replace(old, new, 1))
def add_before(path, marker, block):
    text = read(path)
    if block.strip() in text:
        return
    if marker not in text:
        raise RuntimeError(f'{path}: marker not found: {marker[:100]!r}')
    write(path, text.replace(marker, block + marker, 1))

def append_once(path, marker, block):
    text = read(path)
    if marker in text:
        return
    write(path, text.rstrip() + '\n\n' + block.strip() + '\n')

# First apply the Android/CMP renderer + visibility migration already authored in the previous PR.
subprocess.run(['python', str(ROOT / '.github/scripts/apply_unattached_visibility.py')], cwd=ROOT, check=True)

react = ROOT / 'aznavrail-react/src'

types = react / 'types.ts'
visibility_enum = '''/** Reversible ways Az chrome can leave and re-enter the screen. */
export enum AzVisibilityAnimation {
  NONE = 'NONE',
  DISSOLVE = 'DISSOLVE',
  NEAREST_EDGE = 'NEAREST_EDGE',
  SWIPE_LEFT = 'SWIPE_LEFT',
}

'''
add_before(types, '/** Reusable easings for AzNavRail', visibility_enum)

replace_once(
    types,
    'export interface AzNavRailSettings {\n',
    '''export interface AzNavRailSettings {\n  /** Controlled visibility for Az chrome. App screen content remains composed and visible. */\n  visible?: boolean;\n  /** Reversible hide/show transition. */\n  visibilityAnimation?: AzVisibilityAnimation;\n  /** Duration in milliseconds, used in both directions. */\n  visibilityDurationMillis?: number;\n'''
)
replace_once(
    types,
    '  isUnattached?: boolean;\n  /** Where an `isUnattached` host parks itself. Unset on every ordinary item. */\n  unattachedAnchor?: AzUnattachedAnchor;\n}',
    '''  isUnattached?: boolean;\n  /** Where an `isUnattached` host parks itself. Unset on every ordinary item. */\n  unattachedAnchor?: AzUnattachedAnchor;\n  /** Maximum visible height before the whole host subtree scrolls. */\n  unattachedMaxHeight?: number;\n  /** Controlled visibility for this host and its subtree. */\n  unattachedVisible?: boolean;\n  unattachedVisibilityAnimation?: AzVisibilityAnimation;\n  unattachedVisibilityDurationMillis?: number;\n}'''
)
replace_once(
    types,
    'export interface AzUnattachedHostItemProps extends AzHostItemProps {\n  /** Where this host parks itself. Defaults to `AzUnattachedAnchor.OPPOSITE`. */\n  anchor?: AzUnattachedAnchor;\n}',
    '''export interface AzUnattachedHostItemProps extends AzHostItemProps {\n  /** Where this host parks itself. Defaults to `AzUnattachedAnchor.OPPOSITE`. */\n  anchor?: AzUnattachedAnchor;\n  /** Maximum visible height before the complete host subtree becomes scrollable. */\n  maxHeight?: number;\n  /** Controlled visibility; restoring true reverses the same transition. */\n  visible?: boolean;\n  visibilityAnimation?: AzVisibilityAnimation;\n  visibilityDurationMillis?: number;\n}'''
)

visibility_helper = '''import React, { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { Animated, Dimensions, LayoutChangeEvent, ViewStyle } from 'react-native';
import { AzVisibilityAnimation } from './types';

export interface AzVisibilityState {
  visible: boolean;
  animation: AzVisibilityAnimation;
  durationMillis: number;
  overrideChildren: boolean;
}

const AzVisibilityContext = createContext<AzVisibilityState>({
  visible: true,
  animation: AzVisibilityAnimation.DISSOLVE,
  durationMillis: 250,
  overrideChildren: false,
});

export const AzVisibilityProvider: React.FC<{
  visible?: boolean;
  animation?: AzVisibilityAnimation;
  durationMillis?: number;
  children?: React.ReactNode;
}> = ({ visible = true, animation = AzVisibilityAnimation.DISSOLVE, durationMillis = 250, children }) => {
  if (durationMillis < 0) throw new Error('visibility durationMillis must be >= 0');
  const value = useMemo(() => ({ visible, animation, durationMillis, overrideChildren: true }), [visible, animation, durationMillis]);
  return <AzVisibilityContext.Provider value={value}>{children}</AzVisibilityContext.Provider>;
};

export function useAzVisibility(
  visible = true,
  animation = AzVisibilityAnimation.DISSOLVE,
  durationMillis = 250,
  edgeHint?: 'left' | 'right' | 'top' | 'bottom'
) {
  const inherited = useContext(AzVisibilityContext);
  const effectiveVisible = visible && inherited.visible;
  const effectiveAnimation = inherited.overrideChildren ? inherited.animation : animation;
  const effectiveDuration = inherited.overrideChildren ? inherited.durationMillis : durationMillis;
  if (effectiveDuration < 0) throw new Error('visibility durationMillis must be >= 0');

  const progress = useRef(new Animated.Value(effectiveVisible ? 1 : 0)).current;
  const [bounds, setBounds] = useState({ x: 0, y: 0, width: 0, height: 0 });
  const screen = Dimensions.get('window');

  useEffect(() => {
    const animationHandle = Animated.timing(progress, {
      toValue: effectiveVisible ? 1 : 0,
      duration: effectiveAnimation === AzVisibilityAnimation.NONE ? 0 : effectiveDuration,
      useNativeDriver: true,
    });
    animationHandle.start();
    return () => animationHandle.stop();
  }, [effectiveVisible, effectiveAnimation, effectiveDuration, progress]);

  const nearest = edgeHint ?? (() => {
    const d = {
      left: bounds.x + bounds.width,
      right: screen.width - bounds.x,
      top: bounds.y + bounds.height,
      bottom: screen.height - bounds.y,
    };
    return (Object.keys(d) as Array<keyof typeof d>).reduce((a, b) => d[a] <= d[b] ? a : b);
  })();

  let hiddenX = 0;
  let hiddenY = 0;
  if (effectiveAnimation === AzVisibilityAnimation.SWIPE_LEFT) hiddenX = -screen.width;
  if (effectiveAnimation === AzVisibilityAnimation.NEAREST_EDGE) {
    if (nearest === 'left') hiddenX = -(bounds.x + bounds.width || screen.width);
    else if (nearest === 'right') hiddenX = screen.width - bounds.x;
    else if (nearest === 'top') hiddenY = -(bounds.y + bounds.height || screen.height);
    else hiddenY = screen.height - bounds.y;
  }

  const style: Animated.WithAnimatedObject<ViewStyle> = {
    opacity: effectiveAnimation === AzVisibilityAnimation.DISSOLVE || effectiveAnimation === AzVisibilityAnimation.NONE ? progress : 1,
    transform: [
      { translateX: progress.interpolate({ inputRange: [0, 1], outputRange: [hiddenX, 0] }) },
      { translateY: progress.interpolate({ inputRange: [0, 1], outputRange: [hiddenY, 0] }) },
    ],
  };

  return {
    style,
    pointerEvents: effectiveVisible ? ('auto' as const) : ('none' as const),
    onLayout: (event: LayoutChangeEvent) => setBounds(event.nativeEvent.layout),
  };
}
'''
write(react / 'AzVisibility.tsx', visibility_helper)

# Root rail: global visibility context plus rail-only animation. Screen content is not wrapped.
navrail = react / 'AzNavRail.tsx'
text = read(navrail)
if "AzVisibilityProvider" not in text:
    text = text.replace("import { isSafeExternalUrl } from './util/AzSafeUrl';\n", "import { isSafeExternalUrl } from './util/AzSafeUrl';\nimport { AzVisibilityProvider, useAzVisibility } from './AzVisibility';\nimport { AzVisibilityAnimation } from './types';\n", 1)
text = text.replace(
    "    dedupeAbout = true,\n  } = props;",
    "    dedupeAbout = true,\n    visible = true,\n    visibilityAnimation = AzVisibilityAnimation.DISSOLVE,\n    visibilityDurationMillis = 250,\n  } = props;",
    1,
)
marker = "  const flexDirection =\n    config.dockingSide === AzDockingSide.RIGHT ? 'row-reverse' : 'row';\n\n"
if 'const railVisibility = useAzVisibility' not in text:
    text = text.replace(marker, marker + "  const railVisibility = useAzVisibility(\n    true,\n    visibilityAnimation,\n    visibilityDurationMillis,\n    dockingSide === AzDockingSide.LEFT ? 'left' : 'right'\n  );\n\n", 1)
text = text.replace(
    "          <Animated.View\n            style={[\n              styles.railContainer,",
    "          <Animated.View\n            pointerEvents={railVisibility.pointerEvents}\n            onLayout={railVisibility.onLayout}\n            style={[\n              styles.railContainer,\n              railVisibility.style,",
    1,
)
old_export = '''export const AzNavRail: React.FC<AzNavRailProps> = (props) => {\n  return (\n    <AzGuidanceProvider>\n      <AzNavRailInner {...props} />\n    </AzGuidanceProvider>\n  );\n};'''
new_export = '''export const AzNavRail: React.FC<AzNavRailProps> = (props) => {\n  return (\n    <AzVisibilityProvider\n      visible={props.visible ?? true}\n      animation={props.visibilityAnimation ?? AzVisibilityAnimation.DISSOLVE}\n      durationMillis={props.visibilityDurationMillis ?? 250}\n    >\n      <AzGuidanceProvider>\n        <AzNavRailInner {...props} />\n      </AzGuidanceProvider>\n    </AzVisibilityProvider>\n  );\n};'''
if old_export in text:
    text = text.replace(old_export, new_export, 1)
else:
    raise RuntimeError('AzNavRail export marker not found')
write(navrail, text)

# Unattached rail: every top-level host is a capped scroll viewport and inherits global visibility.
unattached = react / 'components/AzUnattachedRail.tsx'
text = read(unattached)
text = text.replace("  View,\n  Vibration,\n", "  View,\n  Vibration,\n  ScrollView,\n  Animated,\n", 1)
if "useAzVisibility" not in text:
    text = text.replace("import { isSafeExternalUrl } from '../util/AzSafeUrl';\n", "import { isSafeExternalUrl } from '../util/AzSafeUrl';\nimport { useAzVisibility } from '../AzVisibility';\nimport { AzVisibilityAnimation } from '../types';\n", 1)
old_stack = '''const UnattachedStack: React.FC<\n  Omit<UnattachedNodeProps, 'item'> & { hosts: AzNavItem[] }\n> = ({ hosts, spacing, ...rest }) => (\n  <View style={styles.centeredColumn}>\n    {hosts.map((host, i) => (\n      <View key={host.id} style={i > 0 ? { marginTop: spacing } : undefined}>\n        <UnattachedNode item={host} spacing={spacing} {...rest} />\n      </View>\n    ))}\n  </View>\n);'''
new_stack = '''const UnattachedStack: React.FC<\n  Omit<UnattachedNodeProps, 'item'> & { hosts: AzNavItem[] }\n> = ({ hosts, spacing, ...rest }) => (\n  <View style={styles.centeredColumn}>\n    {hosts.map((host, i) => (\n      <UnattachedHostViewport key={host.id} host={host} style={i > 0 ? { marginTop: spacing } : undefined}>\n        <UnattachedNode item={host} spacing={spacing} {...rest} />\n      </UnattachedHostViewport>\n    ))}\n  </View>\n);\n\nconst UnattachedHostViewport: React.FC<{ host: AzNavItem; style?: any; children?: React.ReactNode }> = ({ host, style, children }) => {\n  const visibility = useAzVisibility(\n    host.unattachedVisible ?? true,\n    host.unattachedVisibilityAnimation ?? AzVisibilityAnimation.DISSOLVE,\n    host.unattachedVisibilityDurationMillis ?? 250\n  );\n  const safeHeight = Dimensions.get('window').height * 0.8;\n  const maxHeight = Math.min(host.unattachedMaxHeight ?? safeHeight, safeHeight);\n  return (\n    <Animated.View pointerEvents={visibility.pointerEvents} onLayout={visibility.onLayout} style={[style, visibility.style]}>\n      <ScrollView style={{ maxHeight }} contentContainerStyle={styles.centeredColumn} nestedScrollEnabled>\n        {children}\n      </ScrollView>\n    </Animated.View>\n  );\n};'''
if old_stack not in text:
    raise RuntimeError('UnattachedStack marker not found')
text = text.replace(old_stack, new_stack, 1)
# Floating hosts are rendered independently; wrap their node at the single top-level render point.
needle = '<UnattachedNode\n              item={host}'
if needle in text:
    text = text.replace(needle, '<UnattachedHostViewport host={host}>\n            <UnattachedNode\n              item={host}', 1)
    text = text.replace('              {...nodeProps}\n            />', '              {...nodeProps}\n            />\n          </UnattachedHostViewport>', 1)
write(unattached, text)

# Windows participate in host visibility and also have their own controlled visibility.
window = react / 'components/AzWindow.tsx'
text = read(window)
if "useAzVisibility" not in text:
    text = text.replace("} from 'react-native';\n", "} from 'react-native';\nimport { AzVisibilityAnimation } from '../types';\nimport { useAzVisibility } from '../AzVisibility';\n", 1)
text = text.replace('  testID?: string;\n  children?: React.ReactNode;\n}', '  testID?: string;\n  visible?: boolean;\n  visibilityAnimation?: AzVisibilityAnimation;\n  visibilityDurationMillis?: number;\n  children?: React.ReactNode;\n}', 1)
text = text.replace('  testID,\n  children,\n}) => {', '  testID,\n  visible = true,\n  visibilityAnimation = AzVisibilityAnimation.DISSOLVE,\n  visibilityDurationMillis = 250,\n  children,\n}) => {\n  const visibility = useAzVisibility(visible, visibilityAnimation, visibilityDurationMillis);', 1)
# Attach visibility to the outer Animated.View (unique testID surface).
text = text.replace('      testID={testID}\n      style={[', '      testID={testID}\n      pointerEvents={visibility.pointerEvents}\n      onLayout={(event) => { visibility.onLayout(event); handleLayout(event); }}\n      style={[\n        visibility.style,', 1)
# If this component already had onLayout={handleLayout}, remove duplicate.
text = text.replace('      onLayout={handleLayout}\n', '', 1)
write(window, text)

# Dropdown: hide/show trigger and any open modal without destroying controlled state.
dropdown = react / 'components/AzDropdownMenu.tsx'
text = read(dropdown)
if "useAzVisibility" not in text:
    text = text.replace("import { isSafeExternalUrl } from '../util/AzSafeUrl';\n", "import { isSafeExternalUrl } from '../util/AzSafeUrl';\nimport { useAzVisibility } from '../AzVisibility';\nimport { AzVisibilityAnimation } from '../types';\n", 1)
text = text.replace('  /** The menu items — `AzDropdownItem`, `AzDivider`, etc. */\n  children?: React.ReactNode;', '  visible?: boolean;\n  visibilityAnimation?: AzVisibilityAnimation;\n  visibilityDurationMillis?: number;\n  /** The menu items — `AzDropdownItem`, `AzDivider`, etc. */\n  children?: React.ReactNode;', 1)
text = text.replace('  justifyMenuItems = true,\n  children,\n}) => {', '  justifyMenuItems = true,\n  visible = true,\n  visibilityAnimation = AzVisibilityAnimation.DISSOLVE,\n  visibilityDurationMillis = 250,\n  children,\n}) => {\n  const visibility = useAzVisibility(visible, visibilityAnimation, visibilityDurationMillis);', 1)
# Trigger outer View is the first `return (<View style={style}>` in component; give it visibility.
text = text.replace('<View style={style}>', '<Animated.View pointerEvents={visibility.pointerEvents} onLayout={visibility.onLayout} style={[style, visibility.style]}>', 1)
text = text.replace('</View>\n  );\n};\n\n/**', '</Animated.View>\n  );\n};\n\n/**', 1)
write(dropdown, text)

# Keep public exports aligned.
index = react / 'index.ts'
text = read(index)
if "./AzVisibility" not in text:
    text += "\nexport { AzVisibilityProvider, useAzVisibility } from './AzVisibility';\n"
write(index, text)

# Sample apps: demonstrate the same state in every platform sample without turning the showcase into a tutorial maze.
android_sample = ROOT / 'SampleApp/src/main/java/com/hereliesaz/sampleapp/MainApp.kt'
text = read(android_sample)
if 'AzVisibilityAnimation' not in text:
    text = text.replace('import com.hereliesaz.aznavrail.model.AzNestedRailAlignment\n', 'import com.hereliesaz.aznavrail.model.AzNestedRailAlignment\nimport com.hereliesaz.aznavrail.model.AzVisibilityAnimation\n', 1)
if 'var showAzChrome by remember' not in text:
    text = text.replace('    var railIsExpanded by remember { mutableStateOf(false) }\n', '    var railIsExpanded by remember { mutableStateOf(false) }\n    var showAzChrome by remember { mutableStateOf(true) }\n', 1)
text = text.replace('        navController = navController,\n        modifier = Modifier.fillMaxSize(),', '        navController = navController,\n        modifier = Modifier.fillMaxSize(),\n        azVisible = showAzChrome,\n        azVisibilityAnimation = AzVisibilityAnimation.NEAREST_EDGE,\n        azVisibilityDurationMillis = 350,', 1)
# Make the unattached demo self-proving if present.
text = text.replace('anchor = com.hereliesaz.aznavrail.model.AzUnattachedAnchor.FLOATING,', 'anchor = com.hereliesaz.aznavrail.model.AzUnattachedAnchor.FLOATING,\n                maxHeight = 320.dp,', 1)
write(android_sample, text)

cmp_sample = ROOT / 'aznavrail-cmp-demo/src/commonMain/kotlin/com/hereliesaz/aznavrail/demo/App.kt'
text = read(cmp_sample)
if 'AzVisibilityAnimation' not in text:
    text = text.replace('import com.hereliesaz.aznavrail.model.AzUnattachedAnchor\n', 'import com.hereliesaz.aznavrail.model.AzUnattachedAnchor\nimport com.hereliesaz.aznavrail.model.AzVisibilityAnimation\n', 1)
if 'var showAzChrome by remember' not in text:
    text = text.replace('    val navController = rememberNavController()\n', '    val navController = rememberNavController()\n    var showAzChrome by remember { mutableStateOf(true) }\n', 1)
text = text.replace('        navController = navController,', '        navController = navController,\n        azVisible = showAzChrome,\n        azVisibilityAnimation = AzVisibilityAnimation.NEAREST_EDGE,\n        azVisibilityDurationMillis = 350,', 1)
text = text.replace('anchor = AzUnattachedAnchor.FLOATING,', 'anchor = AzUnattachedAnchor.FLOATING,\n            maxHeight = 320.dp,', 1)
write(cmp_sample, text)

# React showcase + PWA sample: demonstrate rail-level reversible visibility and capped unattached overflow.
for sample in [ROOT / 'aznavrail-react/showcase/src/App.tsx', ROOT / 'sample-pwa/src/App.tsx']:
    if not sample.exists():
        continue
    text = read(sample)
    if 'AzVisibilityAnimation' not in text:
        # Add to the first aznavrail-react import when possible; otherwise leave the sample intact.
        text = text.replace('AzDockingSide,', 'AzDockingSide,\n  AzVisibilityAnimation,', 1)
    text = text.replace('<AzNavRail\n', '<AzNavRail\n              visible\n              visibilityAnimation={AzVisibilityAnimation.NEAREST_EDGE}\n              visibilityDurationMillis={350}\n', 1)
    text = text.replace('<AzUnattachedHostItem ', '<AzUnattachedHostItem maxHeight={320} ', 1)
    write(sample, text)

# Documentation: canonical parity section, then mirror the bundled complete guide copies.
parity_doc = '''## Scrollable unattached rails and programmatic visibility\n\nAndroid, Compose Multiplatform, and React expose the same contract:\n\n- unattached hosts grow naturally to `maxHeight` (and never beyond the library's safe viewport cap); overflow scrolls automatically, including nested sub-items;\n- dropdown menus, unattached rails, windows, and the main rail can be controlled programmatically;\n- the whole Az chrome layer can be hidden without hiding application screen content;\n- `NONE`, `DISSOLVE`, `NEAREST_EDGE`, and `SWIPE_LEFT` are reversible: changing visibility back to true runs the same transition backwards;\n- `visibilityDurationMillis` controls both directions.\n\nKotlin:\n```kotlin\nAzHostActivityLayout(\n    navController = navController,\n    azVisible = showAz,\n    azVisibilityAnimation = AzVisibilityAnimation.NEAREST_EDGE,\n    azVisibilityDurationMillis = 350,\n) {\n    azUnattachedHostItem(\n        id = "tools",\n        text = "Tools",\n        maxHeight = 320.dp,\n        anchor = AzUnattachedAnchor.FLOATING,\n    )\n}\n```\n\nReact:\n```tsx\n<AzNavRail\n  visible={showAz}\n  visibilityAnimation={AzVisibilityAnimation.NEAREST_EDGE}\n  visibilityDurationMillis={350}\n>\n  <AzUnattachedHostItem id="tools" text="Tools" maxHeight={320} />\n</AzNavRail>\n```\n\n`AzDropdownMenu` retains its controlled `expanded` / `onExpandedChange` state and additionally accepts the visibility trio. `AzWindow` accepts the same visibility trio. Hidden surfaces stay composed, so their state and floating position survive a hide/show cycle.\n'''
for doc in [ROOT / 'README.md', ROOT / 'docs/API.md', ROOT / 'docs/DSL.md', ROOT / 'docs/AZNAVRAIL_COMPLETE_GUIDE.md', ROOT / 'aznavrail-react/README.md', ROOT / 'aznavrail-react/KNOWN_GAPS.md']:
    if doc.exists():
        append_once(doc, '## Scrollable unattached rails and programmatic visibility', parity_doc)
complete = read(ROOT / 'docs/AZNAVRAIL_COMPLETE_GUIDE.md')
write(ROOT / 'aznavrail/src/main/assets/AZNAVRAIL_COMPLETE_GUIDE.md', complete)
write(ROOT / 'aznavrail/src/main/resources/AZNAVRAIL_COMPLETE_GUIDE.md', complete)

print('Platform parity sync applied.')
