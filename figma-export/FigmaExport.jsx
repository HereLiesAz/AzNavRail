import React from 'react';
import { createRoot } from 'react-dom/client';
import { View, Text } from 'react-native-web';

// Import the components directly from the library source since we don't have it installed via npm
import { AzButton } from '../aznavrail-react/src/components/AzButton.tsx';
import { AzTextBox } from '../aznavrail-react/src/components/AzTextBox.tsx';
import { AzToggle } from '../aznavrail-react/src/components/AzToggle.tsx';
import { AzDivider } from '../aznavrail-react/src/components/AzDivider.tsx';
import { AzLoad } from '../aznavrail-react/src/components/AzLoad.tsx';

const FigmaExport = () => {
  return (
    <>
      <div className="component-row">
        <h2>AzButton Variants</h2>
        <AzButton text="Circle" shape="CIRCLE" color="#6200ee" onClick={() => {}} />
        <AzButton text="Square" shape="SQUARE" color="#6200ee" onClick={() => {}} />
        <AzButton text="Rectangle" shape="RECTANGLE" color="#6200ee" onClick={() => {}} />
        <AzButton text="Disabled" shape="RECTANGLE" color="#6200ee" enabled={false} onClick={() => {}} />
        <AzButton text="Loading" shape="CIRCLE" color="#6200ee" isLoading={true} onClick={() => {}} />
      </div>

      <div className="component-row">
        <h2>AzTextBox</h2>
        <View style={{ width: 300 }}>
          <AzTextBox
            initialValue=""
            hint="Enter your name"
            color="#6200ee"
            onValueChange={() => {}}
          />
        </View>
        <View style={{ width: 300 }}>
          <AzTextBox
            initialValue="Secret Password"
            hint="Password"
            secret={true}
            color="#6200ee"
            onValueChange={() => {}}
          />
        </View>
      </div>

      <div className="component-row">
        <h2>AzToggle</h2>
        <AzToggle
          label="Wi-Fi (Off)"
          color="#6200ee"
          value={false}
          onValueChange={() => {}}
        />
        <AzToggle
          label="Wi-Fi (On)"
          color="#6200ee"
          value={true}
          onValueChange={() => {}}
        />
      </div>

      <div className="component-row">
        <h2>AzDivider</h2>
        <View style={{ width: 300, padding: 20, backgroundColor: '#f0f0f0' }}>
          <Text style={{ color: '#6200ee', marginBottom: 10 }}>Content Above</Text>
          <AzDivider color="#6200ee" />
          <Text style={{ color: '#6200ee', marginTop: 10 }}>Content Below</Text>
        </View>
      </div>

      <div className="component-row">
        <h2>AzLoad</h2>
        <AzLoad color="#6200ee" size={48} />
      </div>
    </>
  );
};

const root = createRoot(document.getElementById('root'));
root.render(<FigmaExport />);