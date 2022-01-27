package com.mapbox.androidauto

import android.util.Log
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.utils.internal.LoggerProvider

object AndroidAutoLog {
    fun logAndroidAuto(message: String) {
        LoggerProvider.logger.i(
            tag = Tag("MapboxAndroidAuto"),
            msg = Message("${Thread.currentThread().id}: $message")
        )
    }

    fun logAndroidAutoFailure(message: String, throwable: Throwable? = null) {
        LoggerProvider.logger.e(
            tag = Tag("MapboxAndroidAuto"),
            msg = Message("${Thread.currentThread().id}: $message"),
            tr = throwable
        )
    }
}

fun logAndroidAuto(message: String) {
    Log.d("ReactAUTO", "logAndroidAuto $message");
    AndroidAutoLog.logAndroidAuto(message)
}

fun logAndroidAutoFailure(message: String, throwable: Throwable? = null) {
    Log.d("ReactAUTO", "logAndroidAutoFailure: $message");
    AndroidAutoLog.logAndroidAutoFailure(message, throwable)
}
