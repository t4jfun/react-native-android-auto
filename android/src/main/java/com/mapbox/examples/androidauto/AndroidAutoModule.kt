package com.mapbox.examples.androidauto

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.ScreenManager
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.facebook.react.modules.debug.DevSettingsModule
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.examples.androidauto.car.MainCarContext
import com.mapbox.examples.androidauto.car.MainCarScreen
import com.mapbox.examples.androidauto.car.search.SearchCarContext
import com.mapbox.geojson.Point
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*
import java.util.WeakHashMap

import com.facebook.react.bridge.ReactApplicationContext
import com.mapbox.androidauto.*
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.examples.androidauto.car.navigation.ActiveGuidanceScreen
import com.mapbox.examples.androidauto.car.navigation.CarActiveGuidanceCarContext
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.turf.TurfMeasurement.distance


@ReactModule(name = AndroidAutoModule.MODULE_NAME)
class AndroidAutoModule(private val mReactContext: ReactApplicationContext) : ReactContextBaseJavaModule(mReactContext),
    DefaultLifecycleObserver {
    private val mHandler = Handler(Looper.getMainLooper())
    private var mCarContext: MainCarContext? = null
    private var mCurrentCarScreen: BaseCarScreen? = null
    private var mScreenManager: ScreenManager? = null
    private var screen: BaseCarScreen? = null
    private var mMainCarScreen: MainCarScreen? = null
    private var mActiveGuidanceScreen: ActiveGuidanceScreen? = null
    private var mReactScreen: BaseCarScreen? = null
    private var mStateConnected: Boolean = false
    private var currentRequestId: Long? = null

    private var carScreens: WeakHashMap<String, BaseCarScreen>
    private var reactCarRenderContextMap: WeakHashMap<BaseCarScreen, ReactCarRenderContext>


    private var onBackPressedCallback: () -> Unit = {
        Log.d("ReactAUTO", "Back: do nothing - set a proper action")
    }

    companion object {
        const val MODULE_NAME = "CarModule"
    }

    init {
        carScreens = WeakHashMap<String, BaseCarScreen>()
        reactCarRenderContextMap = WeakHashMap<BaseCarScreen, ReactCarRenderContext>()
        MapboxCarApp.updateCarAppState(ReactScreenState)
    }

    override fun getName(): String {
        return MODULE_NAME
    }

//    private val carAppStateObserver = Observer<CarAppState> { carAppState ->
//        val currentScreen = mCurrentCarScreen
//        val screenManager = mCarContext?.carContext?.getCarService(ScreenManager::class.java)
//        logAndroidAuto("--- MainScreenManager screen change ${currentScreen?.javaClass?.simpleName}")
//        if (screenManager?.top?.javaClass != currentScreen?.javaClass) {
//            screenManager?.push(currentScreen!!)
//        }
//    }

    fun getCurrentScreen(): BaseCarScreen {
        return mCurrentCarScreen ?: getScreenByState(ReactScreenState)
    }

    fun getScreenByState(carAppState: CarAppState, new: Boolean = false, root: Boolean = false): BaseCarScreen {
        logAndroidAuto("getScreenByState for state: $carAppState")
        //if(mCurrentScreen != null) return mCurrentScreen as MainCarScreen
        val screen = when (carAppState) {
            FreeDriveState, RoutePreviewState -> {
                if(new || mMainCarScreen == null){
                    mMainCarScreen = MainCarScreen(mCarContext!!)
                }
                mMainCarScreen!!

            }
//            ActiveGuidanceState, ArrivalState -> {
//                if(new || mActiveGuidanceScreen == null){
//                    mActiveGuidanceScreen = ActiveGuidanceScreen(
//                        CarActiveGuidanceCarContext(mCarContext!!),
//                        null,
//                        onBackPressedCallback,
//                        waypointPoints = waypointPoints
//                    )
//                }
//                mActiveGuidanceScreen!!
//
//            }
            ReactScreenState -> {
                if(new || mReactScreen == null){
                    mReactScreen = BaseCarScreen(mCarContext!!.carContext)
                }
                mReactScreen!!
            }
            else -> mMainCarScreen!!
        }
        //mCurrentCarScreen = screen
        if(root) screen.marker = "root"
        return screen
    }


    override fun onCreate(owner: LifecycleOwner) {
        logAndroidAuto("MainScreenManager onCreate")
        //MapboxCarApp.carAppState.observe(owner, carAppStateObserver)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        logAndroidAuto("MainScreenManager onDestroy")
        //MapboxCarApp.carAppState.removeObserver(carAppStateObserver)
    }

    @ReactMethod
    fun invalidate(name: String) {
        Log.i("ReactAUTO", "invalidate AA Module")
        screen = getScreen(name)
        mHandler.post {
            if (screen === mScreenManager!!.top) {
                screen?.invalidate()
            }
        }
    }

    @ReactMethod
    fun setTemplate(
        name: String,
        renderMap: ReadableMap,
        callback: Callback?
    ) {
        val reactCarRenderContext = ReactCarRenderContext()
        screen = getScreen(name)
        if (screen == null) {
            screen = mCurrentCarScreen
            Log.i("ReactAUTO", "Screen $name not found!")
        }
        reactCarRenderContext
            .setEventCallback(callback).screenMarker = screen!!.marker
        val template = parseTemplate(renderMap, reactCarRenderContext) {
            Log.d("ReactAUTO", "Set onBackPressed")
            onBackPressedCallback = it
        }
        reactCarRenderContextMap.remove(screen)
        reactCarRenderContextMap[screen] = reactCarRenderContext
        Log.i("ReactAUTO", "Screen setTemplate$template")
        screen?.setTemplate(template)
    }

    @InternalCoroutinesApi
    @ReactMethod
    fun pushScreen(
        name: String?,
        renderMap: ReadableMap,
        callback: Callback?
    ) {
        mHandler.post {
            val reactCarRenderContext = ReactCarRenderContext()
            reactCarRenderContext.setEventCallback(callback).screenMarker = name

            // TODO: parse template only if template provided
            val type = renderMap.getString("type")
            // if type endswith template :
            if (type!!.endsWith("template")) {
                val template = parseTemplate(renderMap, reactCarRenderContext) {
                    Log.d("ReactAUTO", "Set onBackPressed")
                    onBackPressedCallback = it
                }
                screen = getScreenByState(ReactScreenState, true)
                screen?.setTemplate(template)
                reactCarRenderContextMap.remove(screen)
                reactCarRenderContextMap[screen] = reactCarRenderContext

                // else raise error
                screen?.marker = name
                Log.d("ReactAUTO", reactCarRenderContextMap.toString())
                Log.d("ReactAUTO", carScreens.toString())
                carScreens[name] = screen
                Log.d("ReactAUTO", "name: $name")
                mCurrentCarScreen = screen
                Log.i("ReactAUTO", "pushScreen $screen")
                Log.d("ReactAUTO", carScreens.toString())
                mScreenManager!!.push(screen!!)

            } else {
                // else if endswith screen:
                var waypointPoints = mutableListOf<Point>()
                Log.d("ReactAUTO", "----------------------")
                val waypoints = renderMap.getArray("children")?.getMap(0)?.getMap("metadata")?.getArray("waypoints")
                for (i in 0 until waypoints?.size()!!) {
                    val waypoint = waypoints?.getMap(i)
                    val data = waypoint?.getMap("data")
                    val geometry = data?.getMap("geometry")
                    val coordinates = geometry?.getArray("coordinates")
                    // Geometry: { NativeMap: {"type":"Point","coordinates":[-123.775287,39.577113]} }
                    waypointPoints.add(
                        Point.fromLngLat(
                            coordinates!!.getDouble(0),
                            coordinates.getDouble(1)
                        )
                    )
                }

                Log.d("ReactAUTO", "Points: $waypointPoints")
                Log.d("ReactAUTO", "----------------------")
                val searchCarContext = SearchCarContext(mCarContext!!)
                // TODO: check distance to start point
                val location = MapboxCarApp.carAppServices.location().navigationLocationProvider.lastLocation
                val useFakePoints = false
                if(useFakePoints){
                    waypointPoints = listOf(
                        Point.fromLngLat(location!!.longitude, location.latitude),
                        Point.fromLngLat(18.970608, 47.522207),
                        Point.fromLngLat(18.972462, 47.520275),
                        Point.fromLngLat(18.974289, 47.518817),
                        Point.fromLngLat(18.970734, 47.517787)
                    ) as MutableList<Point>
                }
                val maxDistanceAllowedInKm = 0.402336
                if(
                    location == null ||
                    ( waypointPoints.size > 1 &&
                    distance(waypointPoints.get(0), Point.fromLngLat(location.longitude, location.latitude)) > maxDistanceAllowedInKm))
                {
                    screen = ErrorScreen(mCarContext!!.carContext)

                    reactCarRenderContextMap.remove(screen)
                    reactCarRenderContextMap[screen] = reactCarRenderContext
                    screen?.marker = name
                    carScreens[name] = screen
                    Log.d("ReactAUTO", carScreens.toString())
                    mCurrentCarScreen = screen
                    Log.i("ReactAUTO", "pushScreen $screen")
                    mScreenManager!!.push(screen!!)
                } else {
                    // Navigation view
                    Log.d("ReactAUTO", "routePrevMap after")

                    currentRequestId?.let { searchCarContext.carRouteRequest.mapboxNavigation.cancelRouteRequest(it) }
                    currentRequestId = searchCarContext.carRouteRequest.mapboxNavigation.requestRoutes(
                        RouteOptions.builder()
                            .applyDefaultNavigationOptions()
                            .profile(DirectionsCriteria.PROFILE_DRIVING)
                            .coordinatesList(waypointPoints)
                            .waypointIndicesList(listOf(0, waypointPoints.size-1))
                            .build(),
                        routesRequestCallback = object : RouterCallback {

                            override fun onRoutesReady(
                                routes: List<DirectionsRoute>,
                                routerOrigin: RouterOrigin
                            ) {
                                Log.d("ReactAUTO", "onRoutesReady --- : $routes");

                                val carActiveGuidanceCarContext = CarActiveGuidanceCarContext(mCarContext!!)
                                val showNotification = try {
                                    renderMap.getBoolean("notification")
                                } catch (e: NoSuchKeyException) {
                                    false
                                }

                                screen = ActiveGuidanceScreen(
                                    carActiveGuidanceCarContext,
                                    routes,
                                    onBackPressedCallback,
                                    showNotification,
                                    waypointPoints
                                )


                                reactCarRenderContextMap.remove(screen)
                                reactCarRenderContextMap[screen] = reactCarRenderContext
                                screen?.marker = name
                                carScreens[name] = screen
                                Log.d("ReactAUTO", carScreens.toString())
                                mCurrentCarScreen = screen
                                Log.i("ReactAUTO", "pushScreen $screen")
                                mScreenManager!!.push(screen!!)
                                carActiveGuidanceCarContext.mapboxNavigation.setRoutes(routes)
                            }

                            override fun onCanceled(
                                routeOptions: RouteOptions,
                                routerOrigin: RouterOrigin
                            ) {
                                Log.d("ReactAUTO", "RouteRequest onCanceled");

                            }

                            override fun onFailure(
                                reasons: List<RouterFailure>,
                                routeOptions: RouteOptions
                            ) {
                                Log.d("ReactAUTO", "RouteRequest onFailure");
                            }

                        }
                    )


                }

            }
        }
    }

    @ReactMethod
    fun popScreen() {
        Log.d("ReactAUTO", "popScreen");
        mHandler.post {
            mScreenManager!!.pop()
            removeScreen(mCurrentCarScreen)
            mCurrentCarScreen = mScreenManager!!.top as BaseCarScreen
        }
    }

    @ReactMethod
    fun mapNavigate(address: String) {
        mCarContext!!.carContext.startCarApp(
            Intent(CarContext.ACTION_NAVIGATE, Uri.parse("geo:0,0?q=$address"))
        )
    }

    @ReactMethod
    fun toast(text: String?, duration: Int) {
        CarToast.makeText(mCarContext!!.carContext, text!!, duration).show()
    }

    @ReactMethod
    fun reload() {
        val devSettingsModule = mReactContext.getNativeModule(
            DevSettingsModule::class.java
        )
        devSettingsModule?.reload()
    }

    @ReactMethod
    fun finishCarApp() {
        mCarContext!!.carContext.finishCarApp()
    }

    @ReactMethod
    fun setEventCallback(name: String, callback: Callback?) {
        screen = getScreen(name)
        Log.d("ReactAUTO", "Set callback 1 for $name")
        if (screen == null) {
            return
        }
        Log.d("ReactAUTO", "Set callback 2 for $name")
        val reactCarRenderContext = reactCarRenderContextMap[screen] ?: return
        Log.d("ReactAUTO", "Set callback 3 for $name")
        reactCarRenderContext.eventCallback = callback
    }

    @ReactMethod
    fun getState(){
        if(mStateConnected){
            sendEvent("android_auto:connect")
        } else {
            sendEvent("android_auto:disconnect")
        }
    }

    fun setCarContext(carContext: MainCarContext, startScreen: BaseCarScreen) {
        mCarContext = carContext
        mCurrentCarScreen = startScreen
        mScreenManager = mCurrentCarScreen?.screenManager
        carScreens["root"] = mCurrentCarScreen
        Log.d("ReactAUTO", carScreens.toString())
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("ReactAUTO", "Back button pressed")
                onBackPressedCallback()
                sendEvent("android_auto:back_button", WritableNativeMap())
            }
        }
        carContext.carContext.onBackPressedDispatcher.addCallback(callback)
        sendEvent("android_auto:ready", WritableNativeMap())
    }

    private fun parseTemplate(
        renderMap: ReadableMap,
        reactCarRenderContext: ReactCarRenderContext,
        setOnBackPressedCallback: (() -> Unit) -> Unit = {}
    ): Template {
        val templateParser = TemplateParser(mCarContext!!.carContext, reactCarRenderContext, setOnBackPressedCallback)
        return templateParser.parseTemplate(renderMap)
    }

    private fun getScreen(name: String): BaseCarScreen? {
        return carScreens[name]
    }

    private fun removeScreen(screen: BaseCarScreen?) {
        Log.d("ReactAUTO", "RemoveScreen2 $screen")

        val params = WritableNativeMap()
        params.putString("screen", screen!!.marker)
        sendEvent("android_auto:remove_screen", params)
        carScreens.values.remove(screen)
    }

    fun sendEvent(eventName: String, params: Any = WritableNativeMap()) {
        mReactContext
            .getJSModule(RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    fun connect(){
        mStateConnected = true
        getState()
    }

    fun disconnect(){
        mStateConnected = false
        getState()
    }


}