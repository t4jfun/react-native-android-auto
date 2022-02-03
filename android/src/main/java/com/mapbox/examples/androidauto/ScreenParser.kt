package com.mapbox.examples.androidauto

import android.util.Log
import com.facebook.react.bridge.ReadableMap
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.examples.androidauto.car.MainCarContext
import com.mapbox.examples.androidauto.car.preview.CarRoutePreviewScreen
import com.mapbox.examples.androidauto.car.preview.CarRouteRequestCallback
import com.mapbox.examples.androidauto.car.preview.RoutePreviewCarContext
import com.mapbox.examples.androidauto.car.search.PlaceRecord
import com.mapbox.examples.androidauto.car.search.SearchCarContext
import com.mapbox.geojson.Point
import com.mapbox.search.ResponseInfo
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchSuggestionsCallback
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.*


class ScreenParser {

//    fun parseType(mCarContext: MainCarContext, map: ReadableMap, type: String): Screen {
//
//    }


    @DelicateCoroutinesApi
    @InternalCoroutinesApi
    fun getRoutePreviewScreen(
        mCarContext: MainCarContext,
        map: ReadableMap,
        type: String
    ): CarRoutePreviewScreen {
        Log.d("ReactAUTO", "routePrevMap: $map")

        // Route preview
        val searchCarContext = SearchCarContext(mCarContext)
        val point = Point.fromLngLat(19.021870, 47.493226)
        val placeRecord = PlaceRecord(
            "some_id",
            "some_name",
            point,
            "some_description"
        )
        Log.d("ReactAUTO", "routePrevMap after")

//
//        val searchResult: List<SearchResult>? = runBlocking {
//            suspendCancellableCoroutine {
//                searchCarContext.reverseSearchEngine.search(
//                    ReverseGeoOptions(
//                        center = point
//                    ),
//                    object : SearchCallback {
//                        override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
//                            Log.d("ReactAUTO", "Search trySend: $results");
//                            it.tryResume(results)
//                        }
//
//                        override fun onError(e: Exception) {
//                            Log.d("ReactAUTO", "Search onError");
//                            it.tryResume(null)
//                        }
//                    }
//                )
//            }
//        }
//
//        Log.d("ReactAUTO", "searchResult: $searchResult")

//        val directions: List<DirectionsRoute>? = runBlocking {
//            suspendCancellableCoroutine {
//                searchCarContext.carRouteRequest.request(
//                    placeRecord,
//                    object : CarRouteRequestCallback {
//                        override fun onRoutesReady(
//                            placeRecord: PlaceRecord,
//                            routes: List<DirectionsRoute>
//                        ) {
//                            Log.d("ReactAUTO", "trySend: $routes");
//                            it.tryResume(routes)
//                        }
//
//                        override fun onUnknownCurrentLocation() {
//                            Log.d("ReactAUTO", "onUnknownCurrentLocation");
//                            it.tryResume(null)
//                        }
//
//                        override fun onDestinationLocationUnknown() {
//                            Log.d("ReactAUTO", "onDestinationLocationUnknown");
//                            it.tryResume(null)
//                        }
//
//                        override fun onNoRoutesFound() {
//                            Log.d("ReactAUTO", "onNoRoutesFound");
//                            it.tryResume(null)
//                        }
//                    }
//                )
//            }
//        }

        val directions: List<DirectionsRoute>? = runBlocking {
            val job = GlobalScope.async {
                return@async searchCarContext.carRouteRequest.requestSync(placeRecord)
            }
            return@runBlocking job.await()
        }

        Log.d("ReactAUTO", "directions: $directions")

        return CarRoutePreviewScreen(
            RoutePreviewCarContext(mCarContext),
            placeRecord,
            directions!!
        )
    }

}