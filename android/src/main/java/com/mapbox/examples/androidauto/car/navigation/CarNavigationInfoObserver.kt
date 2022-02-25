package com.mapbox.examples.androidauto.car.navigation

import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement


private fun Point.distanceTo(other: Point): Double{
    return TurfMeasurement.distance(
        this,
        other,
        TurfConstants.UNIT_METRES
    )
}
/**
 * Observe MapboxNavigation properties that create NavigationInfo.
 *
 * Attach the [start] [stop] functions to start observing navigation info.
 */
class CarNavigationInfoObserver(
    private val carActiveGuidanceCarContext: CarActiveGuidanceCarContext,
    private val maneuvers: MutableMap<Point, String>
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
    private var processedExpectedManeuvers: Expected<ManeuverError, List<Maneuver>>? = null
    private val maneuverOverrideMap : MutableMap<Maneuver, String> = mutableMapOf<Maneuver, String>()

    private var routeProgress: RouteProgress? = null

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        this.routeProgress = routeProgress
        expectedManeuvers = carActiveGuidanceCarContext.maneuverApi.getManeuvers(routeProgress)
        updateNavigationInfo()
    }

    private fun updateNavigationInfo() {
        //logAndroidAuto("Expected maneuvers: ${expectedManeuvers?.value}")
        val overrides = mutableListOf<String?>()
//        if(expectedManeuvers != processedExpectedManeuvers){
//            expectedManeuvers?.value?.forEach {
//                if(!maneuverOverrideMap.containsKey(it)){
//                    var distance = 50.0
//                    maneuvers.forEach { maneuver ->
//                        if (it.maneuverPoint.distanceTo(maneuver.key) < distance){
//                            maneuverOverrideMap[it] = maneuver.value
//                            distance = it.maneuverPoint.distanceTo(maneuver.key)
//                            logAndroidAuto("Override maneuver: $it to ${maneuver.value}")
//                        }
//                    }
//
//                }
//
//                overrides.add(maneuverOverrideMap[it])
//
//            }
//            processedExpectedManeuvers = expectedManeuvers
//        }

        this.navigationInfo = carActiveGuidanceCarContext.navigationInfoMapper
            .mapNavigationInfo(expectedManeuvers, routeProgress, overrides)

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
