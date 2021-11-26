package com.adkom666.shrednotes.ask.util

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * A set of callbacks that do nothing by default so that you override only the callbacks you need.
 */
open class ActivityLifecycleOptionalCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
