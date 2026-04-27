import { useState } from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import {
  AzNavRail,
  AzButtonShape,
  AzDockingSide,
  AzNestedRailAlignment,
  AzConfig,
  AzTheme,
  AzAdvanced,
  AzRailItem,
  AzMenuItem,
  AzRailToggle,
  AzMenuToggle,
  AzHelpRailItem,
  AzDivider,
  AzRailCycler,
  AzMenuCycler,
  AzMenuHostItem,
  AzMenuSubItem,
  AzRailHostItem,
  AzRailSubItem,
  AzMenuSubToggle,
  AzRailSubCycler,
  AzRailRelocItem,
  AzNestedRail,
} from '@HereLiesAz/aznavrail-react';

function App() {
  const location = useLocation();

  const [isOnline, setIsOnline] = useState(true);
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [packRailButtons, setPackRailButtons] = useState(false);
  const [railSelectedOption, setRailSelectedOption] = useState("A");
  const [menuSelectedOption, setMenuSelectedOption] = useState("X");
  const [isDockingRight, setIsDockingRight] = useState(false);
  const [noMenu, setNoMenu] = useState(false);
  const [showHelp, setShowHelp] = useState(false);
  const [usePhysicalDocking, setUsePhysicalDocking] = useState(false);

  const themeColor = '#6200EE'; // primary color fallback

  const renderRoutes = () => {
    const routes = [
      'multi-line', 'menu-host', 'menu-sub-1', 'menu-sub-2', 'rail-host',
      'rail-sub-1', 'rail-sub-2', 'sub-toggle', 'sub-cycler', 'pack-rail',
      'profile', 'online', 'dark-mode', 'rail-cycler', 'menu-cycler',
      'loading', 'physical-docking', 'docking-side', 'no-menu', 'nested-1',
      'nested-2', 'nested-h-1', 'nested-h-2', 'nested-h-3'
    ];

    return routes.map((route) => (
      <Route
        key={route}
        path={`/${route}`}
        element={<ScreenContent text={`${route.split('-').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ')} Screen`} />}
      />
    ));
  };

  return (
    <div style={{ width: '100vw', height: '100vh', display: 'flex', flexDirection: 'row', overflow: 'hidden' }}>
      <AzNavRail
        currentDestination={location.pathname.replace('/', '') || 'home'}
        packRailButtons={packRailButtons}
        dockingSide={isDockingRight ? AzDockingSide.RIGHT : AzDockingSide.LEFT}
        noMenu={noMenu}
        usePhysicalDocking={usePhysicalDocking}
        defaultShape={AzButtonShape.RECTANGLE}
        activeColor={themeColor}
        isLoading={isLoading}
        enableRailDragging={true}
        infoScreen={showHelp}
        onDismissInfoScreen={() => setShowHelp(false)}
        helpList={{
          "home": "This is a test helpList text!",
          "nested-1": "Nested helpList entries work too!"
        }}
        onInteraction={(action, details) => console.log(`[SampleApp] ${action}${details ? ' ' + details : ''}`)}
      >
        <AzConfig
          packRailButtons={packRailButtons}
          dockingSide={isDockingRight ? AzDockingSide.RIGHT : AzDockingSide.LEFT}
          noMenu={noMenu}
          usePhysicalDocking={usePhysicalDocking}
        />
        <AzTheme
          defaultShape={AzButtonShape.RECTANGLE}
          activeColor={themeColor}
        />
        <AzAdvanced
          isLoading={isLoading}
          enableRailDragging={true}
          infoScreen={showHelp}
          onDismissInfoScreen={() => setShowHelp(false)}
          helpList={{
              "home": "This is a test helpList text!",
              "nested-1": "Nested helpList entries work too!"
          }}
          tutorials={{
            "home": {
              scenes: [
                {
                  id: "scene1",
                  content: () => (
                    <div style={{ width: '100%', height: '100%', backgroundColor: '#222', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <p style={{ color: 'white', fontSize: 24 }}>Scripted App Screen</p>
                    </div>
                  ),
                  cards: [
                    {
                      title: "Welcome",
                      text: "Welcome to the tutorial.",
                      highlight: { type: "FullScreen" }
                    },
                    {
                      title: "Highlighting",
                      text: "Notice the highlighted item.",
                      highlight: { type: "Item", id: "home" },
                      actionText: "Finish"
                    }
                  ]
                }
              ]
            }
          }}
        />

        <AzMenuItem id="home" text="Home" route="home" info="Navigate to the Home screen" onClick={() => console.log('Home menu item clicked')} />
        <AzMenuItem id="multi-line" text={"This is a\nmulti-line item"} route="multi-line" info="Shows how multi-line text is handled" onClick={() => console.log('Multi-line menu item clicked')} />

        <AzRailToggle
          id="pack-rail"
          text="Pack Rail"
          isChecked={packRailButtons}
          toggleOnText="Packed"
          toggleOffText="Unpacked"
          route="pack-rail"
          info="Toggle to pack items together or space them out"
          onClick={() => {
            setPackRailButtons(!packRailButtons);
            console.log('Pack rail toggled to:', !packRailButtons);
          }}
        />

        <AzRailItem
          id="color-item"
          text="Color"
          menuText="Custom Menu Text"
          textColor="#FFFFFF"
          fillColor="#0000FF"
          content={<div style={{ backgroundColor: '#FF0000', width: '100%', height: '100%' }} />}
          info="Demonstrates dynamic content with Color and Custom Text/Colors"
          onClick={() => console.log('Color item clicked')}
        />

        <AzRailItem
          id="none-shape"
          text="No Shape"
          shape={AzButtonShape.NONE}
          info="Item with shape NONE"
          onClick={() => console.log('No Shape item clicked')}
        />

        <AzRailItem
          id="profile"
          text="Profile"
          disabled={true}
          route="profile"
          info="User profile settings (Disabled)"
        />

        <AzDivider />

        <AzRailToggle
          id="online"
          text="Online"
          isChecked={isOnline}
          toggleOnText="Online"
          toggleOffText="Offline"
          route="online"
          onClick={() => {
            setIsOnline(!isOnline);
            console.log('Online toggled to:', !isOnline);
          }}
        />

        <AzMenuToggle
          id="dark-mode"
          text="Dark Mode"
          isChecked={isDarkMode}
          toggleOnText="Dark Mode"
          toggleOffText="Light Mode"
          route="dark-mode"
          onClick={() => {
            setIsDarkMode(!isDarkMode);
            console.log('Dark mode toggled to:', !isDarkMode);
          }}
        />

        <AzMenuToggle
          id="docking-side"
          text="Docking"
          isChecked={isDockingRight}
          toggleOnText="Dock: Right"
          toggleOffText="Dock: Left"
          route="docking-side"
          onClick={() => {
            setIsDockingRight(!isDockingRight);
            console.log('Docking side toggled to:', !isDockingRight ? 'Right' : 'Left');
          }}
        />

        <AzMenuToggle
          id="no-menu"
          text="No Menu"
          isChecked={noMenu}
          toggleOnText="No Menu: On"
          toggleOffText="No Menu: Off"
          route="no-menu"
          onClick={() => {
            setNoMenu(!noMenu);
            console.log('No Menu toggled to:', !noMenu);
          }}
        />

        <AzMenuToggle
          id="physical-docking"
          text="Physical Dock"
          isChecked={usePhysicalDocking}
          toggleOnText="Physical Dock: On"
          toggleOffText="Physical Dock: Off"
          route="physical-docking"
          onClick={() => {
            setUsePhysicalDocking(!usePhysicalDocking);
            console.log('Physical Docking toggled to:', !usePhysicalDocking);
          }}
        />

        <AzHelpRailItem id="toggle-help" text="Help" onClick={() => setShowHelp(!showHelp)} />

        <AzDivider />

        <AzRailCycler
          id="rail-cycler"
          text="Rail Cycler"
          options={["A", "B", "C", "D"]}
          selectedOption={railSelectedOption}
          disabledOptions={["C"]}
          route="rail-cycler"
          onClick={() => {
            const opts = ["A", "B", "C", "D"];
            const next = opts[(opts.indexOf(railSelectedOption) + 1) % opts.length];
            setRailSelectedOption(next);
            console.log('Rail cycler clicked, new option:', next);
          }}
        />

        <AzMenuCycler
          id="menu-cycler"
          text="Menu Cycler"
          options={["X", "Y", "Z"]}
          selectedOption={menuSelectedOption}
          route="menu-cycler"
          onClick={() => {
            const opts = ["X", "Y", "Z"];
            const next = opts[(opts.indexOf(menuSelectedOption) + 1) % opts.length];
            setMenuSelectedOption(next);
            console.log('Menu cycler clicked, new option:', next);
          }}
        />

        <AzRailItem id="loading" text="Load" route="loading" onClick={() => {
            setIsLoading(!isLoading);
            console.log('Loading toggled to:', !isLoading);
        }} />

        <AzDivider />

        <AzMenuHostItem id="menu-host" text="Menu Host" route="menu-host" onClick={() => console.log('Menu host item clicked')} />
        <AzMenuSubItem id="menu-sub-1" hostId="menu-host" text="Menu Sub 1" route="menu-sub-1" onClick={() => console.log('Menu sub item 1 clicked')} />
        <AzMenuSubItem id="menu-sub-2" hostId="menu-host" text="Menu Sub 2" route="menu-sub-2" onClick={() => console.log('Menu sub item 2 clicked')} />

        <AzRailHostItem id="rail-host" text="Rail Host" route="rail-host" onClick={() => console.log('Rail host item clicked')} />
        <AzRailSubItem id="rail-sub-1" hostId="rail-host" text="Rail Sub 1" route="rail-sub-1" onClick={() => console.log('Rail sub item 1 clicked')} />
        <AzMenuSubItem id="rail-sub-2" hostId="rail-host" text="Menu Sub 2" route="rail-sub-2" onClick={() => console.log('Menu sub item 2 (from rail host) clicked')} />

        <AzMenuSubToggle
          id="sub-toggle"
          hostId="menu-host"
          text="Sub Toggle"
          isChecked={isDarkMode}
          toggleOnText="Sub Toggle On"
          toggleOffText="Sub Toggle Off"
          route="sub-toggle"
          onClick={() => {
            setIsDarkMode(!isDarkMode);
            console.log('Sub toggle clicked, dark mode is now:', !isDarkMode);
          }}
        />

        <AzRailSubCycler
          id="sub-cycler"
          text="Sub Cycler"
          hostId="rail-host"
          options={["X", "Y", "Z"]}
          selectedOption={menuSelectedOption}
          route="sub-cycler"
          onClick={() => {
            const opts = ["X", "Y", "Z"];
            const next = opts[(opts.indexOf(menuSelectedOption) + 1) % opts.length];
            setMenuSelectedOption(next);
            console.log('Sub cycler clicked, new option:', next);
          }}
        />

        <AzRailRelocItem
          id="reloc-1"
          hostId="rail-host"
          text="Reloc Item 1"
          onRelocate={(from, to, newOrder) => console.log(`Relocated item 1 from ${from} to ${to}. New order: ${newOrder}`)}
          hiddenMenu={(scope) => {
            scope.listItem("Action 1", () => console.log('Reloc 1 action clicked'));
          }}
        />

        <AzRailRelocItem
          id="reloc-nested-parent"
          hostId="rail-host"
          text="Reloc + Nested"
          onRelocate={(from, to, newOrder) => console.log(`Relocated item 2 from ${from} to ${to}. New order: ${newOrder}`)}
          nestedRailAlignment={AzNestedRailAlignment.HORIZONTAL}
          hiddenMenu={(scope) => {
             scope.listItem("Remove", () => console.log('Reloc Nested action clicked'));
          }}
          nestedContent={
            <>
              <AzRailItem id="nested-tool-1" text="Tool 1" onClick={() => console.log('Tool 1 clicked')} />
              <AzRailItem id="nested-tool-2" text="Tool 2" onClick={() => console.log('Tool 2 clicked')} />
            </>
          }
        />

        <AzNestedRail
          id="nested-rail"
          text="Vertical Nested"
          alignment={AzNestedRailAlignment.VERTICAL}
        >
          <AzRailItem id="nested-1" text="Nested Item 1" route="nested-1" info="This is a nested item. Tap the card to expand it and read the full help text, demonstrating that nested items report bounds correctly!" />
          <AzRailItem id="nested-2" text="Nested Item 2" route="nested-2" />
          <AzRailItem
             id="nested-custom"
             text="Size Slider"
             content={
               <div style={{ backgroundColor: 'red', width: 300, height: 50, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white' }}>
                 Wide Content (Should Clip)
               </div>
             }
          />
        </AzNestedRail>

        <AzNestedRail
          id="nested-horizontal"
          text="Horizontal Nested"
          alignment={AzNestedRailAlignment.HORIZONTAL}
        >
          <AzRailItem id="nested-h-1" text="H-Item 1" route="nested-h-1" />
          <AzRailItem id="nested-h-2" text="H-Item 2" route="nested-h-2" />
          <AzRailItem id="nested-h-3" text="H-Item 3" route="nested-h-3" />
        </AzNestedRail>

        {/* Onscreen content will go here */}
        <div style={{ flex: 1, padding: 20, overflowY: 'auto' }}>
          <Routes>
             <Route path="/" element={<ScreenContent text="Home Screen" />} />
             <Route path="/home" element={<ScreenContent text="Home Screen" />} />
             {renderRoutes()}
          </Routes>
        </div>
      </AzNavRail>
    </div>
  );
}

const ScreenContent = ({ text }: { text: string }) => (
  <div style={{ display: 'flex', flex: 1, height: '100%', alignItems: 'center', justifyContent: 'center' }}>
    <p>{text}</p>
  </div>
);

export default App;
