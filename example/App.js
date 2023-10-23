import React, {useEffect, useState} from 'react';

import {
  StyleSheet,
  View,
  PermissionsAndroid,
  Platform,
  Alert,
  Button,
} from 'react-native';
import {MapboxNavigation} from 'bg-react-native-mapbox-navigation';

export default function App() {
  useEffect(() => {
    Platform.OS === 'android' && requestLocationPermission();
  }, []);

  const requestLocationPermission = async () => {
    try {
      await PermissionsAndroid.request(
        'android.permission.ACCESS_FINE_LOCATION',
        {
          title: 'Example App',
          message: 'Example App access to your location',
          buttonPositive: 'OK',
        },
      );
    } catch (err) {
      console.warn(err);
    }
  };

  return (
    <View style={styles.container}>
      <MapboxNavigation
        origin={[73.0336933, 26.2841672]}
        destination={[73.031205, 26.270589]}
        style={styles.box}
        shouldSimulateRoute={false}
        showsEndOfRouteFeedback={false}
        hideStatusView={false}
        onLocationChange={event => {
          console.log('onLocationChange', event.nativeEvent);
        }}
        onRouteProgressChange={event => {
          console.log('onRouteProgressChange', event.nativeEvent);
        }}
        onArrive={() => {
          Alert.alert('You have reached your destination');
        }}
        onCancelNavigation={() => {
          Alert.alert('Cancelled navigation event');
        }}
        onError={event => {
          const message = event?.nativeEvent?.message;
          if (message) {
            Alert.alert(message);
          }
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  box: {
    flex: 1,
  },
});
