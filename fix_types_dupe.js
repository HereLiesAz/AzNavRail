const fs = require('fs');

let types = fs.readFileSync('aznavrail-react/src/types.ts', 'utf8');

// The replacement script must have run twice and duplicated properties. Let's deduplicate.
types = types.replace(/  secLocPort\?: number;\n  appRepositoryUrl\?: string;\n  helpEnabled\?: boolean;\n/g, '');
types = types.replace(/  keepNestedRailOpen\?: boolean;\n/g, '');

types = types.replace(/  secLoc\?: string;/, '  secLoc?: string;\n  secLocPort?: number;\n  appRepositoryUrl?: string;\n  helpEnabled?: boolean;');
types = types.replace(/  isNestedRail\?: boolean;/, '  isNestedRail?: boolean;\n  keepNestedRailOpen?: boolean;');

fs.writeFileSync('aznavrail-react/src/types.ts', types);
