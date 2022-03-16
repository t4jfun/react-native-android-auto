package com.mapbox.examples.androidauto.car.navigation

import android.app.NotificationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.car.app.model.*
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.notification.CarNotificationManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.*
import com.mapbox.androidauto.FreeDriveState
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.navigation.audioguidance.CarAudioGuidanceUi
import com.mapbox.androidauto.car.navigation.roadlabel.RoadLabelSurfaceLayer
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.examples.androidauto.R
import com.mapbox.examples.androidauto.car.location.CarLocationRenderer
import com.mapbox.androidauto.car.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.androidauto.logAndroidAutoFailure
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.examples.androidauto.BaseCarScreen
import com.mapbox.examples.androidauto.car.MainMapActionStrip
import com.mapbox.examples.androidauto.car.preview.CarRouteLine
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.trip.session.LegIndexUpdatedCallback
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

/**
 * After a route has been selected. This view gives turn-by-turn instructions
 * for completing the route.
 */
class ActiveGuidanceScreen(
    private val carActiveGuidanceContext: CarActiveGuidanceCarContext,
    private val directionsRoutes: List<DirectionsRoute>?,
    private val onBackPressedCallback: () -> Unit,
    private val showNotificationDebugButton: Boolean? = false,
    private val alertTooFar: Boolean = false,
    private val waypointPoints: MutableList<Point>,
    private val maneuvers: MutableMap<Point, String> = mutableMapOf(),
    private val offroad: Boolean = false
) : BaseCarScreen(carActiveGuidanceContext.carContext) {

    val carRouteLine = CarRouteLine(carActiveGuidanceContext.mainCarContext)
    val carLocationRenderer = CarLocationRenderer(carActiveGuidanceContext.mainCarContext)
    val carSpeedLimitRenderer = CarSpeedLimitRenderer(carContext)
    val carNavigationCamera = CarNavigationCamera(
        carActiveGuidanceContext.mapboxNavigation,
        CarCameraMode.OVERVIEW //CarCameraMode.FOLLOWING
    )
    private val roadLabelSurfaceLayer = RoadLabelSurfaceLayer(
        carActiveGuidanceContext.carContext,
        carActiveGuidanceContext.mapboxNavigation
    )


    private val NOTIFICATION_CHANNEL_ID = "channel_00"
    private val NOTIFICATION_CHANNEL_NAME: CharSequence = "Default Channel"
    private val NOTIFICATION_ID = 1001

    private val NOTIFICATION_CHANNEL_HIGH_ID = "channel_01"
    private val NOTIFICATION_CHANNEL_HIGH_NAME: CharSequence = "High Channel"

    private val NOTIFICATION_CHANNEL_LOW_ID = "channel_02"
    private val NOTIFICATION_CHANNEL_LOW_NAME: CharSequence = "Low Channel"

    private val MSG_SEND_NOTIFICATION = 1

    private val mImportance = NotificationManager.IMPORTANCE_HIGH
    private val mSetOngoing = false
    private var mNotificationCount = 0

    private val OFF_ROUTE_TIMEOUT = 30_000L  // 30s
    private var mOffRouteState : Boolean = false
    private var mOffRouteTime : Long? = null
    private var mOffRouteTimer : TimerTask? = null
    private var mTooFarNotificationShowed : Boolean = false
    private var mBackupCameraMode : CarCameraMode = CarCameraMode.OVERVIEW
    private var mPaused : Boolean = false


    var overviewed = false
    val _started = MutableStateFlow(false)
    val started = _started.asStateFlow()
    val _renderMode = MutableStateFlow(CarRenderMode.RENDER_3D)
    val renderMode = _renderMode.asStateFlow()

    private val carAudioGuidanceUi = CarAudioGuidanceUi(this)
    private val carRouteProgressObserver = CarNavigationInfoObserver(carActiveGuidanceContext, maneuvers, offroad)
    private val mapActionStripBuilder = MainMapActionStrip(this, carNavigationCamera)
    private val offRouteObserver = object : OffRouteObserver {
        override fun onOffRouteStateChanged(offRoute: Boolean) {
            logAndroidAuto("onOffRouteStateChanged $offRoute")
            if (offRoute){
                mOffRouteState = true
                mOffRouteTime = SystemClock.elapsedRealtimeNanos()
                mOffRouteTimer?.cancel()
                mOffRouteTimer = Timer("OffRoute", false).schedule(OFF_ROUTE_TIMEOUT) {
                    if(SystemClock.elapsedRealtimeNanos() - (mOffRouteTime ?: 0L) >= TimeUnit.MILLISECONDS.toNanos(OFF_ROUTE_TIMEOUT)){
                        sendNotification(
                            carContext.getString(R.string.car_off_route_title),
                            carContext.getString(R.string.car_off_route_message)
                        )
                    }
                }
            } else {
                mOffRouteState = false
                mOffRouteTimer?.cancel()
                val carNotificationManager = CarNotificationManager.from(carContext)
                carNotificationManager.cancel(NOTIFICATION_ID)
            }
        }
    }

    private val legIndexUpdatedCallback = object : LegIndexUpdatedCallback {
        override fun onLegIndexUpdatedCallback(updated: Boolean) {
            logAndroidAuto("onLegIndexUpdatedCallback $updated")
        }

    }

    private val arrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            logAndroidAuto("--- onFinalDestinationArrival ---")
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            logAndroidAuto("--- onNextRouteLegStart ---")
            carActiveGuidanceContext.mapboxNavigation.navigateNextRouteLeg(legIndexUpdatedCallback)
        }

        override fun onWaypointArrival(routeProgress: RouteProgress) {
            logAndroidAuto("onWaypointArrival")
        }
    }

    init {
        logAndroidAuto("ActiveGuidanceScreen constructor")
        carActiveGuidanceContext.mapboxNavigation.setRerouteController(null)
        val arrivalController = CustomArrivalController()
        carActiveGuidanceContext.mapboxNavigation.setArrivalController(arrivalController)

        // do something when the reroute state changes
        lifecycle.coroutineScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                MapboxCarApp.carAppServices.audioGuidance().stateFlow()
//                    .distinctUntilChanged { old, new ->
//                        old.isMuted != new.isMuted || old.isPlayable != new.isPlayable
//                    }
                    .collect { this@ActiveGuidanceScreen.invalidate() }
            }
        }

        lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onResume(owner: LifecycleOwner) {
                logAndroidAuto("ActiveGuidanceScreen onResume")
                carActiveGuidanceContext.mapboxCarMap.registerObserver(carLocationRenderer)
                carActiveGuidanceContext.mapboxCarMap.registerObserver(roadLabelSurfaceLayer)
                carActiveGuidanceContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                carActiveGuidanceContext.mapboxCarMap.registerObserver(carNavigationCamera)
                carActiveGuidanceContext.mapboxCarMap.registerObserver(carRouteLine)
                carActiveGuidanceContext.mapboxNavigation.registerOffRouteObserver(offRouteObserver)
                if(started.value){
                    carRouteProgressObserver.start {
                        invalidate()
                    }
                }
                carActiveGuidanceContext.mapboxNavigation.registerArrivalObserver(arrivalObserver)

                Handler(Looper.getMainLooper()).postDelayed({
                    val mapView = carActiveGuidanceContext.mapboxCarMap.mapboxCarMapSurface?.mapSurface
                    val annotationApi = mapView?.annotations
                    val annotationConfig = AnnotationConfig(
                        belowLayerId = LocationComponentConstants.LOCATION_INDICATOR_LAYER
                    )
                    val circleAnnotationManager = annotationApi?.createCircleAnnotationManager(annotationConfig)
                    Log.d("ReactAUTO", "circleAnnotationManager $circleAnnotationManager")
                    var index = 0
                    waypointPoints.forEach {
                        if(index > 0 && index != waypointPoints.size -1){
                            val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                                // Define a geographic coordinate.
                                .withPoint(it)
                                // Style the circle that will be added to the map.
                                .withCircleRadius(10.0)
                                .withCircleColor("#ffffff")
                                .withCircleStrokeWidth(2.0)
                                .withCircleStrokeColor("#b61827")
                            circleAnnotationManager?.create(circleAnnotationOptions)
                        } else if(index == waypointPoints.size -1){
                            val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                                // Define a geographic coordinate.
                                .withPoint(it)
                                // Style the circle that will be added to the map.
                                .withCircleRadius(10.0)
                                .withCircleColor("#b61827")
                                .withCircleStrokeWidth(2.0)
                                .withCircleStrokeColor("#ffffff")
                            circleAnnotationManager?.create(circleAnnotationOptions)
                        }
                        index += 1
                    }

                    if(alertTooFar && !mTooFarNotificationShowed){
                        sendNotification(
                            carContext.getString(R.string.car_error_title),
                            carContext.getString(R.string.car_error_message)
                        )
                        mTooFarNotificationShowed = true
                    }

                }, 1000)

            }

            override fun onPause(owner: LifecycleOwner) {
                logAndroidAuto("ActiveGuidanceScreen onPause")
                mPaused = true
                carActiveGuidanceContext.mapboxCarMap.unregisterObserver(roadLabelSurfaceLayer)
                carActiveGuidanceContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                carActiveGuidanceContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                carActiveGuidanceContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                carActiveGuidanceContext.mapboxCarMap.unregisterObserver(carRouteLine)
                carRouteProgressObserver.stop()
                carActiveGuidanceContext.mapboxNavigation.unregisterOffRouteObserver(offRouteObserver)
                carActiveGuidanceContext.mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
            }
        })
    }

    private fun sendNotification(title: CharSequence, text: CharSequence) {
        when (mImportance) {
            NotificationManager.IMPORTANCE_HIGH -> sendNotification(
                title, text, NOTIFICATION_CHANNEL_HIGH_ID,
                NOTIFICATION_CHANNEL_HIGH_NAME, NOTIFICATION_ID,
                NotificationManager.IMPORTANCE_HIGH
            )
            NotificationManager.IMPORTANCE_DEFAULT -> sendNotification(
                title, text, NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME,
                NOTIFICATION_ID, NotificationManager.IMPORTANCE_DEFAULT
            )
            NotificationManager.IMPORTANCE_LOW -> sendNotification(
                title, text, NOTIFICATION_CHANNEL_LOW_ID,
                NOTIFICATION_CHANNEL_LOW_NAME, NOTIFICATION_ID,
                NotificationManager.IMPORTANCE_LOW
            )
            else -> {}
        }
    }

    private fun sendNotification(
        title: CharSequence, text: CharSequence, channelId: String,
        channelName: CharSequence, notificationId: Int, importance: Int
    ) {
        val carNotificationManager = CarNotificationManager.from(carContext)
        val channel = NotificationChannelCompat.Builder(
            channelId,
            importance
        ).setName(channelName).build()
        carNotificationManager.createNotificationChannel(channel)
        val builder = NotificationCompat.Builder(carContext, channelId)
        builder.setOngoing(mSetOngoing)
        builder.setSmallIcon(R.drawable.warning_sign)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .clearActions()
//            .setColor(mCarContext!!.carContext.getColor(androidx.car.app.R.color.carColorGreen))
//            .setColorized(true)
        carNotificationManager.notify(notificationId, builder)
    }

    override fun onGetTemplate(): Template {
        //logAndroidAuto("ActiveGuidanceScreen onGetTemplate")
        carActiveGuidanceContext.mapboxNavigation.setRoutes(directionsRoutes!!)
        if (!started.value && !overviewed) {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    mBackupCameraMode = CarCameraMode.OVERVIEW
                    carNavigationCamera.updateCameraMode(
                        mBackupCameraMode
                    )
                    overviewed = true
                } catch (e :Exception){
                    logAndroidAutoFailure("ActiveGuidanceScreen carNavigationCamera.updateCameraMode failed")
                }
            }, 1000)
        }

        if(mPaused){
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    carNavigationCamera.updateCameraMode(
                        CarCameraMode.OVERVIEW
                    )
                    carNavigationCamera.updateCameraMode(
                        mBackupCameraMode
                    )
                    mPaused = false
                } catch (e :Exception){
                    logAndroidAutoFailure("ActiveGuidanceScreen carNavigationCamera.updateCameraMode failed")
                }
            }, 1000)
        }
        val actionStrip = ActionStrip.Builder()
        if(showNotificationDebugButton == true) actionStrip.addAction(
            // Example button for notification test
            Action.Builder()
                .setIcon(CarIcon.ALERT)
                .setOnClickListener {
                    sendNotification(
                        carContext.getString(R.string.car_off_route_title),
                        carContext.getString(R.string.car_off_route_message)
                    )
                }
                .build()
        )
        actionStrip.addAction(
            // Example button for notification test
            Action.Builder()
                .setIcon(CarIcon.BACK)
                .setOnClickListener {
                    stopNavigation()
                    _started.value = false
                    onBackPressedCallback()
                    val carNotificationManager = CarNotificationManager.from(carContext)
                    carNotificationManager.cancel(NOTIFICATION_ID)
                }
                .build()
        )
        .addAction(
            buildMainButtonAction()
        )
        .addAction(
            carAudioGuidanceUi.buildSoundButtonAction()
        )
        .addAction(
            buildRenderModeButtonAction()
        )
        // mapActionStripBuilder.updateCustomAction(buildRenderModeButtonAction())
        val builder = NavigationTemplate.Builder()
            .setBackgroundColor(CarColor.PRIMARY)
            .setActionStrip(
                actionStrip.build()
            )
            .setMapActionStrip(mapActionStripBuilder.build())

        carRouteProgressObserver.navigationInfo?.let {
            builder.setNavigationInfo(it)
        }

        carRouteProgressObserver.travelEstimateInfo?.let {
            builder.setDestinationTravelEstimate(it)
        }

        return builder.build()
    }

    private fun buildMainButtonAction(): Action {
        return if (!started.value) {
            Action.Builder()
                .setTitle(carContext.getString(R.string.car_action_navigation_go_button))
                .setOnClickListener {
                    mBackupCameraMode = CarCameraMode.FOLLOWING
                    carNavigationCamera.updateCameraMode(
                        mBackupCameraMode
                    )
                    carRouteProgressObserver.start {
                        invalidate()
                    }
                    _started.value = true
                }
                .build()
        } else {
            Action.Builder()
                .setTitle(carContext.getString(R.string.car_action_navigation_stop_button))
                .setOnClickListener {
                    stopNavigation()
                    _started.value = false
                    onBackPressedCallback()
                    val carNotificationManager = CarNotificationManager.from(carContext)
                    carNotificationManager.cancel(NOTIFICATION_ID)
                }
                .build()
        }
    }

    private fun buildRenderModeButtonAction(): Action {
        return when (renderMode.value){
            CarRenderMode.RENDER_2D -> {
                buildIconAction(R.drawable.ic_2d_24) {
                    // OnClick
                    _renderMode.value = CarRenderMode.RENDER_3D
                    carNavigationCamera.updateRenderMode(_renderMode.value)
                    invalidate()
                }
            }
            CarRenderMode.RENDER_3D -> {
                buildIconAction(R.drawable.ic_3d_24) {
                    // OnClick
                    _renderMode.value = CarRenderMode.RENDER_2D
                    carNavigationCamera.updateRenderMode(_renderMode.value)
                    invalidate()
                }
            }
        }
    }

    private fun buildIconAction(@DrawableRes icon: Int, onClick: () -> Unit) = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(carContext, icon)
            ).build()
        )
        .setOnClickListener { onClick() }
        .build()

    private fun stopNavigation() {
        logAndroidAuto("ActiveGuidanceScreen stopNavigation")
        MapboxNavigationProvider.retrieve().setRoutes(emptyList())
        MapboxCarApp.updateCarAppState(FreeDriveState)
    }
}
