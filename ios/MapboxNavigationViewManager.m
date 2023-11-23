#import <React/RCTViewManager.h>

@interface RCT_EXTERN_REMAP_MODULE(MapboxNavigationView, MapboxNavigationViewManager, RCTViewManager)
RCT_EXPORT_VIEW_PROPERTY(onLocationChange, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onRouteProgressChange, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onError, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onCancelNavigation, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onArrive, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(shouldSimulateRoute, BOOL)
RCT_EXPORT_VIEW_PROPERTY(showsEndOfRouteFeedback, BOOL)
RCT_EXPORT_VIEW_PROPERTY(hideStatusView, BOOL)
RCT_EXPORT_VIEW_PROPERTY(mute, BOOL)

RCT_REMAP_VIEW_PROPERTY(edge, reactEdge, NSDictionary)
RCT_REMAP_VIEW_PROPERTY(destination, reactDestination, NSArray)
RCT_REMAP_VIEW_PROPERTY(origin, reactOrigin, NSArray)
@end
