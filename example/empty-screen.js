import React from 'react';
import {StyleSheet, Text, TouchableOpacity, View} from 'react-native';

export default function EmptyScreen(props) {
  const {navigation} = props;
  const onClick = () => {
    navigation.pop();
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity onPress={onClick} style={styles.btn}>
        <Text>Back</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'blue',
  },
  btn: {
    width: '100%',
    height: 48,
    backgroundColor: 'gray',
    justifyContent: 'center',
    alignItems: 'center',
  },
});
