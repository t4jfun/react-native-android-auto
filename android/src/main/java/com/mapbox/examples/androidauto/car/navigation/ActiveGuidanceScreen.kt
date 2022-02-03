package com.mapbox.examples.androidauto.car.navigation

import android.app.NotificationManager
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.car.app.model.*
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.notification.CarNotificationManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * After a route has been selected. This view gives turn-by-turn instructions
 * for completing the route.
 */
class ActiveGuidanceScreen(
    private val carActiveGuidanceContext: CarActiveGuidanceCarContext,
    private val directionsRoutes: List<DirectionsRoute>?,
    private val onBackPressedCallback: () -> Unit
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

    var overviewed = false
    val _started = MutableStateFlow(false)
    val started = _started.asStateFlow()

    private val carAudioGuidanceUi = CarAudioGuidanceUi(this)
    private val carRouteProgressObserver = CarNavigationInfoObserver(carActiveGuidanceContext)
    private val mapActionStripBuilder = MainMapActionStrip(this, carNavigationCamera)
    private val offRouteObserver = object : OffRouteObserver {
        override fun onOffRouteStateChanged(offRoute: Boolean) {
            logAndroidAuto("onOffRouteStateChanged $offRoute")
            if (offRoute){
                sendNotification(
                    carContext.getString(R.string.car_off_route_title),
                    carContext.getString(R.string.car_off_route_message)
                )
            }
        }
    }

    init {
        logAndroidAuto("ActiveGuidanceScreen constructor")
        carActiveGuidanceContext.mapboxNavigation.setRerouteController(null)
        // do something when the reroute state changes

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
            }

            override fun onPause(owner: LifecycleOwner) {
                logAndroidAuto("ActiveGuidanceScreen onPause")
                carActiveGuidanceContext.mapboxCarMap.unregisterObserver(roadLabelSurfaceLayer)
                carActiveGuidanceContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                carActiveGuidanceContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                carActiveGuidanceContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                carActiveGuidanceContext.mapboxCarMap.unregisterObserver(carRouteLine)
                carRouteProgressObserver.stop()
                carActiveGuidanceContext.mapboxNavigation.unregisterOffRouteObserver(offRouteObserver)

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
        logAndroidAuto("ActiveGuidanceScreen onGetTemplate")
        carActiveGuidanceContext.mapboxNavigation.setRoutes(directionsRoutes!!)
        if (!started.value && !overviewed) {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    carNavigationCamera.updateCameraMode(
                        CarCameraMode.OVERVIEW
                    )
                    overviewed = true
                } catch (e :Exception){
                    logAndroidAutoFailure("ActiveGuidanceScreen carNavigationCamera.updateCameraMode failed")
                }
            }, 1000)
        }
        val builder = NavigationTemplate.Builder()
            .setBackgroundColor(CarColor.PRIMARY)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
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
                    .addAction(
                        buildMainButtonAction()
                    )
                    .addAction(
                        carAudioGuidanceUi.buildSoundButtonAction()
                    )
                    .build()
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
                    carNavigationCamera.updateCameraMode(
                        CarCameraMode.FOLLOWING
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
                }
                .build()
        }
    }

    private fun stopNavigation() {
        logAndroidAuto("ActiveGuidanceScreen stopNavigation")
        MapboxNavigationProvider.retrieve().setRoutes(emptyList())
        MapboxCarApp.updateCarAppState(FreeDriveState)
    }
}
