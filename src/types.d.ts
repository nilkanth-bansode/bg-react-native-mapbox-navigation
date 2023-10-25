import type { ViewStyle } from 'react-native';

/** @type {[number, number]}
 * Provide an array with longitude and latitude [$longitude, $latitude]
 */
export declare type Coordinate = [number, number];
export declare type OnLocationChangeEvent = {
  nativeEvent?: {
    latitude: number;
    longitude: number;
  };
};

export declare type OnRouteProgressChangeEvent = {
  nativeEvent?: {
    distanceTraveled: number;
    durationRemaining: number;
    fractionTraveled: number;
    distanceRemaining: number;
  };
};

export declare type OnErrorEvent = {
  nativeEvent?: {
    message?: string;
  };
};

export declare type EdgeInsets = {
  top: number;
  left: number;
  right: number;
  bottom: number;
};

export interface IMapboxNavigationProps {
  origin: Coordinate;
  destination: Coordinate;
  shouldSimulateRoute?: boolean;
  onLocationChange?: (event: OnLocationChangeEvent) => void;
  onRouteProgressChange?: (event: OnRouteProgressChangeEvent) => void;
  onError?: (event: OnErrorEvent) => void;
  onCancelNavigation?: () => void;
  onArrive?: () => void;
  showsEndOfRouteFeedback?: boolean;
  hideStatusView?: boolean;
  mute?: boolean;
  style?: ViewStyle;
  padding?: EdgeInsets;
}
export {};
