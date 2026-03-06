with open('./aznavrail-react-native/src/components/RailMenuItem.tsx', 'r') as f:
    content = f.read()

replacement = """    const accessibilityState: any = {};
    if (item.isToggle) accessibilityState.checked = item.isChecked;
    if (item.isHost || item.isNestedRail) accessibilityState.expanded = isExpandedHost;

    return (
        <View>
            <TouchableOpacity
                onPress={handlePress}
                style={[styles.menuItem, { paddingLeft: 16 + (depth * 16) }]}
                accessibilityRole={item.isToggle ? "switch" : "menuitem"}
                accessibilityLabel={displayText}
                accessibilityState={accessibilityState}
            >"""

content = content.replace("""    return (
        <View>
            <TouchableOpacity
                onPress={handlePress}
                style={[styles.menuItem, { paddingLeft: 16 + (depth * 16) }]}
                accessibilityRole="menuitem"
                accessibilityLabel={displayText}
                accessibilityState={{ expanded: item.isHost ? isExpandedHost : undefined }}
            >""", replacement)

with open('./aznavrail-react-native/src/components/RailMenuItem.tsx', 'w') as f:
    f.write(content)
