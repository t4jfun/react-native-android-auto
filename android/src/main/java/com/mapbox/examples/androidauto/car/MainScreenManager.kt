package com.mapbox.examples.androidauto.car

import android.util.Log
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.mapbox.androidauto.ActiveGuidanceState
import com.mapbox.androidauto.ArrivalState
import com.mapbox.androidauto.CarAppState
import com.mapbox.androidauto.FreeDriveState
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.RoutePreviewState
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.examples.androidauto.car.navigation.ActiveGuidanceScreen
import com.mapbox.examples.androidauto.car.navigation.CarActiveGuidanceCarContext

class MainScreenManager(
    val mainCarContext: MainCarContext
) : DefaultLifecycleObserver {

    private var mCurrentScreen : Screen? = null

    private val carAppStateObserver = Observer<CarAppState> { carAppState ->
        val currentScreen = _currentScreen(carAppState)
        val screenManager = mainCarContext.carContext.getCarService(ScreenManager::class.java)
        logAndroidAuto("--- MainScreenManager screen change ${currentScreen.javaClass.simpleName}")
        if (screenManager.top.javaClass != currentScreen.javaClass) {
            screenManager.push(currentScreen)
        }
    }

    fun currentScreen(root: Boolean = false): Screen = _currentScreen(MapboxCarApp.carAppState.value!!, root)

    private fun _currentScreen(carAppState: CarAppState,root: Boolean = false): Screen {
        logAndroidAuto("_currentScreen for state: $carAppState")
        //if(mCurrentScreen != null) return mCurrentScreen as MainCarScreen
        val screen = when (carAppState) {
            FreeDriveState, RoutePreviewState -> MainCarScreen(mainCarContext)
            ActiveGuidanceState, ArrivalState -> {
                ActiveGuidanceScreen(CarActiveGuidanceCarContext(mainCarContext))
            }
            else -> MainCarScreen(mainCarContext)
        }
        if(root) screen.marker = "root"
        mCurrentScreen = screen
        return screen
    }

    fun getScreenByState(carAppState: CarAppState, root: Boolean = false): Screen {
        logAndroidAuto("_currentScreen for state: $carAppState")
        //if(mCurrentScreen != null) return mCurrentScreen as MainCarScreen
        val screen = when (carAppState) {
            FreeDriveState, RoutePreviewState -> MainCarScreen(mainCarContext)
            ActiveGuidanceState, ArrivalState -> {
                ActiveGuidanceScreen(CarActiveGuidanceCarContext(mainCarContext))
            }
            else -> MainCarScreen(mainCarContext)
        }
        if(root) screen.marker = "root"
        mCurrentScreen = screen
        return screen
    }

    fun getNewScreen(root: Boolean = false): MainCarScreen {
        Log.d("ReactAUTO", "getNewScreen, root: $root");
        val screen = MainCarScreen(mainCarContext)
        if(root) screen.marker = "root"
        mCurrentScreen = screen
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
}
