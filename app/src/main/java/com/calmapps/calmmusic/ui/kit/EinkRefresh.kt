// SPDX-License-Identifier: GPL-3.0-or-later
// Ported from the AnkiDroid MMD fork, which ported it from the Loop Habit Tracker MMD fork.

package com.calmapps.calmmusic.ui.kit

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.MainThread

/**
 * Clears E Ink ghosting by flashing the window black for [FLASH_MS].
 *
 * A heuristic, not a panel command. The Kompakt exposes a `meink` service, but every method sets a
 * persistent display mode; none is a one-shot refresh. A full-screen maximum-delta repaint pushes
 * the EPD driver into a fuller waveform on its own, which is what InkOS and other E Ink launchers
 * do, and it needs no permission or hidden API.
 *
 * Every tap or swipe in an activity counts; every [actions] of them the screen flashes, a moment
 * after the last one so the screen it opened is drawn first. Set in Settings, E Ink. Taps inside a
 * panel or menu, which are windows of their own, are not counted.
 */
@MainThread
object EinkRefresh {
    private const val FLASH_MS = 100L

    /** Long enough for the screen a tap opens to be drawn before the flash clears it. */
    private const val AFTER_ACTION_MS = 400L

    const val KEY_ENABLED = "mmd_eink_refresh"
    const val KEY_ACTIONS = "mmd_eink_refresh_actions"
    const val DEFAULT_ACTIONS = 12
    val ACTION_CHOICES = listOf(6, 8, 12, 16, 24)

    private val handler = Handler(Looper.getMainLooper())
    private var pending = 0
    private lateinit var prefs: SharedPreferences

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var actions: Int
        get() = prefs.getInt(KEY_ACTIONS, DEFAULT_ACTIONS)
        set(value) = prefs.edit().putInt(KEY_ACTIONS, value).apply()

    /** Counts every tap and swipe in every activity of [application]. */
    fun install(application: Application) {
        prefs = application.getSharedPreferences("${application.packageName}_preferences", Context.MODE_PRIVATE)
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    val window = activity.window ?: return
                    val callback = window.callback ?: return
                    // AppCompat sets its own callback while the activity is created: wrap on top of it
                    if (callback !is CountingCallback) window.callback = CountingCallback(callback, activity)
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    /** Records one tap or swipe; true when it completes the count and the screen should flash. */
    private fun countAction(): Boolean {
        if (!enabled) return false
        if (++pending < actions) return false
        pending = 0
        return true
    }

    /** Flashes regardless of the count and the setting, after [delayMs]. */
    fun flash(activity: Activity?, delayMs: Long = 0) {
        val root = activity?.window?.decorView as? ViewGroup ?: return
        handler.postDelayed({
            if (activity.isFinishing || activity.isDestroyed) return@postDelayed
            val overlay = View(activity).apply {
                setBackgroundColor(Color.BLACK)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            root.addView(overlay)
            overlay.bringToFront()
            handler.postDelayed({ root.removeView(overlay) }, FLASH_MS)
        }, delayMs)
    }

    /** Passes everything on to the activity's own callback, counting each lifted finger. */
    private class CountingCallback(
        private val delegate: Window.Callback,
        private val activity: Activity,
    ) : Window.Callback by delegate {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            val handled = delegate.dispatchTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP && countAction()) flash(activity, AFTER_ACTION_MS)
            return handled
        }
    }
}
