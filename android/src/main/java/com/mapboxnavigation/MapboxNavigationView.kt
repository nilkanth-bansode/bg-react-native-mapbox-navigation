package com.mapboxnavigation

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.dropin.internal.extensions.updateMargins
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressViewOptions
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapboxnavigation.databinding.NavigationViewBinding
import java.util.Locale

@SuppressLint("ViewConstructor")
class MapboxNavigationView(
  private val context: ThemedReactContext,
  private val accessToken: String?
) :
  FrameLayout(context.baseContext), LifecycleOwner {

  private val CLASS_NAME = "MapboxNavigationView"
  private lateinit var lifecycleRegistry: LifecycleRegistry

  private companion object {
    private const val BUTTON_ANIMATION_DURATION = 1500L
  }

  private var origin: Point? = null
  private var destination: Point? = null
  private var shouldSimulateRoute = false
  private var showsEndOfRouteFeedback = false

  /**
   * Bindings to the example layout.
   */
  private var binding: NavigationViewBinding =
    NavigationViewBinding.inflate(LayoutInflater.from(context), this, true)

  /**
   * Mapbox Maps entry point obtained from the [MapView].
   * You need to get a new reference to this object whenever the [MapView] is recreated.
   */
  private lateinit var mapboxMap: MapboxMap

  /**
   * Used to execute camera transitions based on the data generated by the [viewportDataSource].
   * This includes transitions from route overview to route following and continuously updating the camera as the location changes.
   */
  private lateinit var navigationCamera: NavigationCamera

  /**
   * Produces the camera frames based on the location and routing data for the [navigationCamera] to execute.
   */
  private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

  /*
   * Below are generated camera padding values to ensure that the route fits well on screen while
   * other elements are overlaid on top of the map (including instruction view, buttons, etc.)
   */
  private val pixelDensity = Resources.getSystem().displayMetrics.density
  private var padding: EdgeInsets = EdgeInsets(
    140.0 * pixelDensity,
    40.0 * pixelDensity,
    120.0 * pixelDensity,
    40.0 * pixelDensity
  )

  /**
   * Generates updates for the [MapboxManeuverView] to display the upcoming maneuver instructions
   * and remaining distance to the maneuver point.
   */
  private lateinit var maneuverApi: MapboxManeuverApi

  /**
   * Generates updates for the [MapboxTripProgressView] that include remaining time and distance to the destination.
   */
  private lateinit var tripProgressApi: MapboxTripProgressApi

  /**
   * Generates updates for the [routeLineView] with the geometries and properties of the routes that should be drawn on the map.
   */
  private lateinit var routeLineApi: MapboxRouteLineApi

  /**
   * Draws route lines on the map based on the data from the [routeLineApi]
   */
  private lateinit var routeLineView: MapboxRouteLineView

  /**
   * Generates updates for the [routeArrowView] with the geometries and properties of maneuver arrows that should be drawn on the map.
   */
  private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()

  /**
   * Draws maneuver arrows on the map based on the data [routeArrowApi].
   */
  private lateinit var routeArrowView: MapboxRouteArrowView

  /**
   * Stores and updates the state of whether the voice instructions should be played as they come or muted.
   */
  private var isVoiceInstructionsMuted = false
    set(value) {
      field = value
      if (value) {
        binding.soundButton.muteAndExtend(BUTTON_ANIMATION_DURATION)
        voiceInstructionsPlayer.volume(SpeechVolume(0f))
      } else {
        binding.soundButton.unmuteAndExtend(BUTTON_ANIMATION_DURATION)
        voiceInstructionsPlayer.volume(SpeechVolume(1f))
      }
    }

  /**
   * Extracts message that should be communicated to the driver about the upcoming maneuver.
   * When possible, downloads a synthesized audio file that can be played back to the driver.
   */
  private lateinit var speechApi: MapboxSpeechApi

  /**
   * Plays the synthesized audio files with upcoming maneuver instructions
   * or uses an on-device Text-To-Speech engine to communicate the message to the driver.
   */
  private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

  /**
   * Observes when a new voice instruction should be played.
   */
  private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
    speechApi.generate(voiceInstructions, speechCallback)
  }

  /**
   * Based on whether the synthesized audio file is available, the callback plays the file
   * or uses the fall back which is played back using the on-device Text-To-Speech engine.
   */
  private val speechCallback =
    MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
      expected.fold(
        { error ->
          // play the instruction via fallback text-to-speech engine
          voiceInstructionsPlayer.play(
            error.fallback,
            voiceInstructionsPlayerCallback
          )
        },
        { value ->
          // play the sound file from the external generator
          voiceInstructionsPlayer.play(
            value.announcement,
            voiceInstructionsPlayerCallback
          )
        }
      )
    }

  /**
   * When a synthesized audio file was downloaded, this callback cleans up the disk after it was played.
   */
  private val voiceInstructionsPlayerCallback =
    MapboxNavigationConsumer<SpeechAnnouncement> { value ->
      // remove already consumed file to free-up space
      speechApi.clean(value)
    }

  /**
   * [NavigationLocationProvider] is a utility class that helps to provide location updates generated by the Navigation SDK
   * to the Maps SDK in order to update the user location indicator on the map.
   */
  private val navigationLocationProvider = NavigationLocationProvider()

  /**
   * Gets notified with location updates.
   *
   * Exposes raw updates coming directly from the location services
   * and the updates enhanced by the Navigation SDK (cleaned up and matched to the road).
   */
  private val locationObserver = object : LocationObserver {
    var firstLocationUpdateReceived = false

    override fun onNewRawLocation(rawLocation: Location) {
      // not handled
    }

    override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
      val enhancedLocation = locationMatcherResult.enhancedLocation

      // update location puck's position on the map
      navigationLocationProvider.changePosition(
        location = enhancedLocation,
        keyPoints = locationMatcherResult.keyPoints,
      )

      // update camera position to account for new location
      viewportDataSource.onLocationChanged(enhancedLocation)
      viewportDataSource.evaluate()

      val event = Arguments.createMap()
      event.putDouble("longitude", enhancedLocation.longitude)
      event.putDouble("latitude", enhancedLocation.latitude)
      context
        .getJSModule(RCTEventEmitter::class.java)
        .receiveEvent(id, "onLocationChange", event)

      // if this is the first location update the activity has received,
      // it's best to immediately move the camera to the current user location
      if (!firstLocationUpdateReceived) {
        firstLocationUpdateReceived = true
        navigationCamera.requestNavigationCameraToFollowing(
          stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
            .maxDuration(0) // instant transition
            .build()
        )
      }
    }
  }

  /**
   * Gets notified with progress along the currently active route.
   */
  private val routeProgressObserver = RouteProgressObserver { routeProgress ->
    // update the camera position to account for the progressed fragment of the route
    viewportDataSource.onRouteProgressChanged(routeProgress)
    viewportDataSource.evaluate()

    // draw the upcoming maneuver arrow on the map
    val style = binding.mapView.getMapboxMap().getStyle()
    if (style != null) {
      val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
      routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
    }

    // update top banner with maneuver instructions
    val maneuvers = maneuverApi.getManeuvers(routeProgress)
    maneuvers.fold(
      { error ->
        Toast.makeText(
          context,
          error.errorMessage,
          Toast.LENGTH_SHORT
        ).show()
      },
      {
        binding.maneuverView.visibility = View.VISIBLE
        binding.maneuverView.renderManeuvers(maneuvers)
      }
    )

    // update bottom trip progress summary
    binding.tripProgressView.render(
      tripProgressApi.getTripProgress(routeProgress)
    )

    val event = Arguments.createMap()
    event.putDouble("distanceTraveled", routeProgress.distanceTraveled.toDouble())
    event.putDouble("durationRemaining", routeProgress.durationRemaining.toDouble())
    event.putDouble("fractionTraveled", routeProgress.fractionTraveled.toDouble())
    event.putDouble("distanceRemaining", routeProgress.distanceRemaining.toDouble())
    context
      .getJSModule(RCTEventEmitter::class.java)
      .receiveEvent(id, "onRouteProgressChange", event)
  }

  /**
   * Gets notified whenever the tracked routes change.
   *
   * A change can mean:
   * - routes get changed with [MapboxNavigation.setRoutes]
   * - routes annotations get refreshed (for example, congestion annotation that indicate the live traffic along the route)
   * - driver got off route and a reroute was executed
   */
  private val routesObserver = RoutesObserver { routeUpdateResult ->
    Log.d(CLASS_NAME, "routesObserver routeUpdateResult:" + routeUpdateResult.toString())
    val style = binding.mapView.getMapboxMap().getStyle()
    if (routeUpdateResult.navigationRoutes.isNotEmpty()) {
      // generate route geometries asynchronously and render them
      routeLineApi.setNavigationRoutes(
        routeUpdateResult.navigationRoutes
      ) { value ->
        Log.d(CLASS_NAME, "style " + style.toString())
        style?.apply {
          routeLineView.renderRouteDrawData(style, value)
        }
      }

      // update the camera position to account for the new route
      viewportDataSource.onRouteChanged(routeUpdateResult.navigationRoutes.first())
      viewportDataSource.evaluate()
    } else {
      // remove the route line and route arrow from the map
      if (style != null) {
        routeLineApi.clearRouteLine { value ->
          routeLineView.renderClearRouteLineValue(
            style,
            value
          )
        }
        routeArrowView.render(style, routeArrowApi.clearArrows())
      }

      // remove the route reference from camera position evaluations
      viewportDataSource.clearRouteData()
      viewportDataSource.evaluate()
    }
  }


  private val arrivalObserver = object : ArrivalObserver {

    override fun onWaypointArrival(routeProgress: RouteProgress) {
      // do something when the user arrives at a waypoint
    }

    override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
      // do something when the user starts a new leg
    }

    override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
      val event = Arguments.createMap()
      event.putString("onArrive", "")
      context
        .getJSModule(RCTEventEmitter::class.java)
        .receiveEvent(id, "onRouteProgressChange", event)
    }
  }

  private val measureAndLayout = Runnable {
    measure(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
    )
    layout(left, top, right, bottom)
  }

  @SuppressLint("MissingPermission")
  fun onCreate() {
    Log.d(CLASS_NAME, accessToken.toString());
    Log.d(CLASS_NAME, "onCreate: origin" + this.origin.toString());
    Log.d(CLASS_NAME, "onCreate: destination" + this.destination.toString());
    if (accessToken == null) {
      sendErrorToReact("Mapbox access token is not set")
      return
    }

    if (origin == null || destination == null) {
      sendErrorToReact("origin and destination are required")
      return
    }

    // initialize location puck
    binding.mapView.location.apply {
      setLocationProvider(navigationLocationProvider)
      this.locationPuck = LocationPuck2D(
        bearingImage = ContextCompat.getDrawable(
          context,
          R.drawable.mapbox_navigation_puck_icon
        )
      )
      enabled = true
    }

    // initialize Navigation Camera
    viewportDataSource = MapboxNavigationViewportDataSource(binding.mapView.getMapboxMap())
    navigationCamera = NavigationCamera(
      binding.mapView.getMapboxMap(),
      binding.mapView.camera,
      viewportDataSource
    )
    // set the animations lifecycle listener to ensure the NavigationCamera stops
    // automatically following the user location when the map is interacted with
    binding.mapView.camera.addCameraAnimationsLifecycleListener(
      NavigationBasicGesturesHandler(navigationCamera)
    )
    navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
      // shows/hide the recenter button depending on the camera state
      when (navigationCameraState) {
        NavigationCameraState.TRANSITION_TO_FOLLOWING,
        NavigationCameraState.FOLLOWING -> binding.recenter.visibility = View.INVISIBLE

        NavigationCameraState.TRANSITION_TO_OVERVIEW,
        NavigationCameraState.OVERVIEW,
        NavigationCameraState.IDLE -> binding.recenter.visibility = View.VISIBLE
      }
    }

    // make sure to use the same DistanceFormatterOptions across different features
    val distanceFormatterOptions = DistanceFormatterOptions.Builder(context).build()

    // initialize maneuver api that feeds the data to the top banner maneuver view
    maneuverApi = MapboxManeuverApi(
      MapboxDistanceFormatter(distanceFormatterOptions)
    )

    // initialize bottom progress view
    tripProgressApi = MapboxTripProgressApi(
      TripProgressUpdateFormatter.Builder(context)
        .distanceRemainingFormatter(
          DistanceRemainingFormatter(distanceFormatterOptions)
        )
        .timeRemainingFormatter(
          TimeRemainingFormatter(context)
        )
        .percentRouteTraveledFormatter(
          PercentDistanceTraveledFormatter()
        )
        .estimatedTimeToArrivalFormatter(
          EstimatedTimeToArrivalFormatter(context, TimeFormat.NONE_SPECIFIED)
        )
        .build()
    )

    // initialize voice instructions api and the voice instruction player
    speechApi = MapboxSpeechApi(
      context,
      accessToken,
      Locale.US.language
    )
    voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
      context,
      accessToken,
      Locale.US.language
    )

    // initialize route line, the withRouteLineBelowLayerId is specified to place
    // the route line below road labels layer on the map
    // the value of this option will depend on the style that you are using
    // and under which layer the route line should be placed on the map layers stack
    val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(context)
      .withRouteLineBelowLayerId("road-label-navigation")
      .build()
    routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
    routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

    // initialize maneuver arrow view to draw arrows on the map
    val routeArrowOptions = RouteArrowOptions.Builder(context).build()
    routeArrowView = MapboxRouteArrowView(routeArrowOptions)

    // load map style
    binding.mapView.getMapboxMap().loadStyleUri(NavigationStyles.NAVIGATION_NIGHT_STYLE)

    binding.maneuverView.updatePrimaryManeuverTextAppearance(R.style.ManeuverTextAppearance)
    binding.maneuverView.updateSecondaryManeuverTextAppearance(R.style.ManeuverTextAppearance)
    binding.maneuverView.updateSubManeuverTextAppearance(R.style.ManeuverTextAppearance)
    binding.maneuverView.updateStepDistanceTextAppearance(R.style.StepDistanceRemainingAppearance)

    val optionBuild: TripProgressViewOptions.Builder = TripProgressViewOptions.Builder();
    optionBuild.backgroundColor(R.color.stratos);
    optionBuild.timeRemainingTextAppearance(R.style.TimeRemainingTextAppearance);
    optionBuild.distanceRemainingTextAppearance(R.style.ManeuverTextAppearance);
    optionBuild.estimatedArrivalTimeTextAppearance(R.style.ManeuverTextAppearance);

    binding.tripProgressView.updateOptions(optionBuild.build());

    // initialize view interactions
    binding.stop.setOnClickListener {
      clearRouteAndStopNavigation()
    }

    binding.recenter.setOnClickListener {
      navigationCamera.requestNavigationCameraToFollowing()
      binding.routeOverview.showTextAndExtend(BUTTON_ANIMATION_DURATION)
    }
    binding.routeOverview.setOnClickListener {
      navigationCamera.requestNavigationCameraToOverview()
      binding.recenter.showTextAndExtend(BUTTON_ANIMATION_DURATION)
    }
    binding.soundButton.setOnClickListener {
      // mute/unmute voice instructions
      isVoiceInstructionsMuted = !isVoiceInstructionsMuted
    }

    // set initial sounds button state
    binding.soundButton.unmute()

    this.applyProps()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    Log.d(CLASS_NAME, "onAttachedToWindow");
    lifecycleRegistry = LifecycleRegistry(this)
    lifecycleRegistry.currentState = Lifecycle.State.CREATED
    lifecycleRegistry.currentState = Lifecycle.State.STARTED
    initNavigation()
    MapboxNavigationApp.attach(this);
    onCreate()
  }

  override fun requestLayout() {
    super.requestLayout()
    Log.d(CLASS_NAME, "requestLayout");
    post(measureAndLayout)
  }

  private fun startRoute() {
    // register event listeners
    Log.d(CLASS_NAME, "startRoute origin:" + this.origin.toString());
    Log.d(CLASS_NAME, "startRoute destination:" + this.destination.toString());
    this.origin?.let { this.destination?.let { it1 -> this.findRoute(it, it1) } }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    MapboxNavigationApp.detach(this);
  }

  private fun onDestroy() {
    maneuverApi.cancel()
    routeLineApi.cancel()
    routeLineView.cancel()
    speechApi.cancel()
    voiceInstructionsPlayer.shutdown()
  }

  private fun initNavigation() {
    Log.d(CLASS_NAME, "initNavigation:");
    MapboxNavigationApp.setup {
      NavigationOptions.Builder(context)
        .accessToken(accessToken)
        .build()
    }

    MapboxNavigationApp.registerObserver(
      object : MapboxNavigationObserver {
        @SuppressLint("MissingPermission")
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
          mapboxNavigation.registerRoutesObserver(routesObserver)
          mapboxNavigation.registerArrivalObserver(arrivalObserver)
          mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
          mapboxNavigation.registerLocationObserver(locationObserver)
          mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
          mapboxNavigation.startTripSession();
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
          mapboxNavigation.unregisterRoutesObserver(routesObserver)
          mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
          mapboxNavigation.unregisterLocationObserver(locationObserver)
          mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        }
      }
    )
  }

  private fun findRoute(origin: Point?, destination: Point?) {

    // execute a route request
    // it's recommended to use the
    // applyDefaultNavigationOptions and applyLanguageAndVoiceUnitOptions
    // that make sure the route request is optimized
    // to allow for support of all of the Navigation SDK features
    MapboxNavigationApp.current()?.requestRoutes(
      RouteOptions.builder()
        .applyDefaultNavigationOptions()
        .language("en-IN")
        .voiceUnits(DirectionsCriteria.METRIC)
        .coordinatesList(listOf(origin, destination))
        // provide the bearing for the origin of the request to ensure
        // that the returned route faces in the direction of the current user movement
        .bearingsList(
          listOf(
            Bearing.builder()
              .angle(0.0)
              .degrees(45.0)
              .build(),
            null
          )
        )
        .layersList(listOf(MapboxNavigationApp.current()?.getZLevel(), null))
        .build(),
      object : NavigationRouterCallback {
        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
          // no impl
          Log.d(CLASS_NAME, "onCanceled routeOptions:");
        }

        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
          // no impl
          Log.d(CLASS_NAME, "onFailure reasons:" + reasons.toString());
        }

        override fun onRoutesReady(
          routes: List<NavigationRoute>,
          routerOrigin: RouterOrigin
        ) {
          Log.d(CLASS_NAME, "onRoutesReady routes:" + routes.toString());
          setRouteAndStartNavigation(routes)
        }
      }
    )
  }

  private fun setRouteAndStartNavigation(routes: List<NavigationRoute>) {
    Log.d(CLASS_NAME, "setRouteAndStartNavigation routes" + routes.toString())
    if (routes.isEmpty()) {
      sendErrorToReact("No route found")
      return;
    }
    // set routes, where the first route in the list is the primary route that
    // will be used for active guidance
    MapboxNavigationApp.current()?.setNavigationRoutes(routes)

    // show UI elements
    binding.soundButton.visibility = View.VISIBLE
    binding.routeOverview.visibility = View.VISIBLE
    binding.tripProgressCard.visibility = View.VISIBLE

    navigationCamera.requestNavigationCameraToFollowing();
  }

  private fun clearRouteAndStopNavigation() {
    // clear
    MapboxNavigationApp.current()?.setNavigationRoutes(listOf())

    // hide UI elements
    binding.soundButton.visibility = View.INVISIBLE
    binding.maneuverView.visibility = View.INVISIBLE
    binding.routeOverview.visibility = View.INVISIBLE
    binding.tripProgressCard.visibility = View.INVISIBLE
  }

  private fun sendErrorToReact(error: String?) {
    val event = Arguments.createMap()
    event.putString("error", error)
    context
      .getJSModule(RCTEventEmitter::class.java)
      .receiveEvent(id, "onError", event)
  }

  private fun applyProps() {
    if (this::viewportDataSource.isInitialized) {
      viewportDataSource.overviewPadding = this.padding
      viewportDataSource.followingPadding = this.padding
      viewportDataSource.evaluate()
    }
    this.startRoute()
  }

  fun onDropViewInstance() {
    this.onDestroy()
  }

  fun setOrigin(origin: Point?) {
    this.origin = origin
  }

  fun setDestination(destination: Point?) {
    this.destination = destination
    if (MapboxNavigationApp.isSetup() && this.binding != null){
      this.origin?.let {
        this.startRoute();
      }
    }
  }

  fun setShouldSimulateRoute(shouldSimulateRoute: Boolean) {
    this.shouldSimulateRoute = shouldSimulateRoute
  }

  fun setShowsEndOfRouteFeedback(showsEndOfRouteFeedback: Boolean) {
    this.showsEndOfRouteFeedback = showsEndOfRouteFeedback
  }

  fun setMute(mute: Boolean) {
    this.isVoiceInstructionsMuted = mute
  }

  fun setMapEdge(edge: ReadableMap) {
    if (edge != null) {
      val top =
        if (edge.getDouble("top") > 0) edge.getDouble("top") * pixelDensity else this.padding.top
      val left =
        if (edge.getDouble("left") > 0) edge.getDouble("left") * pixelDensity else this.padding.left
      val right =
        if (edge.getDouble("right") > 0) edge.getDouble("right") * pixelDensity; else this.padding.right
      val bottom =
        if (edge.getDouble("bottom") > 0) edge.getDouble("bottom") * pixelDensity else this.padding.bottom

      val edge = EdgeInsets(top, left, bottom, right);
      this.padding = edge;
      if (this::viewportDataSource.isInitialized) {
        viewportDataSource.overviewPadding = edge
        viewportDataSource.followingPadding = edge;
        viewportDataSource.evaluate()
      }
    }
  }

  fun setEdge(edge: ReadableMap) {
    if (edge != null) {
      val top =
        if (edge.getInt("top") > 0) edge.getInt("top") * pixelDensity else binding.maneuverView.marginTop
      val bottom =
        if (edge.getInt("bottom") > 0) edge.getInt("bottom") * pixelDensity else binding.tripProgressCard.marginBottom

      binding.tripProgressCard.updateMargins(null, null, null, bottom.toInt());
      binding.maneuverView.updateMargins(null, top.toInt(), null, null);
    }
  }

  override fun getLifecycle(): Lifecycle {
    return lifecycleRegistry;
  }

}
