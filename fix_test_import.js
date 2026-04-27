const fs = require('fs');

let file = fs.readFileSync('aznavrail-react/src/AzNavRail.tsx', 'utf8');
if (!file.includes('useContext')) {
    console.log("Still no useContext import in AzNavRail.tsx!");
}
console.log(file.split('\n')[0]);
