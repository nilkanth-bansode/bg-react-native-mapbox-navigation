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

export default function App() {
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

  const onDeatch = () => {};

  return (
    <View style={styles.container}>
      <MapboxNavigation
        style={styles.container}
        origin={[73.0323619, 26.2864074]}
        destination={[73.030259, 26.282121]}
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
