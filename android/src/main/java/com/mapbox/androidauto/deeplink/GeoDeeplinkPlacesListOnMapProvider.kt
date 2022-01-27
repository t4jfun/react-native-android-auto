package com.mapbox.androidauto.deeplink

import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.examples.androidauto.car.placeslistonmap.PlacesListOnMapProvider
import com.mapbox.examples.androidauto.car.search.GetPlacesError
import com.mapbox.examples.androidauto.car.search.PlaceRecord
import com.mapbox.examples.androidauto.car.search.PlaceRecordMapper
import com.mapbox.geojson.Point

class GeoDeeplinkPlacesListOnMapProvider(
    private val geoDeeplinkGeocoding: GeoDeeplinkGeocoding,
    private val geoDeeplink: GeoDeeplink
) : PlacesListOnMapProvider {

    @Suppress("ReturnCount")
    override suspend fun getPlaces(): Expected<GetPlacesError, List<PlaceRecord>> {
        // Wait for an origin location
        val origin = MapboxCarApp.carAppServices.location().validLocation()
            ?.run { Point.fromLngLat(longitude, latitude) }
            ?: return ExpectedFactory.createError(
                GetPlacesError("Did not find current location.", null)
            )

        // Request places from the origin to the deeplink place
        val result = geoDeeplinkGeocoding.requestPlaces(geoDeeplink, origin)
            ?: return ExpectedFactory.createError(
                GetPlacesError("Error getting geo deeplink places.", null)
            )
        return ExpectedFactory.createValue(
            result.features().map(PlaceRecordMapper::fromCarmenFeature)
        )
    }

    override fun cancel() {
        geoDeeplinkGeocoding.cancel()
    }
}
