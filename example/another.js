import React from 'react';

import {StyleSheet, View, TouchableOpacity} from 'react-native';

export default function Another(props) {
  const onDeatch = () => {
    props.navigation.goBack();
  };

  return (
    <View style={styles.container}>
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
