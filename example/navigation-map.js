import React, {useEffect, useState} from 'react';

import {
  StyleSheet,
  View,
  PermissionsAndroid,
  Platform,
  TouchableOpacity,
  Text,
} from 'react-native';
import {MapboxNavigation} from 'bg-react-native-mapbox-navigation';

export default function NavigationMap(props) {
  const {navigation} = props;
  const [destination, setDestination] = useState([73.030259, 26.282121]);
  const edge = {top: 10, bottom: 20};

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

  const onNext = () => {
    navigation.navigate('EmptyScreen');
  };

  return (
    <View style={styles.container}>
      <MapboxNavigation
        style={styles.container}
        origin={[73.0323619, 26.2864074]}
        destination={destination}
        edge={edge}
      />
      <TouchableOpacity onPress={onNext} style={styles.btn}>
        <Text>Next</Text>
      </TouchableOpacity>
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
  btn: {
    width: '100%',
    height: 48,
    justifyContent: 'center',
    alignItems: 'center',
  },
});
