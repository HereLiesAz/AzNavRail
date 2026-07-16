import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { AzNavRail, AzNavRailButton, AzButton, AzForm, AzTextBox, AzRoller, AzNavRailScope } from '@HereLiesAz/aznavrail-react';

function App() {
  return (
    <View style={styles.container}>
      <Text style={styles.header}>AzNavRail Components</Text>
      
      <View style={styles.section}>
        <Text style={styles.title}>AzNavRail (Docked)</Text>
        <AzNavRailScope id="demo-rail">
          <View style={{ height: 600, width: '100%', flexDirection: 'row' }}>
            <AzNavRail
              isDocked={true}
              railMode="Docked"
              items={[
                { id: '1', label: 'Home', icon: () => <View style={styles.icon}/> },
                { id: '2', label: 'Settings', icon: () => <View style={styles.icon}/> }
              ]}
            />
          </View>
        </AzNavRailScope>
      </View>
      
      <View style={styles.section}>
        <Text style={styles.title}>AzButton</Text>
        <AzButton label="Click Me" onClick={() => {}} />
      </View>

    </View>
  );
}

const styles = StyleSheet.create({
  container: { padding: 20, flex: 1, backgroundColor: '#f0f0f0' },
  header: { fontSize: 24, fontWeight: 'bold', marginBottom: 20 },
  section: { marginBottom: 40 },
  title: { fontSize: 18, marginBottom: 10, fontWeight: '600' },
  icon: { width: 24, height: 24, backgroundColor: 'gray', borderRadius: 12 }
});

export default App;
