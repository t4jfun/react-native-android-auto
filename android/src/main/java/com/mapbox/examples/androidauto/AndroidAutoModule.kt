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
import com.mapbox.examples.androidauto.car.preview.CarRoutePreviewScreen
import com.mapbox.examples.androidauto.car.preview.CarRouteRequestCallback
import com.mapbox.examples.androidauto.car.preview.RoutePreviewCarContext
import com.mapbox.examples.androidauto.car.search.PlaceRecord
import com.mapbox.examples.androidauto.car.search.SearchCarContext
import com.mapbox.geojson.Point
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*
import java.util.WeakHashMap

import com.facebook.react.bridge.ReactApplicationContext
import com.mapbox.androidauto.*
import com.mapbox.examples.androidauto.car.navigation.ActiveGuidanceScreen
import com.mapbox.examples.androidauto.car.navigation.CarActiveGuidanceCarContext


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
            ActiveGuidanceState, ArrivalState -> {
                if(new || mActiveGuidanceScreen == null){
                    mActiveGuidanceScreen = ActiveGuidanceScreen(CarActiveGuidanceCarContext(mCarContext!!))
                }
                mActiveGuidanceScreen!!

            }
            ReactScreenState -> {
                if(new || mReactScreen == null){
                    mReactScreen = BaseCarScreen(mCarContext!!.carContext)
                }
                mReactScreen!!
            }
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

                // Route preview
                val searchCarContext = SearchCarContext(mCarContext!!)
                val point = Point.fromLngLat(19.021870, 47.493226)
                val placeRecord = PlaceRecord(
                    "some_id",
                    "some_name",
                    point,
                    "some_description"
                )
                Log.d("ReactAUTO", "routePrevMap after")
                searchCarContext.carRouteRequest.request(
                    placeRecord,
                    object : CarRouteRequestCallback {
                        override fun onRoutesReady(
                            placeRecord: PlaceRecord,
                            routes: List<DirectionsRoute>
                        ) {
                            Log.d("ReactAUTO", "onRoutesReady --- : $routes");
                            reactCarRenderContextMap.remove(screen)
                            reactCarRenderContextMap[screen] = reactCarRenderContext

                            screen = CarRoutePreviewScreen(
                                RoutePreviewCarContext(mCarContext!!),
                                placeRecord,
                                routes
                            )
                            screen?.marker = name
                            carScreens[name] = screen
                            Log.d("ReactAUTO", carScreens.toString())
                            mCurrentCarScreen = screen
                            Log.i("ReactAUTO", "pushScreen $screen")
                            mScreenManager!!.push(screen!!)
                        }

                        override fun onUnknownCurrentLocation() {
                            Log.d("ReactAUTO", "onUnknownCurrentLocation")
                        }

                        override fun onDestinationLocationUnknown() {
                            Log.d("ReactAUTO", "onDestinationLocationUnknown");
                        }

                        override fun onNoRoutesFound() {
                            Log.d("ReactAUTO", "onNoRoutesFound");
                        }
                    }
                )


                //          SearchCarContext searchCarContext = new SearchCarContext(mCarContext);
                //          Point point = Point.fromLngLat(19.021870, 47.493226);
                //          PlaceRecord placeRecord = new PlaceRecord(
                //                  "some_id",
                //                  "some_name",
                //                  point,
                //                  "some_description",
                //                  Arrays.asList()
                //          );
                //          searchCarContext.getCarRouteRequest().request(placeRecord, carRouteRequestCallback);

                //val screenParser = ScreenParser();
                //screen = screenParser.getRoutePreviewScreen(mCarContext!!, renderMap, type);
            }
        }
    }

    @ReactMethod
    fun popScreen() {
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

    fun setCarContext(carContext: MainCarContext, startScreen: BaseCarScreen) {
        mCarContext = carContext
        //mMainScreenManager = mainScreenManager
        mCurrentCarScreen = startScreen
        mScreenManager = mCurrentCarScreen?.getScreenManager()
        carScreens["root"] = mCurrentCarScreen
        Log.d("ReactAUTO", carScreens.toString())
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("ReactAUTO", "Back button pressed")
                //popScreen()
                //mScreenManager!!.pop()
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
        val templateParser = TemplateParser(reactCarRenderContext, setOnBackPressedCallback)
        return templateParser.parseTemplate(renderMap)
    }

    private fun getScreen(name: String): BaseCarScreen? {
        return carScreens[name]
    }

    private fun removeScreen(screen: BaseCarScreen?) {
        Log.d("ReactAUTO", "RemoveScreen $screen")

        val params = WritableNativeMap()
        params.putString("screen", screen!!.marker)
        sendEvent("android_auto:remove_screen", params)
        carScreens.values.remove(screen)
    }

    private fun sendEvent(eventName: String, params: Any) {
        mReactContext
            .getJSModule(RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }


}