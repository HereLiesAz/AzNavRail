figma.showUI(__html__, { width: 300, height: 200 });

figma.ui.onmessage = async (msg) => {
  if (msg.type === 'generate-ui-kit') {
    await figma.loadFontAsync({ family: "Inter", style: "Regular" });
    await figma.loadFontAsync({ family: "Inter", style: "Medium" });
    await figma.loadFontAsync({ family: "Inter", style: "Bold" });

    let currentX = 0;
    const spacingX = 400;

    // Helper to create a text node
    function createText(text: string, size: number, weight: "Regular" | "Medium" | "Bold" = "Regular") {
      const node = figma.createText();
      node.characters = text;
      node.fontSize = size;
      node.fontName = { family: "Inter", style: weight };
      return node;
    }

    // Helper to create Auto Layout frame
    function createAutoLayout(name: string, direction: "HORIZONTAL" | "VERTICAL") {
      const frame = figma.createFrame();
      frame.name = name;
      frame.layoutMode = direction;
      frame.primaryAxisSizingMode = "AUTO";
      frame.counterAxisSizingMode = "AUTO";
      frame.fills = [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
      return frame;
    }

    // --- Generate AzButton ---
    const azButton = figma.createComponent();
    azButton.name = "AzButton";
    azButton.layoutMode = "HORIZONTAL";
    azButton.primaryAxisSizingMode = "AUTO";
    azButton.counterAxisSizingMode = "AUTO";
    azButton.paddingLeft = 16;
    azButton.paddingRight = 16;
    azButton.paddingTop = 12;
    azButton.paddingBottom = 12;
    azButton.cornerRadius = 8;
    azButton.fills = [{ type: 'SOLID', color: { r: 0.2, g: 0.5, b: 0.9 } }]; // Primary color
    
    const btnText = createText("AzButton", 14, "Medium");
    btnText.fills = [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
    azButton.appendChild(btnText);
    
    azButton.x = currentX;
    azButton.y = 0;
    currentX += spacingX;

    // --- Generate AzNavRailButton ---
    const azRailButton = figma.createComponent();
    azRailButton.name = "AzNavRailButton";
    azRailButton.layoutMode = "VERTICAL";
    azRailButton.primaryAxisSizingMode = "FIXED";
    azRailButton.counterAxisSizingMode = "FIXED";
    azRailButton.resize(80, 80);
    azRailButton.primaryAxisAlignItems = "CENTER";
    azRailButton.counterAxisAlignItems = "CENTER";
    azRailButton.itemSpacing = 4;
    azRailButton.cornerRadius = 16;
    azRailButton.fills = [{ type: 'SOLID', color: { r: 0.95, g: 0.95, b: 0.95 } }];

    // Fake an icon placeholder
    const iconPlaceholder = figma.createEllipse();
    iconPlaceholder.resize(24, 24);
    iconPlaceholder.fills = [{ type: 'SOLID', color: { r: 0.4, g: 0.4, b: 0.4 } }];
    azRailButton.appendChild(iconPlaceholder);

    const railBtnText = createText("Item", 12, "Medium");
    railBtnText.fills = [{ type: 'SOLID', color: { r: 0.2, g: 0.2, b: 0.2 } }];
    azRailButton.appendChild(railBtnText);

    azRailButton.x = currentX;
    azRailButton.y = 0;
    currentX += spacingX;

    // --- Generate AzNavRail (Docked) ---
    const azNavRail = figma.createComponent();
    azNavRail.name = "AzNavRail (Docked)";
    azNavRail.layoutMode = "VERTICAL";
    azNavRail.primaryAxisSizingMode = "FIXED";
    azNavRail.counterAxisSizingMode = "FIXED";
    azNavRail.resize(100, 800); // Typical rail size
    azNavRail.primaryAxisAlignItems = "MIN";
    azNavRail.counterAxisAlignItems = "CENTER";
    azNavRail.paddingTop = 24;
    azNavRail.paddingBottom = 24;
    azNavRail.itemSpacing = 16;
    azNavRail.fills = [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
    
    // Header icon
    const headerIcon = figma.createEllipse();
    headerIcon.resize(48, 48);
    headerIcon.fills = [{ type: 'SOLID', color: { r: 0.8, g: 0.2, b: 0.2 } }];
    azNavRail.appendChild(headerIcon);

    // Add some rail buttons
    for(let i=0; i<3; i++) {
        const instance = azRailButton.createInstance();
        azNavRail.appendChild(instance);
    }

    azNavRail.x = currentX;
    azNavRail.y = 0;
    currentX += spacingX;

    // --- Generate AzDropdownMenu ---
    const azDropdown = figma.createComponent();
    azDropdown.name = "AzDropdownMenu";
    azDropdown.layoutMode = "VERTICAL";
    azDropdown.primaryAxisSizingMode = "AUTO";
    azDropdown.counterAxisSizingMode = "FIXED";
    azDropdown.resize(200, 100); // Height will auto-grow
    azDropdown.paddingLeft = 8;
    azDropdown.paddingRight = 8;
    azDropdown.paddingTop = 8;
    azDropdown.paddingBottom = 8;
    azDropdown.cornerRadius = 12;
    azDropdown.itemSpacing = 4;
    azDropdown.fills = [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
    azDropdown.effects = [{
      type: "DROP_SHADOW",
      color: { r: 0, g: 0, b: 0, a: 0.15 },
      offset: { x: 0, y: 4 },
      radius: 12,
      spread: 0,
      visible: true,
      blendMode: "NORMAL"
    }];

    for(let i=1; i<=3; i++) {
      const menuItem = createAutoLayout(`MenuItem ${i}`, "HORIZONTAL");
      menuItem.primaryAxisSizingMode = "FIXED";
      menuItem.layoutAlign = "STRETCH";
      menuItem.resize(184, 40); // 200 - 16 padding
      menuItem.paddingLeft = 12;
      menuItem.paddingRight = 12;
      menuItem.counterAxisAlignItems = "CENTER";
      menuItem.cornerRadius = 6;
      menuItem.fills = []; // Transparent background
      
      const itemText = createText(`Menu Option ${i}`, 14);
      itemText.fills = [{ type: 'SOLID', color: { r: 0.1, g: 0.1, b: 0.1 } }];
      menuItem.appendChild(itemText);
      azDropdown.appendChild(menuItem);
    }

    azDropdown.x = currentX;
    azDropdown.y = 0;
    currentX += spacingX;

    // Notify user
    figma.notify("AzNavRail Compose UI Kit generated!");
  }
};
