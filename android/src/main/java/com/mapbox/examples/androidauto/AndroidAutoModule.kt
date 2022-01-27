package com.mapbox.examples.androidauto

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Template
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.facebook.react.modules.debug.DevSettingsModule
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.examples.androidauto.car.MainCarContext
import com.mapbox.examples.androidauto.car.MainCarScreen
import com.mapbox.examples.androidauto.car.MainScreenManager
import com.mapbox.examples.androidauto.car.preview.CarRoutePreviewScreen
import com.mapbox.examples.androidauto.car.preview.CarRouteRequestCallback
import com.mapbox.examples.androidauto.car.preview.RoutePreviewCarContext
import com.mapbox.examples.androidauto.car.search.PlaceRecord
import com.mapbox.examples.androidauto.car.search.SearchCarContext
import com.mapbox.geojson.Point
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*

@ReactModule(name = AndroidAutoModule.MODULE_NAME)
class AndroidAutoModule internal constructor(context: ReactApplicationContext) :
    ReactContextBaseJavaModule(context) {
    private val mHandler = Handler(Looper.getMainLooper())
    private var mCarContext: MainCarContext? = null
    private var mMainScreenManager: MainScreenManager? = null
    private var mReactCarScreen: ReactCarScreen? = null
    private var mCurrentCarScreen: Screen? = null
    private var mScreenManager: ScreenManager? = null
    private var lastname: String? = null
    private var lastReactCarRenderContext: ReactCarRenderContext? = null
    private var screen: Screen? = null

    private val carScreens: WeakHashMap<String, Screen>
    private val reactCarRenderContextMap: WeakHashMap<Screen, ReactCarRenderContext>


    companion object {
        const val MODULE_NAME = "CarModule"
        private lateinit var mReactContext: ReactApplicationContext
    }

    init {
        carScreens = WeakHashMap<String, Screen>()
        reactCarRenderContextMap = WeakHashMap<Screen, ReactCarRenderContext>()
        mReactContext = context
    }
    override fun getName(): String {
        return MODULE_NAME
    }

//    private val carRouteRequestCallback: CarRouteRequestCallback =
//        object : CarRouteRequestCallback {
//            override fun onRoutesReady(placeRecord: PlaceRecord, routes: List<DirectionsRoute>) {
//                Log.d("ReactAUTO", "OnRoutesReady $routes")
//                mHandler.post { //mScreenManager.push(screen);
//                    val screen: Screen = CarRoutePreviewScreen(
//                        RoutePreviewCarContext(mCarContext!!),
//                        placeRecord,
//                        routes[0] as List<DirectionsRoute?>
//                    )
//                    reactCarRenderContextMap.remove(screen)
//                    reactCarRenderContextMap[screen] = lastReactCarRenderContext
//                    screen.marker = lastname
//                    carScreens[lastname] = screen
//                    Log.d("ReactAUTO", carScreens.toString())
//                    mCurrentCarScreen = screen
//                    Log.i("ReactAUTO", "pushScreen $screen")
//                    mScreenManager!!.push(screen)
//                }
//            }
//
//            override fun onUnknownCurrentLocation() {
//                Log.d("ReactAUTO", "onUnknownCurrentLocation")
//            }
//
//            override fun onDestinationLocationUnknown() {
//                Log.d("ReactAUTO", "onDestinationLocationUnknown")
//            }
//
//            override fun onNoRoutesFound() {
//                Log.d("ReactAUTO", "onNoRoutesFound")
//            }
//        }


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
        val template = parseTemplate(renderMap, reactCarRenderContext)
        reactCarRenderContextMap.remove(screen)
        reactCarRenderContextMap[screen] = reactCarRenderContext
        Log.i("ReactAUTO", "Screen setTemplate$template")
        (screen as MainCarScreen?)!!.setTemplate(template)
    }

    @InternalCoroutinesApi
    @ReactMethod
    fun pushScreen(
        name: String?,
        renderMap: ReadableMap,
        callback: Callback?
    ) {
        mHandler.post(object : Runnable {
            override fun run() {
                val reactCarRenderContext = ReactCarRenderContext()
                reactCarRenderContext.setEventCallback(callback).screenMarker = name
                lastname = name
                lastReactCarRenderContext = reactCarRenderContext

                // TODO: parse template only if template provided
                val type = renderMap.getString("type")
                // if type endswith template :
                if (type!!.endsWith("template")) {
                    val template = parseTemplate(renderMap, reactCarRenderContext)
                    screen = mMainScreenManager!!.getNewScreen(false)
                    if (screen is MainCarScreen) {
                        (screen as MainCarScreen).setTemplate(template)
                    }

                    // else raise error
                    reactCarRenderContextMap.remove(screen)
                    reactCarRenderContextMap[screen] = reactCarRenderContext
                    screen?.setMarker(name)
                    carScreens[name] = screen
                    Log.d("ReactAUTO", carScreens.toString())
                    mCurrentCarScreen = screen
                    Log.i("ReactAUTO", "pushScreen $screen")
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
        })
    }

    @ReactMethod
    fun popScreen() {
        mHandler.post {
            mScreenManager!!.pop()
            removeScreen(mCurrentCarScreen)
            mCurrentCarScreen = mScreenManager!!.top as MainCarScreen
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
        screen = getScreen(name) as MainCarScreen?
        Log.d("ReactAUTO", "Set callback 1 for $name")
        if (screen == null) {
            return
        }
        Log.d("ReactAUTO", "Set callback 2 for $name")
        val reactCarRenderContext = reactCarRenderContextMap[screen] ?: return
        Log.d("ReactAUTO", "Set callback 3 for $name")
        reactCarRenderContext.eventCallback = callback
    }

    fun setCarContext(carContext: MainCarContext, mainScreenManager: MainScreenManager) {
        mCarContext = carContext
        mMainScreenManager = mainScreenManager
        mReactCarScreen = ReactCarScreen(mCarContext!!)
        mCurrentCarScreen = mainScreenManager.currentScreen(false)
        mScreenManager = mCurrentCarScreen?.getScreenManager()
        carScreens["root"] = mCurrentCarScreen
        Log.d("ReactAUTO", carScreens.toString())
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("ReactAUTO", "Back button pressed")
                mScreenManager!!.pop()
                sendEvent("android_auto:back_button", WritableNativeMap())
            }
        }
        carContext.carContext.onBackPressedDispatcher.addCallback(callback)
        sendEvent("android_auto:ready", WritableNativeMap())
    }

    private fun parseTemplate(
        renderMap: ReadableMap,
        reactCarRenderContext: ReactCarRenderContext
    ): Template {
        val templateParser = TemplateParser(reactCarRenderContext)
        return templateParser.parseTemplate(renderMap)
    }

    private fun getScreen(name: String): Screen? {
        return carScreens[name]
    }

    private fun removeScreen(screen: Screen?) {
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