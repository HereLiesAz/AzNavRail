const fs = require('fs');

let file = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');

// We have syntax error: `{railContentHeight > ...` inside JSX but we already opened it with `{`? No, wait:
// `) : (\n                {railContentHeight...` -> This is wrong because it's double `{` or missing parent?
// The ternary `isExpanded && !noMenu ? (...) : (...)` -> the false branch is `{railContentHeight...}` which is an object literal to babel inside JSX, or just a block inside JSX. We don't need the `{}` around it if we're already inside `{ condition ? (...) : (...) }`.
// But actually `( {rail...} )` is a syntax error because `{` opens an object.
// We should change the `) : (` to `) : ` and remove the trailing `)` or wrap in `<>...</>`.

// Let's replace the whole section to be safe
const oldSec = `                {railContentHeight > screenHeightRef.current * 0.65 ? (
                    <ScrollView contentContainerStyle={styles.railContent} onLayout={(e) => setRailContentHeight(e.nativeEvent.layout.height)}>
                         {(isFloating && !showFloatingButtons) ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index))}
                    </ScrollView>
                ) : (
                    <View style={styles.railContent} onLayout={(e) => setRailContentHeight(e.nativeEvent.layout.height)}>
                         {(isFloating && !showFloatingButtons) ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index))}
                    </View>
                )}`;

const newSec = `                railContentHeight > screenHeightRef.current * 0.65 ? (
                    <ScrollView contentContainerStyle={styles.railContent} onLayout={(e) => setRailContentHeight(e.nativeEvent.layout.height)}>
                         {(isFloating && !showFloatingButtons) ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index))}
                    </ScrollView>
                ) : (
                    <View style={styles.railContent} onLayout={(e) => setRailContentHeight(e.nativeEvent.layout.height)}>
                         {(isFloating && !showFloatingButtons) ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index))}
                    </View>
                )`;

file = file.replace(oldSec, newSec);
fs.writeFileSync('aznavrail-react/src/AzNavRail.tsx', file);
