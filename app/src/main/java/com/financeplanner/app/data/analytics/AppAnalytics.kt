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

    private fun log(eventName: String, params: Bundle.() -> Unit = {}) {
        firebaseAnalytics.logEvent(eventName, Bundle().apply(params))
    }

    /** Logged once per calculator screen when it's first composed. */
    fun logCalculatorOpened(calculatorName: String) = logScreenOpened(calculatorName)

    /** Generic screen-view, for any screen (Dashboard, Investments, Calculations, or a
     * calculator — [logCalculatorOpened] is just this under its original name). */
    fun logScreenOpened(screenName: String) = log(FirebaseAnalytics.Event.SCREEN_VIEW) {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
    }

    /** Logged when the user taps "Calculate", regardless of whether validation passes. */
    fun logCalculateTapped(calculatorName: String) = log(EVENT_CALCULATE_TAPPED) {
        putString(PARAM_CALCULATOR_NAME, calculatorName)
    }

    /** Logged only when Calculate actually succeeds and a result is produced. */
    fun logResultViewed(calculatorName: String) = log(EVENT_RESULT_VIEWED) {
        putString(PARAM_CALCULATOR_NAME, calculatorName)
    }

    /** Logged when the user taps "Save" on a result. */
    fun logSaveTapped(calculatorName: String) = log(EVENT_SAVE_TAPPED) {
        putString(PARAM_CALCULATOR_NAME, calculatorName)
    }

    /** Logged when a dual-target calculator's save-target chooser is answered
     * (Investment vs. Calculation). */
    fun logSaveTargetChosen(calculatorName: String, target: String) = log(EVENT_SAVE_TARGET_CHOSEN) {
        putString(PARAM_CALCULATOR_NAME, calculatorName)
        putString(PARAM_SAVE_TARGET, target)
    }

    /** Logged whenever a Settings sheet control changes a persisted preference. */
    fun logSettingChanged(settingName: String, value: String) = log(EVENT_SETTING_CHANGED) {
        putString(PARAM_SETTING_NAME, settingName)
        putString(PARAM_SETTING_VALUE, value)
    }

    fun logAppTourShown() = log(EVENT_APP_TOUR_SHOWN)

    fun logAppTourDismissed() = log(EVENT_APP_TOUR_DISMISSED)

    fun logFeedbackSent() = log(EVENT_FEEDBACK_SENT)

    /** A Portfolio Mix / Asset Class Mix donut chart slice was tapped, on Home. */
    fun logChartSliceTapped(chartName: String, sliceLabel: String) = log(EVENT_CHART_SLICE_TAPPED) {
        putString(PARAM_CHART_NAME, chartName)
        putString(PARAM_SLICE_LABEL, sliceLabel)
    }

    /** The Investments tab's List/Summary layout was switched. */
    fun logInvestmentsViewModeToggled(mode: String) = log(EVENT_VIEW_MODE_TOGGLED) {
        putString(PARAM_VIEW_MODE, mode)
    }

    /** A Summary-view type section was expanded or collapsed. */
    fun logSummarySectionToggled(type: String, expanded: Boolean) = log(EVENT_SUMMARY_SECTION_TOGGLED) {
        putString(PARAM_ITEM_TYPE, type)
        putBoolean(PARAM_EXPANDED, expanded)
    }

    /** Search was used on a saved-items screen (Investments/Calculations). */
    fun logSearchUsed(screenName: String) = log(EVENT_SEARCH_USED) {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
    }

    /** A type/asset-category filter chip was applied on a saved-items screen. */
    fun logFilterApplied(screenName: String, filterType: String) = log(EVENT_FILTER_APPLIED) {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        putString(PARAM_FILTER_TYPE, filterType)
    }

    /** A saved investment/calculation's detail sheet was opened. */
    fun logSavedItemOpened(screenName: String, itemType: String) = log(EVENT_SAVED_ITEM_OPENED) {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        putString(PARAM_ITEM_TYPE, itemType)
    }

    /** "Edit" was tapped on a saved investment/calculation's detail sheet. */
    fun logSavedItemEdited(screenName: String, itemType: String) = log(EVENT_SAVED_ITEM_EDITED) {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        putString(PARAM_ITEM_TYPE, itemType)
    }

    /** A saved investment/calculation was deleted (after confirmation). */
    fun logSavedItemDeleted(screenName: String, itemType: String) = log(EVENT_SAVED_ITEM_DELETED) {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        putString(PARAM_ITEM_TYPE, itemType)
    }

    /** The Investments tab's "Add Investment" menu was opened. */
    fun logAddInvestmentMenuOpened() = log(EVENT_ADD_INVESTMENT_MENU_OPENED)

    /** A type was picked from the "Add Investment" menu. */
    fun logAddInvestmentTypeSelected(type: String) = log(EVENT_ADD_INVESTMENT_TYPE_SELECTED) {
        putString(PARAM_ITEM_TYPE, type)
    }

    /** The Investments tab's JSON/CSV export was triggered. */
    fun logExportTriggered() = log(EVENT_EXPORT_TRIGGERED)

    /** A maturity or monthly-check-in local notification was actually shown. */
    fun logReminderNotificationShown(reminderType: String) = log(EVENT_REMINDER_NOTIFICATION_SHOWN) {
        putString(PARAM_REMINDER_TYPE, reminderType)
    }

    /** A maturity or monthly-check-in local notification was tapped, opening the app. */
    fun logReminderNotificationTapped(reminderType: String) = log(EVENT_REMINDER_NOTIFICATION_TAPPED) {
        putString(PARAM_REMINDER_TYPE, reminderType)
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
        const val EVENT_SAVE_TARGET_CHOSEN = "save_target_chosen"
        const val EVENT_SETTING_CHANGED = "setting_changed"
        const val EVENT_APP_TOUR_SHOWN = "app_tour_shown"
        const val EVENT_APP_TOUR_DISMISSED = "app_tour_dismissed"
        const val EVENT_FEEDBACK_SENT = "feedback_sent"
        const val EVENT_CHART_SLICE_TAPPED = "chart_slice_tapped"
        const val EVENT_VIEW_MODE_TOGGLED = "investments_view_mode_toggled"
        const val EVENT_SUMMARY_SECTION_TOGGLED = "summary_section_toggled"
        const val EVENT_SEARCH_USED = "search_used"
        const val EVENT_FILTER_APPLIED = "filter_applied"
        const val EVENT_SAVED_ITEM_OPENED = "saved_item_opened"
        const val EVENT_SAVED_ITEM_EDITED = "saved_item_edited"
        const val EVENT_SAVED_ITEM_DELETED = "saved_item_deleted"
        const val EVENT_ADD_INVESTMENT_MENU_OPENED = "add_investment_menu_opened"
        const val EVENT_ADD_INVESTMENT_TYPE_SELECTED = "add_investment_type_selected"
        const val EVENT_EXPORT_TRIGGERED = "export_triggered"
        const val EVENT_REMINDER_NOTIFICATION_SHOWN = "reminder_notification_shown"
        const val EVENT_REMINDER_NOTIFICATION_TAPPED = "reminder_notification_tapped"

        const val PARAM_CALCULATOR_NAME = "calculator_name"
        const val PARAM_SETTING_NAME = "setting_name"
        const val PARAM_SETTING_VALUE = "setting_value"
        const val PARAM_SAVE_TARGET = "save_target"
        const val PARAM_CHART_NAME = "chart_name"
        const val PARAM_SLICE_LABEL = "slice_label"
        const val PARAM_VIEW_MODE = "view_mode"
        const val PARAM_ITEM_TYPE = "item_type"
        const val PARAM_EXPANDED = "expanded"
        const val PARAM_FILTER_TYPE = "filter_type"
        const val PARAM_REMINDER_TYPE = "reminder_type"
    }
}
