package com.mapbox.examples.androidauto.car.navigation

import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError

/**
 * Observe MapboxNavigation properties that create NavigationInfo.
 *
 * Attach the [start] [stop] functions to start observing navigation info.
 */
class CarNavigationInfoObserver(
    private val carActiveGuidanceCarContext: CarActiveGuidanceCarContext,
    private val waypoints: MutableList<androidx.car.app.navigation.model.Maneuver>
) {
    private var onNavigationInfoChanged: (() -> Unit)? = null
    var navigationInfo: NavigationTemplate.NavigationInfo? = null
        private set(value) {
            if (field != value) {
                logAndroidAuto("CarNavigationInfoObserver navigationInfo changed")
                field = value
                onNavigationInfoChanged?.invoke()
            }
        }

    var travelEstimateInfo: TravelEstimate? = null
    private var expectedManeuvers: Expected<ManeuverError, List<Maneuver>>? = null
    private var routeProgress: RouteProgress? = null

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        this.routeProgress = routeProgress
        expectedManeuvers = carActiveGuidanceCarContext.maneuverApi.getManeuvers(routeProgress)
        updateNavigationInfo()
    }

    private fun updateNavigationInfo() {
        logAndroidAuto("Expected maneuvers: $expectedManeuvers")
        //expectedManeuvers?.value?.get()
        // TODO: find nearest in waypoints with a limit distance, if the target is too far, use the default maneuver with override
        expectedManeuvers?.value?.forEach {

        }
        this.navigationInfo = carActiveGuidanceCarContext.navigationInfoMapper
            .mapNavigationInfo(expectedManeuvers, routeProgress, listOf<String>())

        this.travelEstimateInfo = carActiveGuidanceCarContext.tripProgressMapper.from(routeProgress)
    }

    fun start(onNavigationInfoChanged: () -> Unit) {
        this.onNavigationInfoChanged = onNavigationInfoChanged
        logAndroidAuto("CarRouteProgressObserver onStart")
        val mapboxNavigation = carActiveGuidanceCarContext.mapboxNavigation
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    fun stop() {
        logAndroidAuto("CarRouteProgressObserver onStop")
        onNavigationInfoChanged = null
        val mapboxNavigation = carActiveGuidanceCarContext.mapboxNavigation
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        carActiveGuidanceCarContext.maneuverApi.cancel()
    }
}
