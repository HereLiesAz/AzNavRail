import React from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';

interface AzLoadProps {
    size?: number | 'small' | 'large';
    color?: string;
}

export const AzLoad: React.FC<AzLoadProps> = ({ size = "large", color = "#6200ee" }) => (
    <View style={styles.container}>
        <ActivityIndicator size={size} color={color} />
    </View>
);

const styles = StyleSheet.create({
    container: {
        width: 80,
        height: 80, // Square container
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: 'white',
        borderRadius: 10,
        elevation: 5,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.25,
        shadowRadius: 3.84,
    }
});
