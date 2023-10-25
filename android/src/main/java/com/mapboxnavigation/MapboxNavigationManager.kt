package com.mapboxnavigation

import android.content.pm.PackageManager
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.google.android.material.internal.ViewUtils.RelativePadding
import com.mapbox.geojson.Point
import com.mapbox.maps.ResourceOptionsManager
import com.mapbox.maps.TileStoreUsageMode
import javax.annotation.Nonnull

class MapboxNavigationManager(var mCallerContext: ReactApplicationContext) :
  SimpleViewManager<MapboxNavigationView>() {
  private var accessToken: String? = null

  init {
    mCallerContext.runOnUiQueueThread {
      try {
        val app = mCallerContext.packageManager.getApplicationInfo(
          mCallerContext.packageName,
          PackageManager.GET_META_DATA
        )
        val bundle = app.metaData
        val accessToken = bundle.getString("MAPBOX_ACCESS_TOKEN")
        this.accessToken = accessToken
        ResourceOptionsManager.getDefault(mCallerContext, accessToken).update {
          tileStoreUsageMode(TileStoreUsageMode.READ_ONLY)
        }
      } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
      }
    }
  }

  override fun getName(): String {
    return "MapboxNavigation"
  }

  public override fun createViewInstance(@Nonnull reactContext: ThemedReactContext): MapboxNavigationView {
    return MapboxNavigationView(reactContext, this.accessToken)
  }

  override fun onDropViewInstance(view: MapboxNavigationView) {
    view.onDropViewInstance()
    super.onDropViewInstance(view)
  }

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Map<String, String>>? {
    return MapBuilder.of<String, Map<String, String>>(
      "onLocationChange", MapBuilder.of("registrationName", "onLocationChange"),
      "onError", MapBuilder.of("registrationName", "onError"),
      "onCancelNavigation", MapBuilder.of("registrationName", "onCancelNavigation"),
      "onArrive", MapBuilder.of("registrationName", "onArrive"),
      "onRouteProgressChange", MapBuilder.of("registrationName", "onRouteProgressChange"),
    )
  }

  @ReactProp(name = "origin")
  fun setOrigin(view: MapboxNavigationView, sources: ReadableArray?) {
    if (sources == null) {
      view.setOrigin(null)
      return
    }
    view.setOrigin(Point.fromLngLat(sources.getDouble(0), sources.getDouble(1)))
  }

  @ReactProp(name = "destination")
  fun setDestination(view: MapboxNavigationView, sources: ReadableArray?) {
    if (sources == null) {
      view.setDestination(null)
      return
    }
    view.setDestination(Point.fromLngLat(sources.getDouble(0), sources.getDouble(1)))
  }

  @ReactProp(name = "shouldSimulateRoute")
  fun setShouldSimulateRoute(view: MapboxNavigationView, shouldSimulateRoute: Boolean) {
    view.setShouldSimulateRoute(shouldSimulateRoute)
  }

  @ReactProp(name = "showsEndOfRouteFeedback")
  fun setShowsEndOfRouteFeedback(view: MapboxNavigationView, showsEndOfRouteFeedback: Boolean) {
    view.setShowsEndOfRouteFeedback(showsEndOfRouteFeedback)
  }

  @ReactProp(name = "mapEdge")
  fun setMapEdge(view: MapboxNavigationView, edge: ReadableMap) {
    view.setMapEdge(edge);
  }

  @ReactProp(name = "edge")
  fun setEdge(view: MapboxNavigationView, edge: ReadableMap){
    view.setEdge(edge);
  }

  @ReactProp(name = "mute")
  fun setMute(view: MapboxNavigationView, mute: Boolean) {
    view.setMute(mute)
  }

}
