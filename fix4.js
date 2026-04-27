const fs = require('fs');

let webHost = fs.readFileSync('aznavrail-react/src/web/AzNavHost.jsx', 'utf8');
webHost = webHost.replace(/import AzNavRail from '.\/AzNavRail';/, "import AzNavRail from './AzNavRail';\nimport { AzNavHostContext } from '../AzNavRail';");
webHost = webHost.replace(/<AzNavRail \{\.\.\.railProps\} \/>/, "<AzNavHostContext.Provider value={true}>\n                     <AzNavRail {...railProps} />\n                 </AzNavHostContext.Provider>");
fs.writeFileSync('aznavrail-react/src/web/AzNavHost.jsx', webHost);

// Make sure AzNavRail export context
let navrail = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');
if(!navrail.includes("export const AzNavHostContext")) {
   console.log("Context export not found! We probably messed up the replace.");
}
