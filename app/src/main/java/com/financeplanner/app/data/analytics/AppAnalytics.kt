package com.financeplanner.app.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single point of contact for Analytics + Crashlytics across the app. Every
 * calculator ViewModel logs through this rather than calling
 * FirebaseAnalytics/FirebaseCrashlytics directly — keeps event names
 * consistent and makes it easy to see every tracked checkpoint in one file.
 */
@Singleton
class AppAnalytics @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    private val crashlytics: FirebaseCrashlytics get() = FirebaseCrashlytics.getInstance()

    /** Logged once per calculator screen when it's first composed. */
    fun logCalculatorOpened(calculatorName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, calculatorName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, calculatorName)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    /** Logged when the user taps "Calculate", regardless of whether validation passes. */
    fun logCalculateTapped(calculatorName: String) {
        val bundle = Bundle().apply { putString(PARAM_CALCULATOR_NAME, calculatorName) }
        firebaseAnalytics.logEvent(EVENT_CALCULATE_TAPPED, bundle)
    }

    /** Logged only when Calculate actually succeeds and a result is produced. */
    fun logResultViewed(calculatorName: String) {
        val bundle = Bundle().apply { putString(PARAM_CALCULATOR_NAME, calculatorName) }
        firebaseAnalytics.logEvent(EVENT_RESULT_VIEWED, bundle)
    }

    /** Logged when the user taps "Save" on a result (currently always shows Coming Soon). */
    fun logSaveTapped(calculatorName: String) {
        val bundle = Bundle().apply { putString(PARAM_CALCULATOR_NAME, calculatorName) }
        firebaseAnalytics.logEvent(EVENT_SAVE_TAPPED, bundle)
    }

    /** Logged whenever a Settings sheet control changes a persisted preference. */
    fun logSettingChanged(settingName: String, value: String) {
        val bundle = Bundle().apply {
            putString(PARAM_SETTING_NAME, settingName)
            putString(PARAM_SETTING_VALUE, value)
        }
        firebaseAnalytics.logEvent(EVENT_SETTING_CHANGED, bundle)
    }

    fun logAppTourShown() {
        firebaseAnalytics.logEvent(EVENT_APP_TOUR_SHOWN, Bundle())
    }

    fun logAppTourDismissed() {
        firebaseAnalytics.logEvent(EVENT_APP_TOUR_DISMISSED, Bundle())
    }

    fun logFeedbackSent() {
        firebaseAnalytics.logEvent(EVENT_FEEDBACK_SENT, Bundle())
    }

    /**
     * Non-fatal validation/calculation errors — these are caught and shown as
     * a UI message today, but recording them here surfaces how often real
     * users hit invalid-input edge cases we haven't seen in testing.
     */
    fun recordException(throwable: Throwable, calculatorName: String) {
        crashlytics.setCustomKey(PARAM_CALCULATOR_NAME, calculatorName)
        crashlytics.recordException(throwable)
    }

    private companion object {
        const val EVENT_CALCULATE_TAPPED = "calculate_tapped"
        const val EVENT_RESULT_VIEWED = "result_viewed"
        const val EVENT_SAVE_TAPPED = "save_tapped"
        const val EVENT_SETTING_CHANGED = "setting_changed"
        const val EVENT_APP_TOUR_SHOWN = "app_tour_shown"
        const val EVENT_APP_TOUR_DISMISSED = "app_tour_dismissed"
        const val EVENT_FEEDBACK_SENT = "feedback_sent"

        const val PARAM_CALCULATOR_NAME = "calculator_name"
        const val PARAM_SETTING_NAME = "setting_name"
        const val PARAM_SETTING_VALUE = "setting_value"
    }
}
