package com.mapbox.examples.androidauto.car.location

import android.graphics.Color
import androidx.core.content.ContextCompat
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.car.map.MapboxCarMapSurface
import com.mapbox.androidauto.car.map.MapboxCarMapObserver
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.examples.androidauto.R
import com.mapbox.examples.androidauto.car.MainCarContext
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.literal
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location

/**
 * Create a simple 3d location puck. This class is demonstrating how to
 * create a renderer. To Create a new location experience, try creating a new class.
 */
class CarLocationRenderer(
    private val mainCarContext: MainCarContext
) : MapboxCarMapObserver {

    override fun loaded(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarLocationRenderer carMapSurface loaded")
        val _locationPuck = LocationPuck2D(
            bearingImage = ContextCompat.getDrawable(mainCarContext.carContext, R.drawable.mapbox_navigation_puck_icon),
            scaleExpression = literal(1.0).toJson()
        )
        mapboxCarMapSurface.mapSurface.location.apply {
            locationPuck = _locationPuck
            enabled = true
            pulsingEnabled = true
            setLocationProvider(MapboxCarApp.carAppServices.location().navigationLocationProvider)
        }
    }
}
