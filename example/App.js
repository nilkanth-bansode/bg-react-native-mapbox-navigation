import React, {useEffect, useState} from 'react';

import {
  StyleSheet,
  View,
  PermissionsAndroid,
  Platform,
  Alert,
  Button,
} from 'react-native';
import {MapboxNavigation} from 'react-native-mapbox-navigation';

export default function App() {
  const [origin, setOrigin] = useState([72.8797, 19.17]);
  const [destination, setDestination] = useState([72.8612, 19.1728]);
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
        origin={[72.8797, 19.17]}
        // origin={[19.17, 72.8797]}
        destination={[72.8612, 19.1728]}
        // destination={[19.1728, 72.8612]}
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
      <View
        style={{
          flexDirection: 'row',
          height: 60,
          width: '100%',
          backgroundColor: 'blue',
        }}>
        <View style={{width: '100%', flex: 1}}>
          <Button
            onPress={() => {}}
            title="Learn More"
            color="#841584"
            accessibilityLabel="Learn more about this purple button"
          />
        </View>
        <View style={{width: '100%', flex: 1}}>
          <Button
            onPress={() => {}}
            title="Learn More"
            color="#841584"
            accessibilityLabel="Learn more about this purple button"
          />
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'green',
  },
  box: {
    flex: 1,
    // flex: 1,
    marginVertical: 20,
  },
});
