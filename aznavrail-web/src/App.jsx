import React, { useState } from 'react';
import {
  AzNavRail,
  AzButton,
  AzTextBox,
  AzToggle,
  AzCycler,
  AzForm
} from './components';
import './App.css';

function App() {
  const [currentPage, setCurrentPage] = useState('Home');
  const [toggleState, setToggleState] = useState(false);
  const [cyclerVal, setCyclerVal] = useState('Option 1');
  const [textVal, setTextVal] = useState('');
  const [formSubmitted, setFormSubmitted] = useState(null);
  const [infoScreen, setInfoScreen] = useState(false);

  const navItems = [
    {
      id: 'home',
      text: 'Home',
      isRailItem: true,
      onClick: () => setCurrentPage('Home'),
      info: 'Go to the home screen.'
    },
    {
      id: 'features',
      text: 'Features',
      isRailItem: true,
      info: 'Explore the various UI components provided by the library.',
      items: [
          { id: 'buttons', text: 'Buttons', onClick: () => setCurrentPage('Buttons') },
          { id: 'inputs', text: 'Inputs', onClick: () => setCurrentPage('Inputs') },
          { id: 'forms', text: 'Forms', onClick: () => setCurrentPage('Forms') },
      ]
    },
    {
        id: 'settings',
        text: 'Settings',
        isRailItem: true,
        info: 'Configure application settings.',
        items: [
            {
                id: 'theme',
                text: 'Theme',
                isCycler: true,
                options: ['Light', 'Dark', 'System'],
                selectedOption: 'System',
                onClick: (val) => console.log('Theme:', val)
            }
        ]
    }
  ];

  return (
    <div className="app-container" style={{ display: 'flex', height: '100vh' }}>
      <AzNavRail
        initiallyExpanded={false}
        content={navItems}
        settings={{
            appName: 'Demo App',
            infoScreen: infoScreen,
            onDismissInfoScreen: () => setInfoScreen(false)
        }}
      />
      <main style={{ flex: 1, padding: '20px', overflowY: 'auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h1>{currentPage}</h1>
            <button onClick={() => setInfoScreen(!infoScreen)}>
                {infoScreen ? 'Exit Help' : 'Help Mode'}
            </button>
        </div>

        {currentPage === 'Home' && (
            <div className="demo-section">
                <p>Welcome to the AzNavRail Web Demo.</p>
                <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    <AzButton text="Click Me" onClick={() => alert('Clicked!')} />
                    <AzButton text="Loading" isLoading={true} />
                    <AzButton text="Square" shape="SQUARE" onClick={() => {}} />
                </div>
            </div>
        )}

        {currentPage === 'Buttons' && (
             <div className="demo-section">
                 <h3>Standalone Buttons</h3>
                 <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                     <AzButton text="Rectangle" shape="RECTANGLE" onClick={() => {}} />
                     <AzButton text="Rounded" shape="ROUNDED" onClick={() => {}} />
                     <AzButton text="None" shape="NONE" onClick={() => {}} />
                     <AzButton text="Circle" shape="CIRCLE" onClick={() => {}} />
                     <AzButton text="Disabled" enabled={false} onClick={() => {}} />
                 </div>
                 <h3>Toggles & Cyclers</h3>
                 <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                     <AzToggle
                        value={toggleState}
                        onValueChange={setToggleState}
                        label={`Toggle is ${toggleState ? 'ON' : 'OFF'}`}
                     />
                     <AzCycler
                        label="Cycler:"
                        options={['Option 1', 'Option 2', 'Option 3']}
                        value={cyclerVal}
                        onValueChange={setCyclerVal}
                     />
                 </div>
             </div>
        )}

        {currentPage === 'Inputs' && (
            <div className="demo-section">
                <h3>Text Boxes</h3>
                <AzTextBox
                    value={textVal}
                    onValueChange={setTextVal}
                    hint="Type something..."
                    suggestions={['Hello', 'World', 'AzNavRail', 'React']}
                />
                <AzTextBox
                    value="Password123"
                    onValueChange={() => {}}
                    secret={true}
                    hint="Password"
                />
                <AzTextBox
                    value="Multiline text..."
                    onValueChange={() => {}}
                    multiline={true}
                />
            </div>
        )}

        {currentPage === 'Forms' && (
            <div className="demo-section">
                <h3>AzForm</h3>
                <AzForm
                    entries={[
                        { name: 'username', hint: 'Username' },
                        { name: 'password', hint: 'Password', secret: true }
                    ]}
                    onSubmit={setFormSubmitted}
                />
                {formSubmitted && (
                    <pre>{JSON.stringify(formSubmitted, null, 2)}</pre>
                )}
            </div>
        )}

      </main>
    </div>
  );
}

export default App;
