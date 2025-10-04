import React, { useState } from 'react';
import AzNavRail from './components/AzNavRail';
import './App.css';

function App() {
  const [isChecked, setIsChecked] = useState(false);
  const [selectedOption, setSelectedOption] = useState('Option A');

  const navItems = [
    {
      id: 'home',
      text: 'Home',
      isRailItem: true,
      onClick: () => console.log('Home clicked'),
    },
    {
      id: 'toggle',
      isRailItem: true,
      isToggle: true,
      isChecked: isChecked,
      toggleOnText: 'On',
      toggleOffText: 'Off',
      onClick: () => setIsChecked(!isChecked),
      color: 'green',
    },
    {
      id: 'cycler',
      isRailItem: true,
      isCycler: true,
      options: ['Option A', 'Option B', 'Option C'],
      selectedOption: selectedOption,
      onClick: (option) => setSelectedOption(option),
      color: 'purple',
    },
    {
      id: 'about',
      text: 'About',
      onClick: () => console.log('About clicked'),
    },
  ];

  const settings = {
    appName: 'My Web App',
    displayAppNameInHeader: true,
    // Add other settings as needed
  };

  return (
    <div className="App">
      <AzNavRail content={navItems} settings={settings} />
      <main className="main-content">
        <h1>Welcome to the App</h1>
        <p>The toggle is currently: {isChecked ? 'On' : 'Off'}</p>
        <p>The selected cycler option is: {selectedOption}</p>
      </main>
    </div>
  );
}

export default App;