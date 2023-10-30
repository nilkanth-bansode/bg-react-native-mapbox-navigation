import React, {useEffect} from 'react';

import {
  StyleSheet,
  View,
  PermissionsAndroid,
  Platform,
  Alert,
  TouchableOpacity,
} from 'react-native';
import {MapboxNavigation} from 'bg-react-native-mapbox-navigation';

export default function App(props) {
  const destination = [73.031205, 26.270589];
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

  const onDeatch = () => {
    props.navigation.navigate('another');
  };

  return (
    <View style={styles.container}>
      <MapboxNavigation
        origin={[73.0336933, 26.2841672]}
        destination={destination}
        style={styles.box}
        shouldSimulateRoute={false}
        showsEndOfRouteFeedback={false}
        hideStatusView={false}
        onLocationChange={event => {
          console.log('onLocationChange', event.nativeEvent);
        }}
        mapEdge={{top: 100, right: 0, left: 0, bottom: 300}}
        edge={{top: 16, bottom: 16}}
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
      <TouchableOpacity
        onPress={onDeatch}
        style={{width: '100%', height: 54, backgroundColor: 'gray'}}
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
