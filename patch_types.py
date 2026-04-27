import re

with open('aznavrail-react/src/types.ts', 'r') as f:
    content = f.read()

# Add to AzNavRailSettings
content = content.replace('secLoc?: string;', 'secLoc?: string;\n  secLocPort?: number;\n  appRepositoryUrl?: string;\n  helpEnabled?: boolean;')

# Add keepNestedRailOpen to AzNavItem
content = content.replace('isNestedRail?: boolean;', 'isNestedRail?: boolean;\n  keepNestedRailOpen?: boolean;')

with open('aznavrail-react/src/types.ts', 'w') as f:
    f.write(content)
