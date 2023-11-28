import React from 'react';

import {StyleSheet, View} from 'react-native';
import {NavigationContainer} from '@react-navigation/native';

import {createNativeStackNavigator} from '@react-navigation/native-stack';
import NavigationMap from './navigation-map';
import EmptyScreen from './empty-screen';
import NextScreen from './next-screen';

const Stack = createNativeStackNavigator();

export default function App() {
  return (
    <View style={styles.container}>
      <NavigationContainer>
        <Stack.Navigator>
          <Stack.Screen name="NavigationMap" component={NavigationMap} />
          <Stack.Screen name="EmptyScreen" component={EmptyScreen} />
          <Stack.Screen name="NextScreen" component={NextScreen} />
        </Stack.Navigator>
      </NavigationContainer>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
