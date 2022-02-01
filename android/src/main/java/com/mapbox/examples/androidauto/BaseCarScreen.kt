package com.mapbox.examples.androidauto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Template
import com.facebook.react.bridge.ReactContext
import com.mapbox.examples.androidauto.car.MainCarContext

open class BaseCarScreen(carContext: CarContext) : Screen(carContext) {
    var mTemplate: Template? = null
    fun setTemplate(template: Template?) {
        mTemplate = template
    }

    override fun onGetTemplate(): Template {
        return mTemplate
            ?: PaneTemplate.Builder(
                Pane.Builder().setLoading(true).build()
            ).setTitle("Loading...").build()
    }
}