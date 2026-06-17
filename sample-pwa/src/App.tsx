import { useState } from 'react'
import { Routes, Route, useLocation, useNavigate } from 'react-router-dom'
import {
  AzHostActivityLayout,
  AzOnscreen,
  AzAlignment,
  AzButtonShape,
  AzDockingSide,
  AzDropdownSource,
  AzHeaderIconShape,
  AzNestedRailAlignment,
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
  AzRailSubHostItem,
  AzMenuSubHostItem,
  AzMenuSubToggle,
  AzRailSubCycler,
  AzRailRelocItem,
  AzNestedRail,
} from '@HereLiesAz/aznavrail-react'

import ShowcaseHome from './screens/ShowcaseHome'
import BottomSheetDemo from './screens/BottomSheetDemo'
import StandaloneWidgets from './screens/StandaloneWidgets'
import CustomizationDemo, {
  type CustomizationState,
  DEMO_CLASSIFIERS,
} from './screens/CustomizationDemo'
import FormDemo from './screens/FormDemo'
import HelpSystemDemo from './screens/HelpSystemDemo'
import TutorialDemo from './screens/TutorialDemo'
import LegacyPlayground from './screens/LegacyPlayground'

function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const currentRoute = location.pathname.replace(/^\//, '') || 'home'

  // Legacy rail toggles
  const [isOnline, setIsOnline] = useState(true)
  const [isDarkMode, setIsDarkMode] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [packRailButtons, setPackRailButtons] = useState(false)
  const [railSelectedOption, setRailSelectedOption] = useState('A')
  const [menuSelectedOption, setMenuSelectedOption] = useState('X')
  const [isDockingRight, setIsDockingRight] = useState(false)
  const [noMenu, setNoMenu] = useState(false)
  const [showHelp, setShowHelp] = useState(false)
  const [dismissCount, setDismissCount] = useState(0)
  const [usePhysicalDocking, setUsePhysicalDocking] = useState(false)

  // Customization (lives at host scope so the rail re-renders live)
  const [customization, setCustomization] = useState<CustomizationState>({
    headerIconShape: AzHeaderIconShape.CIRCLE,
    defaultShape: AzButtonShape.RECTANGLE,
    expandedRailWidth: 280,
    collapsedRailWidth: 136,
    displayAppNameInHeader: false,
    showFooter: true,
    appRepositoryUrl: 'https://github.com/HereLiesAz/AzNavRail',
    vibrate: false,
    activeClassifiers: new Set<string>(),
    headerIconSize: 0,
    dropdownMenu: false,
    dropdownSource: AzDropdownSource.RAIL,
  })

  const themeColor = '#6200EE'

  return (
    <AzHostActivityLayout
      currentDestination={currentRoute}
      packRailButtons={packRailButtons}
      dockingSide={isDockingRight ? AzDockingSide.RIGHT : AzDockingSide.LEFT}
      noMenu={noMenu}
      usePhysicalDocking={usePhysicalDocking}
      defaultShape={customization.defaultShape}
      activeColor={themeColor}
      isLoading={isLoading}
      enableRailDragging
      infoScreen={showHelp}
      headerIconShape={customization.headerIconShape}
      expandedRailWidth={customization.expandedRailWidth}
      collapsedRailWidth={customization.collapsedRailWidth}
      displayAppNameInHeader={customization.displayAppNameInHeader}
      showFooter={customization.showFooter}
      appRepositoryUrl={customization.appRepositoryUrl}
      vibrate={customization.vibrate}
      activeClassifiers={customization.activeClassifiers}
      headerIconSize={customization.headerIconSize || undefined}
      dropdownMenu={customization.dropdownMenu}
      dropdownSource={customization.dropdownSource}
      onDismissInfoScreen={() => {
        setShowHelp(false)
        setDismissCount((n) => n + 1)
      }}
      helpList={{
        home: 'Showcase home — links to each demo screen.',
        'bottom-sheet': 'AzBottomSheet + AzSheetController demo.',
        standalone: 'Standalone widgets at every shape variant.',
        customization: 'Live theme controls.',
        forms: 'AzForm + AzTextBox showcase.',
        'color-item': 'Dynamic content (Color) + custom text/fill.',
        'rail-cycler': 'Cycler with one disabled option (C).',
        'nested-1': 'Nested item — bounds reporting works inside popups.',
      }}
    >
      {/* ---------- Showcase navigation ---------- */}
      <AzMenuItem id="home" text="Showcase Home" route="home" info="Index of every demo screen." onClick={() => navigate('/')} />
      <AzMenuItem id="bottom-sheet" text="Bottom Sheets" route="bottom-sheet" info="AzBottomSheet detents, drag, scrim, swipe." onClick={() => navigate('/bottom-sheet')} />
      <AzMenuItem id="standalone" text="Standalone Widgets" route="standalone" info="AzButton/Toggle/Cycler at every shape, AzLoad, AzRoller." onClick={() => navigate('/standalone')} />
      <AzMenuItem id="customization" text="Customization" route="customization" info="Live theme controls." onClick={() => navigate('/customization')} />
      <AzMenuItem id="forms" text="Forms" route="forms" info="AzForm + AzTextBox showcase." onClick={() => navigate('/forms')} />
      <AzMenuItem id="help-system" text="Help System" route="help-system" info="screenTitle, info, classifiers, helpList." classifiers={new Set(['focus'])} onClick={() => navigate('/help-system')} />
      <AzMenuItem id="tutorial" text="Tutorials" route="tutorial" info="AzTutorial DSL — every advance condition + highlight." classifiers={new Set(['advanced'])} onClick={() => navigate('/tutorial')} />
      <AzMenuItem id="legacy" text="Rail Playground" route="legacy" info="The original rail demos." onClick={() => navigate('/legacy')} />

      <AzDivider />

      {/* ---------- Rail-config toggles (legacy demos) ---------- */}
      <AzRailToggle
        id="pack-rail"
        text="Pack Rail"
        isChecked={packRailButtons}
        toggleOnText="Packed"
        toggleOffText="Unpacked"
        info="Toggle to pack items together or space them out."
        onClick={() => setPackRailButtons((v) => !v)}
      />

      {/* AzButtonShape showcase — one rail item per value */}
      <AzRailItem id="shape-circle" text="Circle" shape={AzButtonShape.CIRCLE} info="AzButtonShape.CIRCLE" onClick={() => console.log('circle')} />
      <AzRailItem id="shape-square" text="Square" shape={AzButtonShape.SQUARE} info="AzButtonShape.SQUARE" onClick={() => console.log('square')} />
      <AzRailItem id="shape-rectangle" text="Rectangle" shape={AzButtonShape.RECTANGLE} info="AzButtonShape.RECTANGLE" onClick={() => console.log('rectangle')} />
      <AzRailItem id="shape-none" text="No Shape" shape={AzButtonShape.NONE} info="AzButtonShape.NONE — text only" onClick={() => console.log('none')} />

      <AzRailItem
        id="color-item"
        text="Color"
        menuText="Custom Menu Text"
        textColor="#FFFFFF"
        fillColor="#0000FF"
        info="Dynamic content with custom text/fill colours."
        onClick={() => console.log('color item')}
      />

      <AzRailItem
        id="vector-item"
        text="Vector"
        content={{
          // An inline SVG (vector graphic) — fills and is clipped to the item's shape.
          uri:
            "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'><path fill='%236200ee' d='M3 6h18v2H3zm2 3h14l-1 12H6zM9 4h6v2H9z'/></svg>",
        }}
        info="Dynamic content with a vector graphic (SVG) that fills the shape."
        onClick={() => console.log('vector item')}
      />

      <AzRailItem
        id="profile"
        text="Profile"
        disabled
        route="profile"
        info="User profile (disabled state demo)."
      />

      <AzDivider />

      <AzRailToggle
        id="online"
        text="Online"
        isChecked={isOnline}
        toggleOnText="Online"
        toggleOffText="Offline"
        onClick={() => setIsOnline((v) => !v)}
      />

      <AzMenuToggle
        id="dark-mode"
        text="Dark Mode"
        isChecked={isDarkMode}
        toggleOnText="Dark Mode"
        toggleOffText="Light Mode"
        onClick={() => setIsDarkMode((v) => !v)}
      />

      <AzMenuToggle
        id="docking-side"
        text="Docking"
        isChecked={isDockingRight}
        toggleOnText="Dock: Right"
        toggleOffText="Dock: Left"
        onClick={() => setIsDockingRight((v) => !v)}
      />

      <AzMenuToggle
        id="no-menu"
        text="No Menu"
        isChecked={noMenu}
        toggleOnText="No Menu: On"
        toggleOffText="No Menu: Off"
        onClick={() => setNoMenu((v) => !v)}
      />

      <AzMenuToggle
        id="physical-docking"
        text="Physical Dock"
        isChecked={usePhysicalDocking}
        toggleOnText="Physical Dock: On"
        toggleOffText="Physical Dock: Off"
        onClick={() => setUsePhysicalDocking((v) => !v)}
      />

      <AzHelpRailItem id="toggle-help" text="Help" onClick={() => setShowHelp((v) => !v)} />

      <AzDivider />

      <AzRailCycler
        id="rail-cycler"
        text="Rail Cycler"
        options={['A', 'B', 'C', 'D']}
        selectedOption={railSelectedOption}
        disabledOptions={['C']}
        onClick={() => {
          const opts = ['A', 'B', 'C', 'D']
          const idx = opts.indexOf(railSelectedOption)
          setRailSelectedOption(opts[(idx + 1) % opts.length])
        }}
      />

      <AzMenuCycler
        id="menu-cycler"
        text="Menu Cycler"
        options={['X', 'Y', 'Z']}
        selectedOption={menuSelectedOption}
        onClick={() => {
          const opts = ['X', 'Y', 'Z']
          const idx = opts.indexOf(menuSelectedOption)
          setMenuSelectedOption(opts[(idx + 1) % opts.length])
        }}
      />

      <AzRailItem id="loading" text="Load" onClick={() => setIsLoading((v) => !v)} />

      <AzDivider />

      {/* ---------- Host + sub items ---------- */}
      <AzMenuHostItem id="menu-host" text="Menu Host" onClick={() => console.log('menu host')} />
      <AzMenuSubItem id="menu-sub-1" hostId="menu-host" text="Menu Sub 1" onClick={() => console.log('menu sub 1')} />
      <AzMenuSubItem id="menu-sub-2" hostId="menu-host" text="Menu Sub 2" onClick={() => console.log('menu sub 2')} />

      <AzRailHostItem id="rail-host" text="Rail Host" onClick={() => console.log('rail host')} />
      <AzRailSubItem id="rail-sub-1" hostId="rail-host" text="Rail Sub 1" onClick={() => console.log('rail sub 1')} />

      {/* Nested host: a sub-item that is itself a host with its own sub-items. Children attach by
          `hostId`, so the sub-host's children are distinct from its sibling sub-items. */}
      <AzRailSubHostItem id="rail-subhost" hostId="rail-host" text="Rail Sub Host" onClick={() => console.log('rail sub host')} />
      <AzRailSubItem id="rail-subhost-1" hostId="rail-subhost" text="Nested A" onClick={() => console.log('nested A')} />
      <AzRailSubItem id="rail-subhost-2" hostId="rail-subhost" text="Nested B" onClick={() => console.log('nested B')} />

      <AzMenuSubHostItem id="menu-subhost" hostId="menu-host" text="Menu Sub Host" onClick={() => console.log('menu sub host')} />
      <AzMenuSubItem id="menu-subhost-1" hostId="menu-subhost" text="Nested 1" onClick={() => console.log('nested 1')} />
      <AzMenuSubItem id="menu-subhost-2" hostId="menu-subhost" text="Nested 2" onClick={() => console.log('nested 2')} />

      <AzMenuSubToggle
        id="sub-toggle"
        hostId="menu-host"
        text="Sub Toggle"
        isChecked={isDarkMode}
        toggleOnText="Sub Toggle On"
        toggleOffText="Sub Toggle Off"
        onClick={() => setIsDarkMode((v) => !v)}
      />

      <AzRailSubCycler
        id="sub-cycler"
        text="Sub Cycler"
        hostId="rail-host"
        options={['X', 'Y', 'Z']}
        selectedOption={menuSelectedOption}
        onClick={() => {
          const opts = ['X', 'Y', 'Z']
          const idx = opts.indexOf(menuSelectedOption)
          setMenuSelectedOption(opts[(idx + 1) % opts.length])
        }}
      />

      {/* ---------- Reloc items ---------- */}
      <AzRailRelocItem
        id="reloc-1"
        hostId="rail-host"
        text="Reloc 1"
        onRelocate={(from, to, newOrder) => console.log(`reloc-1: ${from}→${to}`, newOrder)}
        hiddenMenu={(scope) => {
          scope.listItem('Rename', () => console.log('rename'))
          scope.listItem('Pin', () => console.log('pin'))
        }}
      />

      <AzRailRelocItem
        id="reloc-nested-h"
        hostId="rail-host"
        text="Reloc + Horizontal Nested"
        onRelocate={(from, to, newOrder) => console.log(`reloc-nested-h: ${from}→${to}`, newOrder)}
        nestedRailAlignment={AzNestedRailAlignment.HORIZONTAL}
        hiddenMenu={(scope) => {
          scope.listItem('Remove', () => console.log('remove'))
        }}
        nestedContent={
          <>
            <AzRailItem id="nested-tool-h-1" text="Tool 1" onClick={() => console.log('h-tool-1')} />
            <AzRailItem id="nested-tool-h-2" text="Tool 2" onClick={() => console.log('h-tool-2')} />
          </>
        }
      />

      <AzRailRelocItem
        id="reloc-nested-v"
        hostId="rail-host"
        text="Reloc + Vertical Nested"
        onRelocate={(from, to, newOrder) => console.log(`reloc-nested-v: ${from}→${to}`, newOrder)}
        nestedRailAlignment={AzNestedRailAlignment.VERTICAL}
        hiddenMenu={(scope) => {
          scope.listItem('Remove', () => console.log('remove'))
        }}
        nestedContent={
          <>
            <AzRailItem id="nested-tool-v-1" text="Tool A" onClick={() => console.log('v-tool-a')} />
            <AzRailItem id="nested-tool-v-2" text="Tool B" onClick={() => console.log('v-tool-b')} />
          </>
        }
      />

      <AzNestedRail id="nested-rail" text="Vertical Nested" alignment={AzNestedRailAlignment.VERTICAL}>
        <AzRailItem id="nested-1" text="Nested 1" route="nested-1" info="A nested rail item." />
        <AzRailItem id="nested-2" text="Nested 2" route="nested-2" info="Another nested rail item." />
        <AzHelpRailItem id="nested-help" text="?" onClick={() => setShowHelp((v) => !v)} />
      </AzNestedRail>

      <AzNestedRail id="nested-horizontal" text="Horizontal Nested" alignment={AzNestedRailAlignment.HORIZONTAL}>
        <AzRailItem id="nested-h-1" text="H1" route="nested-h-1" info="First horizontal nested item." />
        <AzRailItem id="nested-h-2" text="H2" route="nested-h-2" info="Second horizontal nested item." />
        <AzRailItem id="nested-h-3" text="H3" route="nested-h-3" info="Third horizontal nested item." />
      </AzNestedRail>

      {/* ---------- Route-driven onscreen content ---------- */}
      <AzOnscreen alignment={AzAlignment.Center}>
        <div style={{ width: '100%', height: '100%', overflowY: 'auto' }}>
          <Routes>
            <Route path="/" element={<ShowcaseHome />} />
            <Route path="/home" element={<ShowcaseHome />} />
            <Route path="/bottom-sheet" element={<BottomSheetDemo />} />
            <Route path="/standalone" element={<StandaloneWidgets />} />
            <Route path="/customization" element={<CustomizationDemo state={customization} onChange={setCustomization} />} />
            <Route path="/forms" element={<FormDemo />} />
            <Route
              path="/help-system"
              element={
                <HelpSystemDemo
                  showHelp={showHelp}
                  onToggleHelp={() => setShowHelp((v) => !v)}
                  activeClassifiers={customization.activeClassifiers}
                  onClassifiersChange={(next) => setCustomization({ ...customization, activeClassifiers: next })}
                  dismissCount={dismissCount}
                />
              }
            />
            <Route path="/tutorial" element={<TutorialDemo />} />
            <Route path="/legacy" element={<LegacyPlayground />} />
            <Route path="*" element={<LegacyPlayground />} />
          </Routes>
        </div>
      </AzOnscreen>
    </AzHostActivityLayout>
  )
}

// Re-export for downstream tooling if needed.
export { DEMO_CLASSIFIERS }
export default App
