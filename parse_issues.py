import sys

def parse():
    print("Issues identified:")
    print("1. AzRailRelocItem hidden menu ID collision: uses `hidden_${hiddenMenuItems.length}` instead of parent ID prefixed.")
    print("2. AzDivider randomized ID on mount: breaks Jest snapshots. `useRef(\"divider-${Math.random()}\")`.")
    print("3. AzSettings, AzTheme, AzConfig, AzAdvanced useEffect dependency array ignores functions because of JSON.stringify.")
    print("4. useAzItem isSame misses onRelocate check.")
    print("5. classifiers and activeClassifiers should be Set<string> rather than string[].")
    print("6. AzHelpSubItem missing hostId existence check against registered items.")
    print("7. AzNestedRail incorrectly uses a consumer and global Provider without scoped map merging.")

if __name__ == '__main__':
    parse()
