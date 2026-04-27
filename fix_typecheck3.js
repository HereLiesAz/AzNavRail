const fs = require('fs');

if (fs.existsSync('aznavrail-react/src/__tests__/AzButton.test.tsx')) {
    let buttonTest = fs.readFileSync('aznavrail-react/src/__tests__/AzButton.test.tsx', 'utf8');
    buttonTest = buttonTest.replace(/root\.findByType\(Text\)/g, 'root.findByType(Text as any)');
    buttonTest = buttonTest.replace(/root\.findByType\(TouchableOpacity\)/g, 'root.findByType(TouchableOpacity as any)');
    fs.writeFileSync('aznavrail-react/src/__tests__/AzButton.test.tsx', buttonTest);
}

if (fs.existsSync('aznavrail-react/src/__tests__/AzTextBox.test.tsx')) {
    let textTest = fs.readFileSync('aznavrail-react/src/__tests__/AzTextBox.test.tsx', 'utf8');
    textTest = textTest.replace(/root\.findByType\(TextInput\)/g, 'root.findByType(TextInput as any)');
    textTest = textTest.replace(/root\.findByType\(TouchableOpacity\)/g, 'root.findByType(TouchableOpacity as any)');
    fs.writeFileSync('aznavrail-react/src/__tests__/AzTextBox.test.tsx', textTest);
}

if (fs.existsSync('aznavrail-react/src/__tests__/AzNavRailFull.test.tsx')) {
    let fullTest = fs.readFileSync('aznavrail-react/src/__tests__/AzNavRailFull.test.tsx', 'utf8');
    fullTest = fullTest.replace(/let component: renderer\.ReactTestRenderer;/g, 'let component: renderer.ReactTestRenderer = {} as any;');
    fs.writeFileSync('aznavrail-react/src/__tests__/AzNavRailFull.test.tsx', fullTest);
}

// 6. fix duplicate attribute in AzButton.tsx
if (fs.existsSync('aznavrail-react/src/components/AzButton.tsx')) {
    let btn = fs.readFileSync('aznavrail-react/src/components/AzButton.tsx', 'utf8');
    // find duplicate attribute (likely style or something)
    // we can't easily parse without knowing it, but let's see.
}
