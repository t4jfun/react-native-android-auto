package com.mapbox.examples.androidauto.car

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactInstanceManager.ReactInstanceEventListener
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.appregistry.AppRegistry
import com.facebook.react.modules.core.TimingModule
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.MapboxCarApp.mapboxCarMap
import com.mapbox.androidauto.car.map.widgets.compass.CarCompassSurfaceRenderer
import com.mapbox.androidauto.car.map.widgets.logo.CarLogoSurfaceRenderer
import com.mapbox.androidauto.deeplink.GeoDeeplinkNavigateAction
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.examples.androidauto.AndroidAutoModule
import com.mapbox.examples.androidauto.ReactCarScreen
import com.mapbox.examples.androidauto.car.permissions.NeedsLocationPermissionsScreen
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.trip.session.TripSessionState

class MainCarSession(private var mReactInstanceManager: ReactInstanceManager) : Session() {

    private val mapStyleUri: String
        get() = MapboxCarApp.options.run {
            if (carContext.isDarkMode) {
                mapNightStyle ?: mapDayStyle
            } else {
                mapDayStyle
            }
        }

    private var hasLocationPermissions = false
    private var mainCarContext: MainCarContext? = null
    private lateinit var mainScreenManager: MainScreenManager

    init {
        // Let the car app know that the car has been created.
        // Make sure to call ths before setting up other car components.
        Log.d("ReactAUTO", "Before MapboxCarApp.setupCar");
        MapboxCarApp.setupCar(this)
        Log.d("ReactAUTO", "After MapboxCarApp.setupCar");


        val logoSurfaceRenderer = CarLogoSurfaceRenderer()
        val compassSurfaceRenderer = CarCompassSurfaceRenderer()
        logAndroidAuto("MainCarSession constructor")
        lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onCreate(owner: LifecycleOwner) {
                logAndroidAuto("MainCarSession onCreate")
                hasLocationPermissions = hasLocationPermission()
                mainCarContext = MainCarContext(carContext)
                mainScreenManager = MainScreenManager(mainCarContext!!)
            }

            override fun onStart(owner: LifecycleOwner) {
                hasLocationPermissions = hasLocationPermission()
                logAndroidAuto("MainCarSession onStart and hasLocationPermissions $hasLocationPermissions")
                if (hasLocationPermissions) {
                    startTripSession(mainCarContext!!)
                    lifecycle.addObserver(mainScreenManager)
                }
            }

            override fun onResume(owner: LifecycleOwner) {
                logAndroidAuto("MainCarSession onResume")
                mapboxCarMap.registerObserver(logoSurfaceRenderer)
                mapboxCarMap.registerObserver(compassSurfaceRenderer)
            }

            override fun onPause(owner: LifecycleOwner) {
                logAndroidAuto("MainCarSession onPause")
                mapboxCarMap.unregisterObserver(logoSurfaceRenderer)
                mapboxCarMap.unregisterObserver(compassSurfaceRenderer)
            }

            override fun onStop(owner: LifecycleOwner) {
                logAndroidAuto("MainCarSession onStop")
                lifecycle.removeObserver(mainScreenManager)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                logAndroidAuto("MainCarSession onDestroy")
                mainCarContext = null
            }
        })
    }

//    public Screen onCreateScreen(@Nullable Intent intent) {
//        mNavigationCarSurface = new SurfaceRenderer(getCarContext(), getLifecycle());
//        screen = new CarScreen(getCarContext(), mReactInstanceManager.getCurrentReactContext(), mNavigationCarSurface);
//        screen.setMarker("root");
//        runJsApplication();
//        return screen;
//    }


    override fun onCreateScreen(intent: Intent): Screen {
        logAndroidAuto("MainCarSession onCreateScreen")
        runJsApplication()
//        val screen = ReactCarScreen(mainCarContext!!)
//        screen.marker = "root"
//        return screen
//        screen.setMarker("root");
        return when (hasLocationPermissions) {
            false -> NeedsLocationPermissionsScreen(carContext)
            true -> mainScreenManager.currentScreen(true)
        }
    }
    private fun runJsApplication() {
        val reactContext = mReactInstanceManager.getCurrentReactContext()
        if (reactContext == null) {
            mReactInstanceManager.addReactInstanceEventListener(
                object : ReactInstanceEventListener {
                    override fun onReactContextInitialized(reactContext: ReactContext) {
                        invokeStartTask(reactContext)
                        mReactInstanceManager.removeReactInstanceEventListener(this)
                    }
                })
            mReactInstanceManager.createReactContextInBackground()
        } else {
            invokeStartTask(reactContext)
        }
    }


    private fun invokeStartTask(reactContext: ReactContext?) {
        try {
            if (mReactInstanceManager == null) {
                return
            }
            if (reactContext == null) {
                return
            }
            val catalystInstance = reactContext.catalystInstance
            val jsAppModuleName = "androidAuto"
            val appParams = WritableNativeMap()
            appParams.putDouble("rootTag", 1.0)
            val appProperties = Bundle.EMPTY
            if (appProperties != null) {
                appParams.putMap("initialProps", Arguments.fromBundle(appProperties))
            }
            catalystInstance.getJSModule(AppRegistry::class.java)
                .runApplication(jsAppModuleName, appParams)
            val timingModule = reactContext.getNativeModule(TimingModule::class.java)
            val carModule = mReactInstanceManager.currentReactContext?.getNativeModule(AndroidAutoModule::class.java)
            carModule?.setCarContext(mainCarContext!!, mainScreenManager)
            timingModule?.onHostResume()
        } finally {
        }
    }

    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun startTripSession(mainCarContext: MainCarContext) {
        mainCarContext.apply {
            logAndroidAuto("MainCarSession startTripSession")
            if (mapboxNavigation.getTripSessionState() != TripSessionState.STARTED) {
                if (MapboxCarApp.options.replayEnabled) {
                    val mapboxReplayer = mapboxNavigation.mapboxReplayer
                    mapboxReplayer.pushRealLocation(carContext, 0.0)
                    mapboxNavigation.startReplayTripSession()
                    mapboxReplayer.play()
                } else {
                    mapboxNavigation.startTripSession()
                }
            }
        }
    }

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        logAndroidAuto("onCarConfigurationChanged ${carContext.isDarkMode}")
        mapboxCarMap.updateMapStyle(mapStyleUri)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        logAndroidAuto("onNewIntent $intent")

        val currentScreen: Screen = when (hasLocationPermissions) {
            false -> NeedsLocationPermissionsScreen(carContext)
            true -> {
                if (intent.action == CarContext.ACTION_NAVIGATE) {
                    GeoDeeplinkNavigateAction(carContext, lifecycle).onNewIntent(intent)
                } else {
                    null
                }
            }
        } ?: mainScreenManager.currentScreen()
        carContext.getCarService(ScreenManager::class.java).push(currentScreen)
    }

    private fun hasLocationPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
            isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun isPermissionGranted(permission: String): Boolean =
        ActivityCompat.checkSelfPermission(
            carContext.applicationContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
}
