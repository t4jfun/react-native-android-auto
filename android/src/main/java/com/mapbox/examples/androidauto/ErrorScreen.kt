package com.mapbox.examples.androidauto

import androidx.car.app.CarContext
import androidx.car.app.model.*

class ErrorScreen(
    private val _carContext: CarContext
) : BaseCarScreen(_carContext) {

    override fun onGetTemplate(): Template {
        val templateBuilder = MessageTemplate.Builder(carContext.getString(R.string.car_error_message))

        return templateBuilder
            .setHeaderAction(Action.BACK)
            .setTitle(carContext.getString(R.string.car_error_title))
            .setIcon(CarIcon.ALERT)
            .build()
    }
}