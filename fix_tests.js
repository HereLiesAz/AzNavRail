const fs = require('fs');

let tests = [
  'aznavrail-react/src/__tests__/AzNavRail.test.tsx',
  'aznavrail-react/src/__tests__/AzNavRailFull.test.tsx'
];

for(let test of tests) {
    if(!fs.existsSync(test)) continue;
    let content = fs.readFileSync(test, 'utf8');

    // Add import
    if(!content.includes("AzNavHostContext")) {
        content = content.replace(/import { AzNavRail } from '\.\.\/AzNavRail';/, "import { AzNavRail, AzNavHostContext } from '../AzNavRail';");
    }

    // Replace <AzNavRail to `<AzNavHostContext.Provider value={true}><AzNavRail`
    // And </AzNavRail> to `</AzNavRail></AzNavHostContext.Provider>`
    content = content.replace(/<AzNavRail([\s\S]*?)>/g, '<AzNavHostContext.Provider value={true}>\n<AzNavRail$1>');
    content = content.replace(/<\/AzNavRail>/g, '</AzNavRail>\n</AzNavHostContext.Provider>');

    fs.writeFileSync(test, content);
}
