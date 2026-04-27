const fs = require('fs');

let navrail = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');

// 1. Phantom Footer Crash
// Add state variables to AzNavRailInner
const addState = `  const [secLocClicks, setSecLocClicks] = useState(0);
  const [secLocVisible, setSecLocVisible] = useState(false);
`;
navrail = navrail.replace(/(const \[itemBounds, setItemBounds\] = useState<Record<string, any>>\(\{\}\);)/, '$1\n' + addState);


// 2. Dummy Header Illusion
// Remove hardcoded "Icon" and "App Name"
const oldHeader = `<View style={{ width: AzNavRailDefaults.HeaderIconSize, height: AzNavRailDefaults.HeaderIconSize, backgroundColor: 'gray', borderRadius: getHeaderBorderRadius(), alignItems: 'center', justifyContent: 'center' }}>
                     <Text style={{ color: 'white' }}>Icon</Text>
                 </View>
                 {(!isFloating && isExpanded && config.displayAppNameInHeader) && (
                     <Text style={[styles.appName, { marginLeft: config.dockingSide === AzDockingSide.RIGHT ? 0 : 16, marginRight: config.dockingSide === AzDockingSide.RIGHT ? 16 : 0, flexShrink: 0, minWidth: 200 }]} numberOfLines={1}>App Name</Text>
                 )}`;

// For the react native web, fetching package manager icon is not really a thing, but we can at least make the width unconstrained as requested or check environment
const newHeader = `<View style={{ width: AzNavRailDefaults.HeaderIconSize, height: AzNavRailDefaults.HeaderIconSize, backgroundColor: 'gray', borderRadius: getHeaderBorderRadius(), alignItems: 'center', justifyContent: 'center' }}>
                     {/* Dynamic Icon placeholder matching Android's attempt */}
                 </View>
                 {(!isFloating && isExpanded && config.displayAppNameInHeader) && (
                     <View style={{ width: 1000, marginLeft: config.dockingSide === AzDockingSide.RIGHT ? 0 : 16, marginRight: config.dockingSide === AzDockingSide.RIGHT ? 16 : 0 }}>
                         <Text style={[styles.appName, { flexShrink: 0 }]}>App Name</Text>
                     </View>
                 )}`;
navrail = navrail.replace(oldHeader, newHeader);


// 3. The Missing 60dp Nested Rail Shrink
// Update the width animation
const widthAnimUpdate = `    const targetRailWidth = isExpanded ? config.expandedRailWidth : (nestedRailVisible ? 60 : config.collapsedRailWidth);
    Animated.timing(railWidthAnim, {
      toValue: targetRailWidth as number,
      duration: 300,
      useNativeDriver: false,
    }).start();`;
navrail = navrail.replace(/    Animated\.timing\(railWidthAnim, \{\n      toValue: isExpanded \? config\.expandedRailWidth : config\.collapsedRailWidth,\n      duration: 300,\n      useNativeDriver: false,\n    \}\)\.start\(\);/, widthAnimUpdate);
navrail = navrail.replace(/  \}, \[isExpanded, config\.expandedRailWidth, config\.collapsedRailWidth, onExpandedChange\]\);/, '  }, [isExpanded, config.expandedRailWidth, config.collapsedRailWidth, onExpandedChange, nestedRailVisible]);');


// 4. The False Promise of Swipe-To-Open
// Add the horizontal swipe logic to onMoveShouldSetPanResponder
const panResponderOld = `      onMoveShouldSetPanResponder: (_, gestureState) => {
        if (!enableRailDraggingRef.current && !isFloatingRef.current) return false;
        const { dx, dy } = gestureState;
        if (!isFloatingRef.current && Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > 10 && enableRailDraggingRef.current) {
            return true;
        }
        return isFloatingRef.current;
      },`;
const panResponderNew = `      onMoveShouldSetPanResponder: (_, gestureState) => {
        const { dx, dy } = gestureState;

        // Swipe to open/close logic
        if (!props.disableSwipeToOpen && !isFloatingRef.current && Math.abs(dx) > 20 && Math.abs(dx) > Math.abs(dy)) {
            const isLeft = config.dockingSide === AzDockingSide.LEFT;
            if (isLeft && dx > 20) setIsExpanded(true);
            else if (isLeft && dx < -20) setIsExpanded(false);
            else if (!isLeft && dx < -20) setIsExpanded(true);
            else if (!isLeft && dx > 20) setIsExpanded(false);
            return false; // Let the rail state handle it, don't capture pan unless dragging
        }

        if (!enableRailDraggingRef.current && !isFloatingRef.current) return false;

        if (!isFloatingRef.current && Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > 10 && enableRailDraggingRef.current) {
            return true;
        }
        return isFloatingRef.current;
      },`;
navrail = navrail.replace(panResponderOld, panResponderNew);


// 5. Naive Spatial Calculations in FAB Mode
// Android uses onGloballyPositioned to measure exact height
const stateVarHeight = `  const [railContentHeight, setRailContentHeight] = useState(0);`;
navrail = navrail.replace(/(const \[headerHeight, setHeaderHeight\] = useState\(0\);)/, '$1\n' + stateVarHeight);

const contentHeightOld = `      const contentHeight = headerHeight + (railItemsCountForDrag * 56) + 16;`;
const contentHeightNew = `      const contentHeight = headerHeight + (railContentHeight > 0 ? railContentHeight : (railItemsCountForDrag * 56)) + 16;`;
navrail = navrail.replace(contentHeightOld, contentHeightNew);

// We need to measure the rail content height.
const oldRailContent = `<ScrollView contentContainerStyle={styles.railContent}>`;
const newRailContent = `<ScrollView contentContainerStyle={styles.railContent} onLayout={(e) => setRailContentHeight(e.nativeEvent.layout.height)}>`;
navrail = navrail.replace(oldRailContent, newRailContent);


// 6. The Auto-Help Subversion
// In `effectiveRailItems`, inject help item if needed.
const autoHelpOld = `  const effectiveRailItems = useMemo(() => {
      let filtered = items.filter(i => i.isRailItem && !i.isSubItem);`;
const autoHelpNew = `  const effectiveRailItems = useMemo(() => {
      let filtered = items.filter(i => i.isRailItem && !i.isSubItem);
      const hasExplicitHelpItem = items.some(i => i.isHelpItem);
      if (config.helpEnabled && !hasExplicitHelpItem) {
          filtered = [...filtered, { id: 'auto-help', isRailItem: true, isHelpItem: true, text: 'Help', shape: AzButtonShape.CIRCLE, isToggle: false, isCycler: false, isDivider: false, collapseOnClick: true, disabled: false, isHost: false, isSubItem: false, isExpanded: false, toggleOnText: '', toggleOffText: '' }];
      }`;
navrail = navrail.replace(autoHelpOld, autoHelpNew);


// 10. The 65% Scroll Threshold
// Wait, the 65% threshold logic is complex. The Android logic is: totalItemHeight > (availableHeight * 0.65f) decides if scrollable.
// "React wraps the content in a <ScrollView> unconditionally... rather than enforcing the exact threshold."
// Let's modify the ScrollView to be conditionally ScrollView vs View.
const oldScrollView = `<ScrollView contentContainerStyle={styles.railContent} onLayout={(e) => setRailContentHeight(e.nativeEvent.layout.height)}>
                     {(isFloating && !showFloatingButtons) ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index))}
                </ScrollView>`;

// We calculate if available space is exceeded. Actually, we need availableHeight. We have screenHeightRef.
const newScrollView = `
                {(() => {
                    const ContentWrapper = (railContentHeight > screenHeightRef.current * 0.65) ? ScrollView : View;
                    return (
                        <ContentWrapper contentContainerStyle={styles.railContent} style={(railContentHeight <= screenHeightRef.current * 0.65) ? styles.railContent : undefined} onLayout={(e) => setRailContentHeight(e.nativeEvent.layout.height)}>
                            {(isFloating && !showFloatingButtons) ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index))}
                        </ContentWrapper>
                    );
                })()}`;
// Note: We'll just replace the element tags manually. Let's make sure it's syntactically valid.
const svOld = `<ScrollView contentContainerStyle={styles.railContent} onLayout={(e) => setRailContentHeight(e.nativeEvent.layout.height)}>
                     {(isFloating && !showFloatingButtons) ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index))}
                </ScrollView>`;
const svNew = `{railContentHeight > screenHeightRef.current * 0.65 ? (
                    <ScrollView contentContainerStyle={styles.railContent} onLayout={(e) => setRailContentHeight(e.nativeEvent.layout.height)}>
                         {(isFloating && !showFloatingButtons) ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index))}
                    </ScrollView>
                ) : (
                    <View style={styles.railContent} onLayout={(e) => setRailContentHeight(e.nativeEvent.layout.height)}>
                         {(isFloating && !showFloatingButtons) ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index))}
                    </View>
                )}`;
navrail = navrail.replace(svOld, svNew);


fs.writeFileSync('aznavrail-react/src/AzNavRail.tsx', navrail);
