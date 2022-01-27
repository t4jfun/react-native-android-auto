package com.mapbox.examples.androidauto

import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactInstanceManagerBuilder
import com.facebook.react.common.LifecycleState
import com.mapbox.examples.androidauto.car.MainCarSession



class MainCarAppService : CarAppService() {
    private lateinit var mReactInstanceManager: ReactInstanceManager

    override fun onCreate() {
        Log.d("ReactAUTO", "onCreate .");

        mReactInstanceManager =
            (application as ReactApplication).getReactNativeHost().getReactInstanceManager()
        Log.d("ReactAUTO", mReactInstanceManager.toString());
    }

    private fun makeInstance(): ReactInstanceManager {
        val builder: ReactInstanceManagerBuilder = ReactInstanceManager.builder()
            .setApplication(application)
            .setJSMainModulePath("android_auto")
            .setUseDeveloperSupport(true)
            .setJSIModulesPackage(null)
            .setInitialLifecycleState(LifecycleState.BEFORE_CREATE)
        builder.addPackage(AndroidAutoPackage())
        return builder.build()
    }

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        // TODO limit hosts for production
        //    https://github.com/mapbox/mapbox-navigation-android-examples/issues/27
//        return HostValidator.Builder(this)
//                .addAllowedHosts(R.array.android_auto_allow_list)
//                .build()
    }

    override fun onCreateSession(): Session {
        Log.d("ReactAUTO", "onCreateSession .");
        return MainCarSession(mReactInstanceManager)
    }
}
