figma.showUI(__html__, { width: 300, height: 200 });

figma.ui.onmessage = async (msg) => {
  if (msg.type === 'generate-ui-kit') {
    await figma.loadFontAsync({ family: "Inter", style: "Regular" });
    await figma.loadFontAsync({ family: "Inter", style: "Medium" });
    await figma.loadFontAsync({ family: "Inter", style: "Bold" });

    let currentX = 0;
    const spacingX = 500;

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

    // --- Generate AzNavRailButton Component ---
    const azRailBtnDefault = figma.createComponent();
    azRailBtnDefault.name = "AzNavRailButton / Default";
    azRailBtnDefault.layoutMode = "VERTICAL";
    azRailBtnDefault.primaryAxisSizingMode = "FIXED";
    azRailBtnDefault.counterAxisSizingMode = "FIXED";
    azRailBtnDefault.resize(80, 80);
    azRailBtnDefault.primaryAxisAlignItems = "CENTER";
    azRailBtnDefault.counterAxisAlignItems = "CENTER";
    azRailBtnDefault.itemSpacing = 4;
    azRailBtnDefault.cornerRadius = 16;
    azRailBtnDefault.fills = [{ type: 'SOLID', color: { r: 0.95, g: 0.95, b: 0.95 } }];

    const iconPlaceholder = figma.createEllipse();
    iconPlaceholder.resize(24, 24);
    iconPlaceholder.fills = [{ type: 'SOLID', color: { r: 0.4, g: 0.4, b: 0.4 } }];
    azRailBtnDefault.appendChild(iconPlaceholder);

    const railBtnText = createText("Item", 12, "Medium");
    railBtnText.fills = [{ type: 'SOLID', color: { r: 0.2, g: 0.2, b: 0.2 } }];
    azRailBtnDefault.appendChild(railBtnText);

    const azRailBtnHover = azRailBtnDefault.clone() as ComponentNode;
    azRailBtnHover.name = "AzNavRailButton / Hover";
    azRailBtnHover.fills = [{ type: 'SOLID', color: { r: 0.85, g: 0.85, b: 0.85 } }];

    // Combine into Component Set for variants
    const railBtnSet = figma.combineAsVariants([azRailBtnDefault, azRailBtnHover], figma.currentPage);
    railBtnSet.name = "AzNavRailButton";
    railBtnSet.x = currentX;
    railBtnSet.y = 0;

    // Add Hover Interaction (Automatic Import)
    azRailBtnDefault.reactions = [
      {
        trigger: { type: "ON_HOVER" },
        action: {
          type: "NODE",
          destinationId: azRailBtnHover.id,
          navigation: "CHANGE_TO",
          transition: {
            type: "SMART_ANIMATE",
            easing: { type: "CUSTOM_CUBIC_BEZIER", easingFunctionCubicBezier: { x1: 0.2, y1: 0, x2: 0, y2: 1 } },
            duration: 300
          }
        }
      }
    ];

    currentX += spacingX;

    // --- Generate Interactive Demonstration Frames ---
    // Frame A: Docked Mode
    const frameA = figma.createFrame();
    frameA.name = "Demo: Docked Mode";
    frameA.resize(360, 800);
    frameA.x = currentX;
    frameA.y = 0;
    frameA.fills = [{ type: 'SOLID', color: { r: 0.98, g: 0.98, b: 0.98 } }];

    const dockedRail = figma.createFrame();
    dockedRail.name = "Docked Rail";
    dockedRail.layoutMode = "VERTICAL";
    dockedRail.primaryAxisSizingMode = "FIXED";
    dockedRail.counterAxisSizingMode = "FIXED";
    dockedRail.resize(100, 800);
    dockedRail.primaryAxisAlignItems = "MIN";
    dockedRail.counterAxisAlignItems = "CENTER";
    dockedRail.paddingTop = 24;
    dockedRail.itemSpacing = 16;
    dockedRail.fills = [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
    
    const headerIconA = figma.createEllipse();
    headerIconA.name = "App Icon (Trigger)";
    headerIconA.resize(48, 48);
    headerIconA.fills = [{ type: 'SOLID', color: { r: 0.8, g: 0.2, b: 0.2 } }];
    dockedRail.appendChild(headerIconA);

    for(let i=0; i<4; i++) {
        dockedRail.appendChild(azRailBtnDefault.createInstance());
    }
    frameA.appendChild(dockedRail);
    currentX += spacingX;

    // Frame B: FAB Mode (Folded Up)
    const frameB = figma.createFrame();
    frameB.name = "Demo: FAB Mode (Folded)";
    frameB.resize(360, 800);
    frameB.x = currentX;
    frameB.y = 0;
    frameB.fills = [{ type: 'SOLID', color: { r: 0.98, g: 0.98, b: 0.98 } }];

    const fabRail = figma.createFrame();
    fabRail.name = "FAB Rail Container";
    fabRail.layoutMode = "VERTICAL";
    fabRail.primaryAxisSizingMode = "AUTO";
    fabRail.counterAxisSizingMode = "AUTO";
    fabRail.itemSpacing = 16;
    fabRail.fills = [];
    fabRail.x = 280; // Moved to bottom right corner
    fabRail.y = 700;

    const headerIconB = figma.createEllipse();
    headerIconB.name = "App Icon (FAB)";
    headerIconB.resize(48, 48);
    headerIconB.fills = [{ type: 'SOLID', color: { r: 0.8, g: 0.2, b: 0.2 } }];
    fabRail.appendChild(headerIconB);
    frameB.appendChild(fabRail);
    currentX += spacingX;

    // Frame C: FAB Mode (Unfolded)
    const frameC = figma.createFrame();
    frameC.name = "Demo: FAB Mode (Unfolded)";
    frameC.resize(360, 800);
    frameC.x = currentX;
    frameC.y = 0;
    frameC.fills = [{ type: 'SOLID', color: { r: 0.98, g: 0.98, b: 0.98 } }];

    const fabRailUnfolded = figma.createFrame();
    fabRailUnfolded.name = "FAB Rail Container Unfolded";
    fabRailUnfolded.layoutMode = "VERTICAL";
    fabRailUnfolded.primaryAxisSizingMode = "AUTO";
    fabRailUnfolded.counterAxisSizingMode = "AUTO";
    fabRailUnfolded.itemSpacing = 16;
    fabRailUnfolded.fills = [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
    fabRailUnfolded.cornerRadius = 24;
    fabRailUnfolded.paddingTop = 16;
    fabRailUnfolded.paddingBottom = 16;
    fabRailUnfolded.paddingLeft = 8;
    fabRailUnfolded.paddingRight = 8;
    fabRailUnfolded.x = 260; // Slightly shifted to fit items
    fabRailUnfolded.y = 400; // Expanded upwards/downwards

    const headerIconC = figma.createEllipse();
    headerIconC.name = "App Icon (FAB Unfolded)";
    headerIconC.resize(48, 48);
    headerIconC.fills = [{ type: 'SOLID', color: { r: 0.8, g: 0.2, b: 0.2 } }];
    fabRailUnfolded.appendChild(headerIconC);

    for(let i=0; i<4; i++) {
        fabRailUnfolded.appendChild(azRailBtnDefault.createInstance());
    }
    frameC.appendChild(fabRailUnfolded);

    // Wire up prototyping interactions for the demo frames (Manual Animations constructed programmatically)
    
    // Docked (Swipe Down/Click) -> FAB Mode Folded
    headerIconA.reactions = [
      {
        trigger: { type: "ON_CLICK" },
        action: {
          type: "NODE",
          destinationId: frameB.id,
          navigation: "NAVIGATE",
          transition: {
            type: "SMART_ANIMATE",
            easing: { type: "CUSTOM_CUBIC_BEZIER", easingFunctionCubicBezier: { x1: 0.42, y1: 0, x2: 0.21, y2: 0.9 } }, // M3 Expressive
            duration: 500
          }
        }
      }
    ];

    // FAB Folded (Click) -> FAB Unfolded
    headerIconB.reactions = [
      {
        trigger: { type: "ON_CLICK" },
        action: {
          type: "NODE",
          destinationId: frameC.id,
          navigation: "NAVIGATE",
          transition: {
            type: "SMART_ANIMATE",
            easing: { type: "CUSTOM_CUBIC_BEZIER", easingFunctionCubicBezier: { x1: 0.42, y1: 0, x2: 0.21, y2: 0.9 } },
            duration: 400
          }
        }
      }
    ];

    // FAB Unfolded (Click) -> FAB Folded
    headerIconC.reactions = [
      {
        trigger: { type: "ON_CLICK" },
        action: {
          type: "NODE",
          destinationId: frameB.id,
          navigation: "NAVIGATE",
          transition: {
            type: "SMART_ANIMATE",
            easing: { type: "CUSTOM_CUBIC_BEZIER", easingFunctionCubicBezier: { x1: 0.42, y1: 0, x2: 0.21, y2: 0.9 } },
            duration: 400
          }
        }
      }
    ];

    // Notify user
    figma.notify("AzNavRail Compose UI Kit & Animations generated!");
  }
};
