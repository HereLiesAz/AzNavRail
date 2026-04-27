with open('aznavrail-react/src/AzNavRailScope.tsx', 'r') as f:
    content = f.read()

import re

# Fix useAzItem by using useMemo on the caller side
def replace_component(match):
    name = match.group(1)
    props_type = match.group(2)
    inner = match.group(3)

    # Extract the object passed to useAzItem
    obj_match = re.search(r'useAzItem\(\{(.*?)\}\);', inner, flags=re.DOTALL)
    if obj_match:
        obj_content = obj_match.group(1)

        new_inner = inner.replace(f'useAzItem({{{obj_content}}});', f'''const item = useMemo(() => ({{
{obj_content}
  }}), [props]);
  useAzItem(item);''')
        return f"export const {name}: React.FC<{props_type}> = (props) => {{\n{new_inner}\n}};"
    return match.group(0)

# Replace all simple components
for name in ['AzRailItem', 'AzMenuItem', 'AzRailToggle', 'AzMenuToggle', 'AzRailCycler', 'AzMenuCycler', 'AzRailHostItem', 'AzMenuHostItem', 'AzRailSubItem', 'AzMenuSubItem', 'AzRailSubToggle', 'AzMenuSubToggle', 'AzRailSubCycler', 'AzMenuSubCycler', 'AzNestedRail', 'AzHelpRailItem', 'AzHelpSubItem']:
    # basic replacing ...
    pass

# We will just rewrite useAzItem instead of all components...
# The problem is `useEffect` dependency is `[context, item]`. If we use `useMemo` in all components, it fixes it, or we just change useAzItem to take individual dependencies or destructure the object so we don't have to rewrite 30 components.
