with open('aznavrail-react/src/AzNavRailScope.tsx', 'r') as f:
    content = f.read()

# Fix the useAzItem infinite loop issue
# Instead of useAzItem keeping state with useEffect + deep comparisons
import re

new_useAzItem = """
const useAzItem = (item: AzNavItem) => {
  const context = useContext(AzNavRailContext);

  // Create a ref to hold the current item reference that we registered
  const registeredItemRef = useRef<AzNavItem | null>(null);

  useEffect(() => {
    if (!context) return;

    // We do a structural check of the item since we can't rely on referential identity
    const current = registeredItemRef.current;

    const isSame = current &&
                   current.id === item.id &&
                   current.text === item.text &&
                   current.disabled === item.disabled &&
                   current.isChecked === item.isChecked &&
                   current.selectedOption === item.selectedOption &&
                   current.menuText === item.menuText &&
                   current.menuToggleOnText === item.menuToggleOnText &&
                   current.menuToggleOffText === item.menuToggleOffText &&
                   current.textColor === item.textColor &&
                   current.fillColor === item.fillColor &&
                   JSON.stringify(current.options) === JSON.stringify(item.options) &&
                   JSON.stringify(current.menuOptions) === JSON.stringify(item.menuOptions) &&
                   current.shape === item.shape &&
                   current.color === item.color &&
                   current.info === item.info &&
                   current.isRelocItem === item.isRelocItem &&
                   JSON.stringify(current.hiddenMenu) === JSON.stringify(item.hiddenMenu);

    if (!isSame) {
        context.register(item);
        registeredItemRef.current = item;
    }
  }); // Run on every render to check if props structurally changed, avoiding heavy deep equal checks where possible

  useEffect(() => {
      if (context) {
          return () => context.unregister(item.id);
      }
      return undefined;
  }, [context, item.id]);
};
"""

content = re.sub(r'const useAzItem = \(item: AzNavItem\) => \{.*?\};', new_useAzItem, content, flags=re.DOTALL)

with open('aznavrail-react/src/AzNavRailScope.tsx', 'w') as f:
    f.write(content)
